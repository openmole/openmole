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
import java.util.concurrent.{ Executors, Executor, Semaphore }
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import org.openmole.core.eventdispatcher.{ Event, EventDispatcher }
import org.openmole.core.exception.{ UserBadDataError, MultipleException }
import org.openmole.core.tools.service.{ Logger, Priority, Random }
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.job.State._
import org.openmole.core.workflow.job.MoleJob.moleJobOrdering
import org.openmole.core.workflow.execution.local._
import org.openmole.core.workspace.Workspace
import org.openmole.core.tools.collection._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.tools._
import scala.collection.JavaConversions._

import scala.collection.immutable.HashMap
import scala.collection.mutable.Buffer
import scala.concurrent.stm.{ Ref, TMap, atomic, retry }
import javax.xml.bind.annotation.XmlTransient
import org.openmole.core.workflow.execution.Environment

object MoleExecution extends Logger {

  class Starting extends Event[MoleExecution]
  case class Finished(canceled: Boolean) extends Event[MoleExecution]
  case class JobStatusChanged(moleJob: MoleJob, capsule: Capsule, newState: State, oldState: State) extends Event[MoleExecution]
  case class JobCreated(moleJob: MoleJob, capsule: Capsule) extends Event[MoleExecution]
  case class JobSubmitted(moleJob: Job, capsule: Capsule, environment: Environment) extends Event[MoleExecution]
  case class JobFinished(moleJob: MoleJob, capsule: Capsule) extends Event[MoleExecution]
  case class JobFailed(moleJob: MoleJob, capsule: Capsule, exception: Throwable) extends Event[MoleExecution] with ExceptionEvent {
    def level = Level.SEVERE
  }
  case class ExceptionRaised(moleJob: MoleJob, exception: Throwable, level: Level) extends Event[MoleExecution] with ExceptionEvent
  case class SourceExceptionRaised(source: Source, capsule: Capsule, exception: Throwable, level: Level) extends Event[MoleExecution] with ExceptionEvent
  case class HookExceptionRaised(hook: Hook, moleJob: MoleJob, exception: Throwable, level: Level) extends Event[MoleExecution] with ExceptionEvent

  def apply(
    mole: Mole,
    sources: Iterable[(Capsule, Source)] = Iterable.empty,
    hooks: Iterable[(Capsule, Hook)] = Iterable.empty,
    environments: Map[Capsule, Environment] = Map.empty,
    grouping: Map[Capsule, Grouping] = Map.empty,
    implicits: Context = Context.empty,
    seed: Long = Workspace.newSeed,
    defaultEnvironment: Environment = LocalEnvironment.default)(implicit executionContext: ExecutionContext) =
    PartialMoleExecution(
      mole,
      sources,
      hooks,
      environments,
      grouping,
      seed,
      defaultEnvironment).toExecution(implicits)(executionContext)

}

class MoleExecution(
    val mole: Mole,
    val sources: Sources,
    val hooks: Hooks,
    val environments: Map[Capsule, Environment],
    val grouping: Map[Capsule, Grouping],
    val seed: Long,
    val defaultEnvironment: Environment,
    val id: String = UUID.randomUUID().toString)(val implicits: Context, val executionContext: ExecutionContext) {

  private val _started = Ref(false)
  private val _canceled = Ref(false)
  private val _finished = Ref(false)

  private val _startTime = Ref(None: Option[Long])
  private val _endTime = Ref(None: Option[Long])

  private val ticketNumber = Ref(0L)

  private val waitingJobs: TMap[Capsule, TMap[MoleJobGroup, Ref[List[MoleJob]]]] =
    TMap(grouping.map { case (c, g) ⇒ c -> TMap.empty[MoleJobGroup, Ref[List[MoleJob]]] }.toSeq: _*)

  private val nbWaiting = Ref(0)

  val rootSubMoleExecution = new SubMoleExecution(None, this)
  val rootTicket = Ticket(id, ticketNumber.next)

  val dataChannelRegistry = new RegistryWithTicket[DataChannel, Buffer[Variable[_]]]

  val _exception = Ref(Option.empty[Throwable])

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
          val category = strategy(moleJob.context, TMap.asMap(groups).map { case (gr, jobs) ⇒ gr -> jobs() })
          val jobs = groups.getOrElseUpdate(category, Ref(List.empty))
          jobs() = moleJob :: jobs()
          nbWaiting += 1

          if (strategy.complete(jobs())) {
            groups -= category
            nbWaiting -= jobs().size
            Some(new Job(this, jobs()) -> capsule)
          }
          else None
        case None ⇒
          val job = new Job(this, List(moleJob))
          Some(job -> capsule)
      }
    }.map { case (j, c) ⇒ submit(j, c) }

  private def submit(job: Job, capsule: Capsule) =
    if (!job.finished) {
      val env = environments.getOrElse(capsule, defaultEnvironment)
      env.submit(job)
      EventDispatcher.trigger(this, new MoleExecution.JobSubmitted(job, capsule, env))
    }

  def submitAll =
    atomic { implicit txn ⇒
      val jobs =
        for {
          (capsule, groups) ← TMap.asMap(waitingJobs).toList
          (_, jobs) ← TMap.asMap(groups).toList
        } yield capsule -> jobs()
      nbWaiting() = 0
      waitingJobs.clear
      jobs
    }.foreach {
      case (capsule, jobs) ⇒ submit(new Job(this, jobs), capsule)
    }

  def allWaiting = atomic { implicit txn ⇒ numberOfJobs <= nbWaiting() }

  def start(context: Context): this.type = {
    EventDispatcher.trigger(this, new MoleExecution.Starting)
    executionContext.directory.foreach(_.mkdirs)
    rootSubMoleExecution.newChild.submit(mole.root, context, nextTicket(rootTicket))
    if (allWaiting) submitAll
    this
  }

  def start: this.type = {
    if (!_started.getUpdate(_ ⇒ true)) {
      val validationErrors = Validation(mole, implicits, sources, hooks)
      if (!validationErrors.isEmpty) throw new UserBadDataError("Formal validation of your mole has failed, several errors have been found: " + validationErrors.mkString("\n"))
      _startTime.single() = Some(System.currentTimeMillis)
      start(Context.empty)
    }
    this
  }

  def cancel(t: Throwable): this.type = {
    _exception.single() = Some(t)
    cancel
  }

  def cancel: this.type = {
    if (!_canceled.getUpdate(_ ⇒ true)) {
      rootSubMoleExecution.cancel
      EventDispatcher.trigger(this, MoleExecution.Finished(canceled = true))
      _finished.single() = true
      _endTime.single() = Some(System.currentTimeMillis)
    }
    this
  }

  def moleJobs = rootSubMoleExecution.jobs

  def waitUntilEnded = {
    atomic { implicit txn ⇒
      if (!_finished()) retry
      _exception().foreach { throw _ }
    }
    this
  }

  def jobFailedOrCanceled(moleJob: MoleJob, capsule: Capsule) = jobOutputTransitionsPerformed(moleJob, capsule)

  def jobOutputTransitionsPerformed(job: MoleJob, capsule: Capsule) =
    if (!_canceled.single()) {
      if (allWaiting) submitAll
      if (numberOfJobs == 0) {
        EventDispatcher.trigger(this, MoleExecution.Finished(canceled = false))
        _finished.single() = true
        _endTime.single() = Some(System.currentTimeMillis)
      }
    }

  def finished: Boolean = _finished.single()

  def started: Boolean = _started.single()

  def nextTicket(parent: Ticket): Ticket = Ticket(parent, ticketNumber.next)

  def nextJobId = UUID.randomUUID

  def newSeed = rng.nextLong

  lazy val rng = Random.newRNG(seed)

}
