/*
 * Copyright (C) 2010 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.mole

import java.util.UUID
import java.util.logging.Level

import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job.State._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole.MoleExecution.MoleExecutionFailed
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.transition.DataChannel
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.lock._
import org.openmole.tool.logger.Logger
import org.openmole.tool.random
import org.openmole.tool.random.Seeder

import scala.collection.mutable.Buffer
import scala.concurrent.stm._

object MoleExecution extends Logger {

  class Starting extends Event[MoleExecution]
  case class Finished(canceled: Boolean) extends Event[MoleExecution]
  case class JobStatusChanged(moleJob: MoleJob, capsule: Capsule, newState: State, oldState: State) extends Event[MoleExecution]
  case class JobCreated(moleJob: MoleJob, capsule: Capsule) extends Event[MoleExecution]
  case class JobSubmitted(moleJob: Job, capsule: Capsule, environment: Environment) extends Event[MoleExecution]
  case class JobFinished(moleJob: MoleJob, capsule: Capsule) extends Event[MoleExecution]

  sealed trait MoleExecutionFailed extends ExceptionEvent {
    def capsule: Capsule
  }
  case class JobFailed(moleJob: MoleJob, capsule: Capsule, exception: Throwable) extends Event[MoleExecution] with MoleExecutionFailed {
    def level = Level.SEVERE
  }
  case class ExceptionRaised(moleJob: MoleJob, capsule: Capsule, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed
  case class SourceExceptionRaised(source: Source, capsule: Capsule, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed
  case class HookExceptionRaised(hook: Hook, capsule: Capsule, moleJob: MoleJob, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed

  private def listOfTupleToMap[K, V](l: Traversable[(K, V)]): Map[K, Traversable[V]] = l.groupBy(_._1).mapValues(_.map(_._2))

  def apply(
    mole:                        Mole,
    sources:                     Iterable[(Capsule, Source)]                = Iterable.empty,
    hooks:                       Iterable[(Capsule, Hook)]                  = Iterable.empty,
    environments:                Map[Capsule, EnvironmentProvider]          = Map.empty,
    grouping:                    Map[Capsule, Grouping]                     = Map.empty,
    implicits:                   Context                                    = Context.empty,
    defaultEnvironment:          OptionalArgument[LocalEnvironmentProvider] = None,
    cleanOnFinish:               Boolean                                    = true,
    executionContext:            OptionalArgument[MoleExecutionContext]     = None,
    startStopDefaultEnvironment: Boolean                                    = true
  )(implicit moleServices: MoleServices): MoleExecution = {
    import moleServices._

    def defaultDefaultEnvironment =
      LocalEnvironment()(varName = sourcecode.Name("local"), preference = implicitly, threadProvider = implicitly, eventDispatcher = implicitly)

    new MoleExecution(
      mole,
      listOfTupleToMap(sources),
      listOfTupleToMap(hooks),
      environments,
      grouping,
      defaultEnvironment.getOrElse(defaultDefaultEnvironment),
      cleanOnFinish,
      implicits,
      executionContext.getOrElse(MoleExecutionContext()),
      startStopDefaultEnvironment,
      id = UUID.randomUUID().toString
    )
  }

}

case class JobStatuses(ready: Long, running: Long, completed: Long)

class MoleExecution(
  val mole:                        Mole,
  val sources:                     Sources,
  val hooks:                       Hooks,
  val environmentProviders:        Map[Capsule, EnvironmentProvider],
  val grouping:                    Map[Capsule, Grouping],
  val defaultEnvironmentProvider:  LocalEnvironmentProvider,
  val cleanOnFinish:               Boolean,
  val implicits:                   Context,
  val executionContext:            MoleExecutionContext,
  val startStopDefaultEnvironment: Boolean,
  val id:                          String
) {

  import executionContext.services._

  private val _started = Ref(false)
  private val _canceled = Ref(false)
  private val _finished = Ref(false)

  private val _startTime = Ref(None: Option[Long])
  private val _endTime = Ref(None: Option[Long])

  private val ticketNumber = Ref(0L)

  private val waitingJobs: TMap[Capsule, TMap[MoleJobGroup, Ref[List[MoleJob]]]] =
    TMap(grouping.map { case (c, g) ⇒ c → TMap.empty[MoleJobGroup, Ref[List[MoleJob]]] }.toSeq: _*)

  private val nbWaiting = Ref(0)
  private val _completed = Ref(0L)

  lazy val environmentInstances = environmentProviders.toVector.map { case (k, v) ⇒ v }.distinct.map { v ⇒ v → v() }.toMap
  lazy val environments = environmentProviders.toVector.map { case (k, v) ⇒ k → environmentInstances(v) }.toMap
  lazy val defaultEnvironment = defaultEnvironmentProvider()
  def allEnvironments = (environmentInstances.values ++ Seq(defaultEnvironment)).toVector.distinct

  val rootSubMoleExecution = new SubMoleExecution(None, this)
  val rootTicket = Ticket(id, ticketNumber.next)
  val taskCache = KeyValueCache()
  val lockRepository = LockRepository[LockKey]()

  val dataChannelRegistry = new RegistryWithTicket[DataChannel, Buffer[Variable[_]]]

  val _exception = Ref(Option.empty[MoleExecutionFailed])

  def numberOfJobs = rootSubMoleExecution.numberOfJobs

  def exception = _exception.single()

  def duration: Option[Long] =
    (_startTime.single(), _endTime.single()) match {
      case (None, _)          ⇒ None
      case (Some(t), None)    ⇒ Some(System.currentTimeMillis - t)
      case (Some(s), Some(e)) ⇒ Some(e - s)
    }

  def group(moleJob: MoleJob, capsule: Capsule, submole: SubMoleExecution) =
    atomic { implicit txn ⇒
      grouping.get(capsule) match {
        case Some(strategy) ⇒
          val groups = waitingJobs(capsule)
          val category = strategy(moleJob.context, TMap.asMap(groups).map { case (gr, jobs) ⇒ gr → jobs() })
          val jobs = groups.getOrElseUpdate(category, Ref(List.empty))
          jobs() = moleJob :: jobs()
          nbWaiting += 1

          if (strategy.complete(jobs())) {
            groups -= category
            nbWaiting -= jobs().size
            Some(new Job(this, jobs()) → capsule)
          }
          else None
        case None ⇒
          val job = new Job(this, List(moleJob))
          Some(job → capsule)
      }
    }.map { case (j, c) ⇒ submit(j, c) }

  private def submit(job: Job, capsule: Capsule) =
    if (!job.finished) {
      val env = environments.getOrElse(capsule, defaultEnvironment)
      env match {
        case env: SubmissionEnvironment ⇒ env.submit(job)
        case env: LocalEnvironment      ⇒ env.submit(job, TaskExecutionContext(newFile.baseDir, env, preference, threadProvider, fileService, workspace, taskCache, lockRepository))
      }
      eventDispatcher.trigger(this, new MoleExecution.JobSubmitted(job, capsule, env))
    }

  def submitAll =
    atomic { implicit txn ⇒
      val jobs =
        for {
          (capsule, groups) ← TMap.asMap(waitingJobs).toList
          (_, jobs) ← TMap.asMap(groups).toList
        } yield capsule → jobs()
      nbWaiting() = 0
      waitingJobs.clear
      jobs
    }.foreach {
      case (capsule, jobs) ⇒ submit(new Job(this, jobs), capsule)
    }

  def allWaiting = atomic { implicit txn ⇒ numberOfJobs <= nbWaiting() }

  def start(context: Context): this.type = {
    def startEnvironments() = {
      if (startStopDefaultEnvironment) defaultEnvironment.start()
      environments.values.foreach(_.start())
    }

    newFile.baseDir.mkdirs()
    _started.single() = true
    _startTime.single() = Some(System.currentTimeMillis)
    eventDispatcher.trigger(this, new MoleExecution.Starting)
    startEnvironments()
    rootSubMoleExecution.newChild.submit(mole.root, context, nextTicket(rootTicket))
    if (allWaiting) submitAll
    this
  }

  def start: this.type = {
    if (!_started.getUpdate(_ ⇒ true)) {
      val validationErrors = Validation(mole, implicits, sources, hooks)
      if (!validationErrors.isEmpty) throw new UserBadDataError(s"Formal validation of your mole has failed, ${validationErrors.size} error(s) has(ve) been found.\n" + validationErrors.mkString("\n") + s"\nIn mole: $mole")
      start(Context.empty)
    }
    this
  }

  def cancel(t: MoleExecutionFailed): this.type = {
    val allReadyCanceled =
      atomic { implicit ctx ⇒
        if (!_canceled()) _exception() = Some(t)
        _canceled.getUpdate(_ ⇒ true)
      }
    if (!allReadyCanceled) cancelAction
    this
  }

  def cancel: this.type = {
    if (!_canceled.getUpdate(_ ⇒ true)) cancelAction
    this
  }

  private def cancelAction = {
    rootSubMoleExecution.cancel
    eventDispatcher.trigger(this, MoleExecution.Finished(canceled = true))
    finish()
  }

  def moleJobs = rootSubMoleExecution.jobs

  def waitUntilEnded = {
    if (!started) throw new UserBadDataError("Execution is not started")
    atomic { implicit txn ⇒
      if (!_finished()) retry
      _exception().foreach { e ⇒ throw e.exception }
    }
    this
  }

  def jobStatuses: JobStatuses = {
    val jobs = moleJobs

    val runningSet: java.util.HashSet[UUID] = {
      def submissionEnvironments = environments.values.toSeq.collect { case e: SubmissionEnvironment ⇒ e }
      def executionJobs = submissionEnvironments.toIterator.flatMap(_.jobs.toIterator)

      val set = new java.util.HashSet[UUID](jobs.size + 1, 1.0f)

      for {
        ej ← executionJobs
        if (ej.state == ExecutionState.RUNNING)
        mj ← ej.moleJobs
      } set.add(mj.id)

      set
    }

    def isRunningOnEnvironment(moleJob: MoleJob): Boolean = runningSet.contains(moleJob.id)

    var ready = 0L
    var running = 0L

    for {
      moleJob ← jobs
    } {
      if (isRunningOnEnvironment(moleJob)) running += 1
      else
        moleJob.state match {
          case READY   ⇒ ready += 1
          case RUNNING ⇒ running += 1
          case _       ⇒
        }
    }

    val completed = _completed.single()
    JobStatuses(ready, running, completed)
  }

  private[mole] def jobFailedOrCanceled(moleJob: MoleJob, capsule: Capsule) = jobOutputTransitionsPerformed(moleJob, capsule)
  private[mole] def jobFinished(moleJob: MoleJob, capsule: Capsule) = {
    _completed.single() += 1
    jobOutputTransitionsPerformed(moleJob, capsule)
  }

  private def jobOutputTransitionsPerformed(job: MoleJob, capsule: Capsule) =
    if (!_canceled.single()) {
      if (allWaiting) submitAll
      if (numberOfJobs == 0) {
        eventDispatcher.trigger(this, MoleExecution.Finished(canceled = false))
        finish()
      }
    }

  private def finish() = {
    def stopEnvironments() = {
      if (startStopDefaultEnvironment) defaultEnvironment.stop()
      environments.values.foreach(_.stop())
    }

    try stopEnvironments()
    finally {
      _finished.single() = true
      _endTime.single() = Some(System.currentTimeMillis)
      if (cleanOnFinish) newFile.baseDir.recursiveDelete
    }
  }

  def canceled: Boolean = _canceled.single()
  def finished: Boolean = _finished.single()
  def started: Boolean = _started.single()
  def startTime: Option[Long] = _startTime.single()

  def nextTicket(parent: Ticket): Ticket = Ticket(parent, ticketNumber.next)
  def nextJobId = UUID.randomUUID

}
