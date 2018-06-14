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
import java.util.concurrent.{ CountDownLatch, Executors, LinkedBlockingQueue, TimeUnit }
import java.util.logging.Level

import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.preference.Preference
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.job
import org.openmole.core.workflow.job.State._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole.MoleExecution.{ AggregationTransitionRegistry, MoleExecutionFailed, SubMoleExecutionState }
import org.openmole.core.workflow.task.{ MoleTask, TaskExecutionContext }
import org.openmole.core.workflow.tools.{ OptionalArgument ⇒ _, _ }
import org.openmole.core.workflow.transition.{ DataChannel, IAggregationTransition, ITransition }
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.collection.PriorityQueue
import org.openmole.tool.lock._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.random
import org.openmole.tool.random.Seeder

import scala.collection.JavaConverters._
import scala.collection.mutable.{ Buffer, ListBuffer }

object MoleExecution extends JavaLogger {

  class Starting extends Event[MoleExecution]
  case class Finished(canceled: Boolean) extends Event[MoleExecution]
  case class JobStatusChanged(moleJob: MoleJob, capsule: Capsule, newState: State, oldState: State) extends Event[MoleExecution]
  case class JobCreated(moleJob: MoleJob, capsule: Capsule) extends Event[MoleExecution]
  case class JobSubmitted(moleJob: Job, capsule: Capsule, environment: Environment) extends Event[MoleExecution]
  case class JobFinished(moleJob: MoleJob, capsule: Capsule) extends Event[MoleExecution]

  object MoleExecutionFailed {
    def exception(moleExecutionError: MoleExecutionFailed) = moleExecutionError.exception
    def capsule(moleExecutionError: MoleExecutionFailed) = moleExecutionError match {
      case e: JobFailed             ⇒ Some(e.capsule)
      case e: ExceptionRaised       ⇒ Some(e.capsule)
      case e: SourceExceptionRaised ⇒ Some(e.capsule)
      case e: HookExceptionRaised   ⇒ Some(e.capsule)
      case e: MoleExecutionError    ⇒ None
    }
  }

  sealed trait MoleExecutionFailed {
    def exception: Throwable
  }

  case class JobFailed(moleJob: MoleJob, capsule: Capsule, exception: Throwable) extends Event[MoleExecution] with MoleExecutionFailed {
    def level = Level.SEVERE
  }

  case class ExceptionRaised(moleJob: MoleJob, capsule: Capsule, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed
  case class SourceExceptionRaised(source: Source, capsule: Capsule, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed
  case class HookExceptionRaised(hook: Hook, capsule: Capsule, moleJob: MoleJob, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed
  case class MoleExecutionError(exception: Throwable) extends MoleExecutionFailed

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
    startStopDefaultEnvironment: Boolean                                    = true,
    taskCache:                   KeyValueCache                              = KeyValueCache(),
    lockRepository:              LockRepository[LockKey]                    = LockRepository()
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
      id = UUID.randomUUID().toString,
      taskCache = taskCache,
      lockRepository = lockRepository
    )
  }

  type CapsuleStatuses = Map[Capsule, JobStatuses]

  case class JobStatuses(ready: Long, running: Long, completed: Long)

  type AggregationTransitionRegistry = RegistryWithTicket[IAggregationTransition, Buffer[(Long, Variable[_])]]
  type MasterCapsuleRegistry = RegistryWithTicket[MasterCapsule, Context]
  type TransitionRegistry = RegistryWithTicket[ITransition, Iterable[Variable[_]]]

  def cancel(subMoleExecution: SubMoleExecutionState): Unit = {
    def cancelJobs() = subMoleExecution.jobs.keys.foreach { j ⇒ j.cancel }
    subMoleExecution.canceled = true
    cancelJobs()
    subMoleExecution.children.values.toVector.foreach(cancel)
    subMoleExecution.parent.foreach(_.children.remove(subMoleExecution.id))
  }

  def clean(subMoleExecutionState: SubMoleExecutionState) = {
    subMoleExecutionState.parent.foreach(s ⇒ s.children.remove(subMoleExecutionState.id))
    subMoleExecutionState.moleExecution.subMoleExecutions.remove(subMoleExecutionState.id)
  }

  def updateNbJobs(subMoleExecutionState: SubMoleExecutionState, v: Int): Unit = {
    subMoleExecutionState.nbJobs = subMoleExecutionState.nbJobs + v
    subMoleExecutionState.parent.foreach(s ⇒ updateNbJobs(s, v))
  }

  def submit(subMoleExecutionState: SubMoleExecutionState, capsule: Capsule, context: Context, ticket: Ticket): Unit = {
    import subMoleExecutionState.moleExecution.executionContext.services._
    if (!subMoleExecutionState.canceled) {
      updateNbJobs(subMoleExecutionState, 1)

      val sourced =
        subMoleExecutionState.moleExecution.sources(capsule).foldLeft(Context.empty) {
          case (a, s) ⇒
            val ctx = try s.perform(subMoleExecutionState.moleExecution.implicits + context, subMoleExecutionState.moleExecution.executionContext)
            catch {
              case t: Throwable ⇒
                Log.logger.log(Log.FINE, "Error in submole execution", t)
                val event = MoleExecution.SourceExceptionRaised(s, capsule, t, Log.SEVERE)
                eventDispatcher.trigger(subMoleExecutionState.moleExecution, event)
                cancel(subMoleExecutionState.moleExecution, Some(event))
                throw new InternalProcessingError(t, s"Error in source execution that is plugged to $capsule")
            }
            a + ctx
        } + Variable(Variable.openMOLESeed, seeder.newSeed)

      capsule match {
        case c: MasterCapsule ⇒
          def stateChanged(job: MoleJob, oldState: State, newState: State) =
            eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobStatusChanged(job, c, newState, oldState))

          import org.openmole.tool.thread._

          val jobId = nextJobId(subMoleExecutionState.moleExecution)

          subMoleExecutionState.masterCapsuleExecutor.submit {
            val savedContext = subMoleExecutionState.masterCapsuleRegistry.remove(c, ticket.parentOrException).getOrElse(Context.empty)
            val moleJob: MoleJob = MoleJob(capsule.task, subMoleExecutionState.moleExecution.implicits + sourced + context + savedContext, jobId, stateChanged)
            eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobCreated(moleJob, capsule))
            MoleExecutionMessage.send(subMoleExecutionState.moleExecution)(MoleExecutionMessage.RegisterJob(subMoleExecutionState, moleJob, capsule))
            val taskContext =
              TaskExecutionContext(
                newFile.baseDir,
                subMoleExecutionState.moleExecution.defaultEnvironment,
                preference,
                threadProvider,
                fileService,
                workspace,
                outputRedirection,
                subMoleExecutionState.moleExecution.taskCache,
                subMoleExecutionState.moleExecution.lockRepository,
                eventDispatcher,
                moleExecution = Some(subMoleExecutionState.moleExecution)
              )
            moleJob.perform(taskContext)
            subMoleExecutionState.masterCapsuleRegistry.register(c, ticket.parentOrException, c.toPersist(moleJob.context))
            MoleExecutionMessage.send(subMoleExecutionState.moleExecution)(MoleExecutionMessage.JobFinished(subMoleExecutionState.id)(moleJob, moleJob.state, capsule, ticket))
          }
        case _ ⇒
          def stateChanged(job: MoleJob, oldState: State, newState: State) = {
            eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobStatusChanged(job, capsule, newState, oldState))
            if (newState.isFinal) MoleExecutionMessage.send(subMoleExecutionState.moleExecution)(MoleExecutionMessage.JobFinished(subMoleExecutionState.id)(job, newState, capsule, ticket))
          }

          val moleJob: MoleJob = MoleJob(capsule.task, subMoleExecutionState.moleExecution.implicits + sourced + context, nextJobId(subMoleExecutionState.moleExecution), stateChanged)
          MoleExecutionMessage.send(subMoleExecutionState.moleExecution)(MoleExecutionMessage.RegisterJob(subMoleExecutionState, moleJob, capsule))
          eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobCreated(moleJob, capsule))
          group(subMoleExecutionState.moleExecution, moleJob, capsule)
      }

    }
  }

  def jobFinished(subMoleExecutionState: SubMoleExecutionState, job: MoleJob, capsule: Capsule, ticket: Ticket) = {
    val mole = subMoleExecutionState.moleExecution.mole

    def ctxForHooks = subMoleExecutionState.moleExecution.implicits + job.context

    def executeHook(h: Hook) =
      try h.perform(ctxForHooks, subMoleExecutionState.moleExecution.executionContext)
      catch {
        case e: Throwable ⇒
          import subMoleExecutionState.moleExecution.executionContext.services._
          val event = MoleExecution.HookExceptionRaised(h, capsule, job, e, Log.SEVERE)
          eventDispatcher.trigger(subMoleExecutionState.moleExecution, event)
          cancel(subMoleExecutionState.moleExecution, Some(event))
          Log.logger.log(Log.FINE, "Error in execution of misc " + h + "at the end of task " + job.task, e)
          throw e
      }

    try {
      val hooksVariables = subMoleExecutionState.moleExecution.hooks(capsule).flatMap(executeHook).unzip._2
      val context = job.context ++ hooksVariables
      mole.outputDataChannels(capsule).toSeq.foreach { d ⇒ DataChannel.provides(d, subMoleExecutionState.moleExecution.implicits + context, ticket, subMoleExecutionState.moleExecution) }

      for {
        transition ← mole.outputTransitions(capsule).toList.sortBy(t ⇒ mole.slots(t.end.capsule).size).reverse
      } transition.perform(subMoleExecutionState.moleExecution.implicits + context, ticket, subMoleExecutionState.moleExecution, subMoleExecutionState.id, subMoleExecutionState.moleExecution.executionContext)

    }
    catch {
      case t: Throwable ⇒
        Log.logger.log(Log.FINE, "Error in submole execution", t)
        val event = MoleExecution.ExceptionRaised(job, capsule, t, Log.SEVERE)
        import subMoleExecutionState.moleExecution.executionContext.services._
        eventDispatcher.trigger(subMoleExecutionState.moleExecution, event)
        cancel(subMoleExecutionState.moleExecution, Some(event))
        throw t
    }
    finally removeJob(subMoleExecutionState, job)
  }

  private def removeJob(subMoleExecutionState: SubMoleExecutionState, job: MoleJob) = {
    subMoleExecutionState.jobs.remove(job)
    updateNbJobs(subMoleExecutionState, -1)
  }

  def newSubMoleExecution(
    parent:        Option[SubMoleExecutionState],
    moleExecution: MoleExecution) = {
    val id = SubMoleExecution(moleExecution.currentSubMoleExecutionId)
    moleExecution.currentSubMoleExecutionId += 1
    val sm = new SubMoleExecutionState(id, parent, moleExecution)
    parent.foreach(_.children.put(id, sm))
    moleExecution.subMoleExecutions.put(id, sm)
    sm
  }

  def newChildSubMoleExecution(subMoleExecution: SubMoleExecutionState): SubMoleExecutionState =
    newSubMoleExecution(Some(subMoleExecution), subMoleExecution.moleExecution)

  def finalState(subMoleExecutionState: SubMoleExecutionState, job: MoleJob, state: State, capsule: Capsule, ticket: Ticket) = {
    job.exception match {
      case Some(e) ⇒
        val error = MoleExecution.JobFailed(job, capsule, e)
        cancel(subMoleExecutionState.moleExecution, Some(error))
        Log.logger.log(Log.FINE, s"Error in user job execution for capsule $capsule, job state is FAILED.", e)
        subMoleExecutionState.moleExecution.executionContext.services.eventDispatcher.trigger(subMoleExecutionState.moleExecution, error)
      case _ ⇒
    }

    if (state == COMPLETED) {
      subMoleExecutionState.moleExecution.completed(capsule) = subMoleExecutionState.moleExecution.completed(capsule) + 1
      subMoleExecutionState.moleExecution.executionContext.services.eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobFinished(job, capsule))
    }

    state match {
      case COMPLETED         ⇒ jobFinished(subMoleExecutionState, job, capsule, ticket)
      case FAILED | CANCELED ⇒ removeJob(subMoleExecutionState, job) //jobFailedOrCanceled(job)
      case _                 ⇒
    }
  }

  /* -------------- Mole Execution ----------------- */

  def start(moleExecution: MoleExecution, context: Option[Context]) =
    if (!moleExecution._started) {
      def startEnvironments() = {
        if (moleExecution.startStopDefaultEnvironment) moleExecution.defaultEnvironment.start()
        moleExecution.environments.values.foreach(_.start())
      }

      import moleExecution.executionContext.services._

      newFile.baseDir.mkdirs()
      moleExecution._started = true
      moleExecution._startTime = Some(System.currentTimeMillis)
      eventDispatcher.trigger(moleExecution, new MoleExecution.Starting)
      startEnvironments()
      submit(moleExecution.rootSubMoleExecution, moleExecution.mole.root, context.getOrElse(Context.empty), nextTicket(moleExecution, moleExecution.rootTicket))
      checkAllWaiting(moleExecution)
    }

  private def finish(moleExecution: MoleExecution, canceled: Boolean = false) =
    if (!moleExecution._finished) {
      def stopEnvironments() = {
        if (moleExecution.startStopDefaultEnvironment) moleExecution.defaultEnvironment.stop()
        moleExecution.environments.values.foreach(_.stop())
      }

      try stopEnvironments()
      finally {
        moleExecution._finished = true
        moleExecution._endTime = Some(System.currentTimeMillis)
        if (moleExecution.cleanOnFinish) {
          moleExecution.executionContext.services.newFile.baseDir.recursiveDelete
          moleExecution.taskCache.close()
        }
        moleExecution.executionContext.services.eventDispatcher.trigger(moleExecution, MoleExecution.Finished(canceled = canceled))
      }

    }

  def cancel(moleExecution: MoleExecution, t: Option[MoleExecutionFailed]): Unit =
    if (!moleExecution._canceled) {
      moleExecution._exception = t
      cancel(moleExecution.rootSubMoleExecution)
      moleExecution._canceled = true
      finish(moleExecution, canceled = true)
    }

  def nextTicket(moleExecution: MoleExecution, parent: Ticket): Ticket = {
    val ticket = Ticket(parent, moleExecution.ticketNumber)
    moleExecution.ticketNumber = moleExecution.ticketNumber + 1
    ticket
  }

  def nextJobId(moleExecution: MoleExecution) = UUID.randomUUID

  def group(moleExecution: MoleExecution, moleJob: MoleJob, capsule: Capsule) = {
    moleExecution.grouping.get(capsule) match {
      case Some(strategy) ⇒
        val groups = moleExecution.waitingJobs.getOrElseUpdate(capsule, collection.mutable.Map())
        val category = strategy(moleJob.context, groups.map { case (gr, jobs) ⇒ gr → jobs })
        val jobs = groups.getOrElseUpdate(category, ListBuffer())
        jobs.append(moleJob)
        moleExecution.nbWaiting += 1

        if (strategy.complete(jobs())) {
          groups -= category
          moleExecution.nbWaiting -= jobs.size
          Some(Job(moleExecution, jobs.toVector) → capsule)
        }
        else None
      case None ⇒
        val job = Job(moleExecution, Vector(moleJob))
        Some(job → capsule)
    }
  }.map { case (j, c) ⇒ submit(moleExecution, j, c) }

  def submit(moleExecution: MoleExecution, job: Job, capsule: Capsule) =
    if (!job.finished) {
      val env = moleExecution.environments.getOrElse(capsule, moleExecution.defaultEnvironment)
      import moleExecution.executionContext.services._

      env match {
        case env: SubmissionEnvironment ⇒ env.submit(job)
        case env: LocalEnvironment ⇒
          env.submit(
            job,
            TaskExecutionContext(
              newFile.baseDir,
              env,
              preference,
              threadProvider,
              fileService,
              workspace,
              outputRedirection,
              moleExecution.taskCache,
              moleExecution.lockRepository,
              eventDispatcher,
              moleExecution = Some(moleExecution)
            )
          )
      }

      eventDispatcher.trigger(moleExecution, MoleExecution.JobSubmitted(job, capsule, env))
    }

  def submitAll(moleExecution: MoleExecution) = {
    val jobs =
      for {
        (capsule, groups) ← moleExecution.waitingJobs
        (_, jobs) ← groups.toList
        job ← jobs
      } submit(moleExecution, Job(moleExecution, job), capsule)
    moleExecution.nbWaiting = 0
    moleExecution.waitingJobs.clear
  }

  def checkIfSubMoleIsFinished(subMoleExecutionState: SubMoleExecutionState) = {
    def hasMessages = subMoleExecutionState.moleExecution.messageQueue.all.exists(MoleExecutionMessage.msgForSubMole(_, subMoleExecutionState))

    if (subMoleExecutionState.nbJobs == 0 && !hasMessages) {
      subMoleExecutionState.onFinish.foreach(_(subMoleExecutionState))
      MoleExecution.clean(subMoleExecutionState)
    }
  }

  def checkAllWaiting(moleExecution: MoleExecution) =
    if (moleExecution.rootSubMoleExecution.nbJobs <= moleExecution.nbWaiting) MoleExecution.submitAll(moleExecution)

  def checkMoleExecutionIsFinished(moleExecution: MoleExecution) =
    if (moleExecution.messageQueue.isEmpty && moleExecution.rootSubMoleExecution.nbJobs == 0) MoleExecution.finish(moleExecution)

  def allJobs(moleExecution: MoleExecution) = moleExecution.subMoleExecutions.values.flatMap(_.jobs.toVector.map { case (mj, c) ⇒ c -> mj })

  def capsuleStatuses(moleExecution: MoleExecution, jobs: Seq[(Capsule, MoleJob)], completed: Map[Capsule, Long]): CapsuleStatuses = {

    val runningSet: java.util.HashSet[UUID] = {
      def submissionEnvironments = moleExecution.environments.values.toSeq.collect { case e: SubmissionEnvironment ⇒ e }
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

    val ready = collection.mutable.Map[Capsule, Long]()
    val running = collection.mutable.Map[Capsule, Long]()

    def increment(map: collection.mutable.Map[Capsule, Long], key: Capsule) = {
      val value = map.getOrElse(key, 0L)
      map.update(key, value + 1)
    }

    for {
      (capsule, moleJob) ← jobs
    } {
      if (isRunningOnEnvironment(moleJob)) increment(running, capsule)
      else
        moleJob.state match {
          case READY   ⇒ increment(ready, capsule)
          case RUNNING ⇒ increment(running, capsule)
          case _       ⇒
        }
    }

    moleExecution.mole.capsules.map { c ⇒
      c ->
        MoleExecution.JobStatuses(
          ready = ready.getOrElse(c, 0L),
          running = running.getOrElse(c, 0L),
          completed = completed.getOrElse(c, 0L)
        )
    }.toMap
  }

  class SubMoleExecutionState(
    val id:            SubMoleExecution,
    val parent:        Option[SubMoleExecutionState],
    val moleExecution: MoleExecution) {

    import moleExecution.executionContext.services._

    var nbJobs = 0
    var children = collection.mutable.TreeMap[SubMoleExecution, SubMoleExecutionState]()
    var jobs = collection.mutable.TreeMap[MoleJob, Capsule]()
    var canceled = false
    val onFinish = collection.mutable.ListBuffer[(SubMoleExecutionState ⇒ Any)]()
    val masterCapsuleRegistry = new MasterCapsuleRegistry
    val aggregationTransitionRegistry = new AggregationTransitionRegistry
    val transitionRegistry = new TransitionRegistry
    lazy val masterCapsuleExecutor = Executors.newSingleThreadExecutor(threadProvider.threadFactory)
  }

}

object SubMoleExecution {
  implicit def ordering: Ordering[SubMoleExecution] = Ordering.by[SubMoleExecution, Long](_.id)
}

case class SubMoleExecution(id: Long) extends AnyVal

sealed trait MoleExecutionMessage

object MoleExecutionMessage {
  case class PerformTransition(subMoleExecution: SubMoleExecution)(val operation: SubMoleExecutionState ⇒ Unit) extends MoleExecutionMessage
  case class JobFinished(subMoleExecution: SubMoleExecution)(val job: MoleJob, val state: State, val capsule: Capsule, val ticket: Ticket) extends MoleExecutionMessage
  case class WithMoleExecutionSate(operation: MoleExecution ⇒ Unit) extends MoleExecutionMessage
  case class StartMoleExecution(context: Option[Context]) extends MoleExecutionMessage
  case class CancelMoleExecution() extends MoleExecutionMessage
  case class RegisterJob(subMoleExecution: SubMoleExecutionState, job: MoleJob, capsule: Capsule) extends MoleExecutionMessage

  def msgForSubMole(msg: MoleExecutionMessage, subMoleExecutionState: SubMoleExecutionState) = msg match {
    case msg: PerformTransition ⇒ msg.subMoleExecution == subMoleExecutionState.id
    case msg: JobFinished       ⇒ msg.subMoleExecution == subMoleExecutionState.id
    case _                      ⇒ false
  }

  def messagePriority(moleExcutionMessage: MoleExecutionMessage) =
    moleExcutionMessage match {
      case _: RegisterJob         ⇒ 200
      case _: CancelMoleExecution ⇒ 100
      case _: PerformTransition   ⇒ 10
      case _                      ⇒ 1
    }

  def send(moleExecution: MoleExecution)(moleExecutionMessage: MoleExecutionMessage, priority: Option[Int] = None) =
    moleExecution.messageQueue.enqueue(moleExecutionMessage, priority getOrElse messagePriority(moleExecutionMessage))

  def processJobFinished(moleExecution: MoleExecution, msg: JobFinished) = {
    val state = moleExecution.subMoleExecutions(msg.subMoleExecution)
    MoleExecution.finalState(state, msg.job, msg.state, msg.capsule, msg.ticket)
    MoleExecution.checkIfSubMoleIsFinished(state)
  }

  def dispatch(moleExecution: MoleExecution, msg: MoleExecutionMessage) = moleExecution.synchronized {
    try {
      if (!moleExecution._canceled)
        msg match {
          case msg: PerformTransition ⇒
            val state = moleExecution.subMoleExecutions(msg.subMoleExecution)
            if (!state.canceled) msg.operation(state)
            MoleExecution.checkIfSubMoleIsFinished(state)
          case msg: JobFinished           ⇒ processJobFinished(moleExecution, msg)
          case msg: WithMoleExecutionSate ⇒ msg.operation(moleExecution)
          case msg: StartMoleExecution    ⇒ MoleExecution.start(moleExecution, msg.context)
          case msg: CancelMoleExecution   ⇒ MoleExecution.cancel(moleExecution, None)
          case msg: RegisterJob           ⇒ msg.subMoleExecution.jobs.put(msg.job, msg.capsule)
        }
    }
    catch {
      case t: Throwable ⇒ MoleExecution.cancel(moleExecution, Some(MoleExecution.MoleExecutionError(t)))
    }

    MoleExecution.checkAllWaiting(moleExecution)
    MoleExecution.checkMoleExecutionIsFinished(moleExecution)
  }

  def dispatcher(moleExecution: MoleExecution) =
    while (!(moleExecution._finished || moleExecution._canceled)) {
      val msg = moleExecution.messageQueue.dequeue()
      dispatch(moleExecution, msg)
    }

}

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
  val id:                          String,
  val taskCache:                   KeyValueCache,
  val lockRepository:              LockRepository[LockKey]
) {

  val messageQueue = PriorityQueue[MoleExecutionMessage](fifo = true)

  private[mole] var _started = false
  private[mole] var _canceled = false
  private[mole] var _finished = false

  def started = synchronized(_started)
  def canceled = synchronized(_canceled)
  def finished = synchronized(_finished)

  private[mole] var _startTime: Option[Long] = None
  private[mole] var _endTime: Option[Long] = None

  def startTime = synchronized(_startTime)
  def endTime = synchronized(_endTime)

  private[mole] var ticketNumber = 1L
  private[mole] val rootTicket = Ticket(id, 0)

  private[mole] val waitingJobs = collection.mutable.Map[Capsule, collection.mutable.Map[MoleJobGroup, ListBuffer[MoleJob]]]()
  private[mole] var nbWaiting = 0

  private[mole] val completed = {
    val map = collection.mutable.Map[Capsule, Long]()
    map ++= mole.capsules.map(_ -> 0L)
    map
  }

  lazy val environmentInstances = environmentProviders.toVector.map { case (k, v) ⇒ v }.distinct.map { v ⇒ v → v() }.toMap
  lazy val environments = environmentProviders.toVector.map { case (k, v) ⇒ k → environmentInstances(v) }.toMap
  lazy val defaultEnvironment = defaultEnvironmentProvider()
  def allEnvironments = (environmentInstances.values ++ Seq(defaultEnvironment)).toVector.distinct

  lazy val rootSubMoleExecution = MoleExecution.newSubMoleExecution(None, this)
  lazy val subMoleExecutions = collection.mutable.TreeMap[SubMoleExecution, SubMoleExecutionState]()

  private[mole] var currentSubMoleExecutionId = 0L

  private[workflow] val dataChannelRegistry = new RegistryWithTicket[DataChannel, Buffer[Variable[_]]]
  private[mole] var _exception = Option.empty[MoleExecutionFailed]

  def exception = synchronized(_exception)

  def duration: Option[Long] = synchronized {
    (_startTime, _endTime) match {
      case (None, _)          ⇒ None
      case (Some(t), None)    ⇒ Some(System.currentTimeMillis - t)
      case (Some(s), Some(e)) ⇒ Some(e - s)
    }
  }

  def run: Unit = run(None)

  def validate = {
    import executionContext.services._
    val validationErrors = Validation(mole, implicits, sources, hooks)
    if (!validationErrors.isEmpty) throw new UserBadDataError(s"Formal validation of your mole has failed, ${validationErrors.size} error(s) has(ve) been found.\n" + validationErrors.mkString("\n") + s"\nIn mole: $mole")
  }

  def run(context: Option[Context] = None, validate: Boolean = true) = {
    if (!_started) {
      if (validate) this.validate
      MoleExecutionMessage.send(this)(MoleExecutionMessage.StartMoleExecution(context))
      MoleExecutionMessage.dispatcher(this)
      _exception.foreach(e ⇒ throw e.exception)
      this
    }
    else this
  }

  def start = {
    validate
    val t = executionContext.services.threadProvider.newThread { () ⇒ run(None, validate = false) }
    t.start()
    this
  }

  def cancel = MoleExecutionMessage.send(this)(MoleExecutionMessage.CancelMoleExecution())
  def capsuleStatuses = {
    val (jobs, cmp) = synchronized { (MoleExecution.allJobs(this).toVector, completed.toMap) }
    MoleExecution.capsuleStatuses(this, jobs, cmp)
  }

}
