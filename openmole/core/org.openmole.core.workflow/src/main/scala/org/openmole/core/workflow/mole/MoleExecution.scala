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
import java.util.concurrent.{Executors, Semaphore}
import java.util.logging.Level
import org.openmole.core.context.{CompactedContext, Context, PrototypeSet, Variable}
import org.openmole.core.event.*
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.composition.Puzzle
import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.execution.*
import org.openmole.core.workflow.grouping.Grouping
import org.openmole.core.workflow.hook.{Hook, HookExecutionContext}
import org.openmole.core.workflow.job.State.*
import org.openmole.core.workflow.job.*
import org.openmole.core.workflow.mole
import org.openmole.core.workflow.mole.MoleExecution.{Cleaned, MoleExecutionFailed, SubMoleExecutionState}
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workflow.transition.{AggregationTransition, DataChannel, Transition, TransitionSlot}
import org.openmole.core.workflow.validation.*
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.collection.{PriorityQueue, StaticArrayBuffer}
import org.openmole.tool.lock.*
import org.openmole.tool.thread.*
import org.openmole.tool.logger.{JavaLogger, LoggerService}
import org.openmole.core.argument.*

import scala.collection.mutable
import scala.collection.mutable.{Buffer, ListBuffer}

object MoleExecution {

  class Started extends Event[MoleExecution]
  case class Finished(canceled: Boolean) extends Event[MoleExecution]
  case class JobCreated(moleJob: Job, capsule: MoleCapsule) extends Event[MoleExecution]
  case class JobSubmitted(moleJob: JobGroup, capsule: MoleCapsule, environment: Environment) extends Event[MoleExecution]
  case class JobFinished(moleJob: JobId, context: Context, capsule: MoleCapsule) extends Event[MoleExecution]
  case class Cleaned() extends Event[MoleExecution]

  type Id = String

  object MoleExecutionFailed:
    def exception(moleExecutionError: MoleExecutionFailed) = moleExecutionError.exception
    def capsule(moleExecutionError: MoleExecutionFailed) = moleExecutionError match {
      case e: JobFailed             ⇒ Some(e.capsule)
      case e: ExceptionRaised       ⇒ Some(e.capsule)
      case e: SourceExceptionRaised ⇒ Some(e.capsule)
      case e: HookExceptionRaised   ⇒ Some(e.capsule)
      case e: MoleExecutionError    ⇒ None
    }
  

  sealed trait MoleExecutionFailed {
    def exception: Throwable
  }

  case class JobFailed(moleJob: JobId, capsule: MoleCapsule, exception: Throwable) extends Event[MoleExecution] with MoleExecutionFailed {
    def level = Level.SEVERE
  }

  case class ExceptionRaised(moleJob: JobId, capsule: MoleCapsule, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed
  case class SourceExceptionRaised(source: Source, capsule: MoleCapsule, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed
  case class HookExceptionRaised(hook: Hook, capsule: MoleCapsule, moleJob: JobId, exception: Throwable, level: Level) extends Event[MoleExecution] with MoleExecutionFailed
  case class MoleExecutionError(exception: Throwable) extends MoleExecutionFailed

  private def listOfTupleToMap[K, V](l: Iterable[(K, V)]): Map[K, Iterable[V]] = l.groupBy(_._1).map { case (k, v) ⇒ k -> v.map(_._2) }


  def apply(dsl: DSL)(implicit moleServices: MoleServices): MoleExecution =
    val p = DSL.toPuzzle(dsl)
    MoleExecution(Puzzle.toMole(p), p.sources, p.hooks, p.environments, p.grouping)

  def apply(
    mole:                        Mole,
    sources:                     Iterable[(MoleCapsule, Source)]            = Iterable.empty,
    hooks:                       Iterable[(MoleCapsule, Hook)]              = Iterable.empty,
    environments:                Map[MoleCapsule, EnvironmentProvider]      = Map.empty,
    grouping:                    Map[MoleCapsule, Grouping]                 = Map.empty,
    implicits:                   Context                                    = Context.empty,
    defaultEnvironment:          OptionalArgument[LocalEnvironmentProvider] = None,
    cleanOnFinish:               Boolean                                    = true,
    startStopDefaultEnvironment: Boolean                                    = true,
    taskCache:                   KeyValueCache                              = KeyValueCache(),
    lockRepository:              LockRepository[LockKey]                    = LockRepository()
  )(implicit moleServices: MoleServices): MoleExecution =

    def defaultDefaultEnvironment = LocalEnvironment()(varName = sourcecode.Name("local"))

    new MoleExecution(
      mole,
      listOfTupleToMap(sources),
      listOfTupleToMap(hooks),
      environments,
      grouping,
      defaultEnvironment.getOrElse(defaultDefaultEnvironment),
      cleanOnFinish,
      implicits,
      MoleExecutionContext(moleLaunchTime = moleServices.timeService.currentTime)(moleServices),
      startStopDefaultEnvironment,
      id = UUID.randomUUID().toString,
      keyValueCache = taskCache,
      lockRepository = lockRepository
    )

  type CapsuleStatuses = Map[MoleCapsule, JobStatuses]

  case class JobStatuses(ready: Long, running: Long, completed: Long)

  object AggregationTransitionRegistryRecord {
    def apply(size: Int): AggregationTransitionRegistryRecord =
      new AggregationTransitionRegistryRecord(new StaticArrayBuffer(size), new StaticArrayBuffer(size))
  }

  case class AggregationTransitionRegistryRecord(ids: StaticArrayBuffer[Long], values: StaticArrayBuffer[Array[Any]])
  type AggregationTransitionRegistry = RegistryWithTicket[AggregationTransition, AggregationTransitionRegistryRecord]
  type MasterCapsuleRegistry = RegistryWithTicket[MoleCapsule, Context]
  type TransitionRegistry = RegistryWithTicket[Transition, Iterable[Variable[_]]]

  def cancel(subMoleExecution: SubMoleExecutionState): Unit = {
    subMoleExecution.canceled = true

    val allJobs = subMoleExecution.jobs.toVector
    allJobs.foreach(j ⇒ removeJob(subMoleExecution, j))
    assert(subMoleExecution.jobs.isEmpty)

    val children = subMoleExecution.children.values.toVector
    children.foreach(cancel)

    removeSubMole(subMoleExecution)
  }

  def removeJob(subMoleExecutionState: SubMoleExecutionState, job: JobId) = {
    val removed = subMoleExecutionState.jobs.remove(job)
    subMoleExecutionState.moleExecution.jobs.remove(job)
    if (removed) updateNbJobs(subMoleExecutionState, -1)
  }

  def addJob(subMoleExecution: SubMoleExecutionState, job: JobId, capsule: MoleCapsule) =
    if (!subMoleExecution.canceled) {
      subMoleExecution.jobs.add(job)
      subMoleExecution.moleExecution.jobs.put(job, capsule)
      updateNbJobs(subMoleExecution, 1)
    }

  def updateNbJobs(subMoleExecutionState: SubMoleExecutionState, v: Int): Unit = {
    import subMoleExecutionState.moleExecution.executionContext.services._
    LoggerService.log(Level.FINE, s"update number of jobs of sub mole execution ${subMoleExecutionState}, add ${v} to ${subMoleExecutionState.nbJobs}")

    subMoleExecutionState.nbJobs = subMoleExecutionState.nbJobs + v
    subMoleExecutionState.parent.foreach(s ⇒ updateNbJobs(s, v))
  }

  def submit(subMoleExecutionState: SubMoleExecutionState, capsule: MoleCapsule, context: Context, ticket: Ticket): Unit = {
    import subMoleExecutionState.moleExecution.executionContext.services._
    if (!subMoleExecutionState.canceled) {
      val jobId = nextJobId(subMoleExecutionState.moleExecution)
      MoleExecution.addJob(subMoleExecutionState, jobId, capsule)

      val sourced =
        subMoleExecutionState.moleExecution.sources(capsule).foldLeft(Context.empty) {
          case (a, s) ⇒
            val ctx = try s.perform(subMoleExecutionState.moleExecution.implicits + context, subMoleExecutionState.moleExecution.executionContext)
            catch {
              case t: Throwable ⇒
                LoggerService.fine("Error in submole execution", t)
                val event = MoleExecution.SourceExceptionRaised(s, capsule, t, Level.SEVERE)
                eventDispatcher.trigger(subMoleExecutionState.moleExecution, event)
                cancel(subMoleExecutionState.moleExecution, Some(event))
                throw new InternalProcessingError(t, s"Error in source execution that is plugged to $capsule")
            }
            a + ctx
        } + Variable(Variable.openMOLESeed, seeder.newSeed)

      capsule.master match {
        case Some(master) ⇒
          //          def stateChanged(job: MoleJob, oldState: State, newState: State) =
          //            eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobStatusChanged(job, c, newState, oldState))

          subMoleExecutionState.masterCapsuleExecutor.submit {
            try
              val savedContext = subMoleExecutionState.masterCapsuleRegistry.remove(capsule, ticket.parentOrException).getOrElse(Context.empty)
              val runtimeTask = subMoleExecutionState.moleExecution.runtimeTask(capsule)
              val moleJob: Job = Job(runtimeTask, subMoleExecutionState.moleExecution.implicits + sourced + context + savedContext, jobId, (_, _) ⇒ (), () ⇒ subMoleExecutionState.canceled)

              eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobCreated(moleJob, capsule))

              val taskExecutionDirectory = moleExecutionDirectory.newDirectory("taskExecution")
              val result =
                try
                  val taskContext =
                    TaskExecutionContext.complete(
                      subMoleExecutionState.moleExecution.partialTaskExecutionContext,
                      taskExecutionDirectory = taskExecutionDirectory,
                      localEnvironment = subMoleExecutionState.moleExecution.defaultEnvironment
                    )

                  moleJob.perform(taskContext)
                finally taskExecutionDirectory.recursiveDelete

              Job.finish(moleJob, result) // Does nothing

              result match
                case Left(newContext) ⇒ subMoleExecutionState.masterCapsuleRegistry.register(capsule, ticket.parentOrException, MasterCapsule.toPersist(master, newContext))
                case _                ⇒

              MoleExecutionMessage.send(subMoleExecutionState.moleExecution)(MoleExecutionMessage.JobFinished(subMoleExecutionState.id)(jobId, result.swap.map(CompactedContext.compact).swap, capsule, ticket))

            catch
              case t: Throwable ⇒ MoleExecutionMessage.send(subMoleExecutionState.moleExecution)(MoleExecutionMessage.MoleExecutionError(t))

          }
        case _ ⇒
          case class JobCallBackClosure(subMoleExecutionState: SubMoleExecutionState, capsule: MoleCapsule, ticket: Ticket) extends Job.CallBack {
            def subMoleCanceled() = subMoleExecutionState.canceled
            def jobFinished(job: JobId, result: Either[Context, Throwable]) =
              MoleExecutionMessage.send(subMoleExecutionState.moleExecution)(MoleExecutionMessage.JobFinished(subMoleExecutionState.id)(job, result.swap.map(CompactedContext.compact).swap, capsule, ticket))
          }

          val newContext = subMoleExecutionState.moleExecution.implicits + sourced + context
          val runtimeTask = subMoleExecutionState.moleExecution.runtimeTask(capsule)
          val moleJob: Job = Job(runtimeTask, newContext, jobId, JobCallBackClosure(subMoleExecutionState, capsule, ticket))

          eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobCreated(moleJob, capsule))

          group(subMoleExecutionState.moleExecution, moleJob, newContext, capsule)
      }

    }
  }

  def processJobFinished(moleExecution: MoleExecution, msg: mole.MoleExecutionMessage.JobFinished) =
    if (!MoleExecution.moleJobIsFinished(moleExecution, msg.job)) {
      val state = moleExecution.subMoleExecutions(msg.subMoleExecution)
      if (!state.canceled) MoleExecution.processFinalState(state, msg.job, msg.result.swap.map(CompactedContext.expand).swap, msg.capsule, msg.ticket)
      removeJob(state, msg.job)
      MoleExecution.checkIfSubMoleIsFinished(state)
    }

  def performHooksAndTransitions(subMoleExecutionState: SubMoleExecutionState, job: JobId, context: Context, capsule: MoleCapsule, ticket: Ticket) = {
    val mole = subMoleExecutionState.moleExecution.mole

    def ctxForHooks = (subMoleExecutionState.moleExecution.implicits + context) - Variable.openMOLESeed + Variable(Variable.openMOLEExperiment, ticket.content)

    def executeHook(h: Hook) =
      try {
        def toHookExecutionContext(cache: KeyValueCache, executionContext: MoleExecutionContext) = {
          val services = executionContext.services
          HookExecutionContext(
            cache = cache,
            ticket = ticket,
            moleLaunchTime = executionContext.moleLaunchTime,
            jobId = job,
            moleExecutionId = subMoleExecutionState.moleExecution.id)(
            preference = services.preference,
            threadProvider = services.threadProvider,
            fileService = services.fileService,
            workspace = services.workspace,
            outputRedirection = services.outputRedirection,
            loggerService = services.loggerService,
            random = services.newRandom,
            tmpDirectory = services.tmpDirectory,
            serializerService = services.serializerService,
            timeService = services.timeService)
        }

        h.perform(ctxForHooks, toHookExecutionContext(subMoleExecutionState.moleExecution.keyValueCache, subMoleExecutionState.moleExecution.executionContext))
      }
      catch {
        case e: Throwable ⇒
          import subMoleExecutionState.moleExecution.executionContext.services._
          val event = MoleExecution.HookExceptionRaised(h, capsule, job, e, Level.SEVERE)
          eventDispatcher.trigger(subMoleExecutionState.moleExecution, event)
          cancel(subMoleExecutionState.moleExecution, Some(event))
          throw e
      }

    try {
      val hooksVariables = subMoleExecutionState.moleExecution.hooks(capsule).flatMap(h ⇒ executeHook(h).values)
      val newContext = context ++ hooksVariables
      mole.outputDataChannels(capsule).toSeq.foreach { d ⇒ DataChannel.provides(d, subMoleExecutionState.moleExecution.implicits + newContext, ticket, subMoleExecutionState.moleExecution) }

      for {
        transition ← mole.outputTransitions(capsule).toList.sortBy(t ⇒ mole.slots(t.end.capsule).size).reverse
      } transition.perform(subMoleExecutionState.moleExecution.implicits + context, ticket, subMoleExecutionState.moleExecution, subMoleExecutionState.id, subMoleExecutionState.moleExecution.executionContext)

    }
    catch {
      case t: Throwable ⇒
        import subMoleExecutionState.moleExecution.executionContext.services._
        LoggerService.fine("Error in submole execution", t)
        val event = MoleExecution.ExceptionRaised(job, capsule, t, Level.SEVERE)
        import subMoleExecutionState.moleExecution.executionContext.services._
        eventDispatcher.trigger(subMoleExecutionState.moleExecution, event)
        cancel(subMoleExecutionState.moleExecution, Some(event))
        throw t
    }
  }

  def newSubMoleExecution(
    parent:        Option[SubMoleExecutionState],
    moleExecution: MoleExecution) = {
    val id: SubMoleExecution = moleExecution.currentSubMoleExecutionId
    moleExecution.currentSubMoleExecutionId += 1
    val sm = new SubMoleExecutionState(id, parent, moleExecution)
    parent.foreach(_.children.put(id, sm))
    moleExecution.subMoleExecutions.put(id, sm)
    sm
  }

  def newChildSubMoleExecution(subMoleExecution: SubMoleExecutionState): SubMoleExecutionState =
    newSubMoleExecution(Some(subMoleExecution), subMoleExecution.moleExecution)

  def processFinalState(subMoleExecutionState: SubMoleExecutionState, job: JobId, result: Either[Context, Throwable], capsule: MoleCapsule, ticket: Ticket) = {
    result match {
      case Right(e) ⇒
        import subMoleExecutionState.moleExecution.executionContext.services._
        val error = MoleExecution.JobFailed(job, capsule, e)
        cancel(subMoleExecutionState.moleExecution, Some(error))
        LoggerService.fine(s"Error in user job execution for capsule $capsule, job state is FAILED.", e)
        subMoleExecutionState.moleExecution.executionContext.services.eventDispatcher.trigger(subMoleExecutionState.moleExecution, error)
      case Left(context) ⇒
        subMoleExecutionState.moleExecution.completed(capsule) = subMoleExecutionState.moleExecution.completed(capsule) + 1
        subMoleExecutionState.moleExecution.executionContext.services.eventDispatcher.trigger(subMoleExecutionState.moleExecution, MoleExecution.JobFinished(job, context, capsule))
        performHooksAndTransitions(subMoleExecutionState, job, context, capsule, ticket)
    }
  }

  /* -------------- Mole Execution ----------------- */

  def start(moleExecution: MoleExecution, context: Option[Context]) =
    if !moleExecution._started && !moleExecution._canceled
    then
      import moleExecution.executionContext.services._
      LoggerService.fine("Starting mole execution")

      def startEnvironments() = {
        if (moleExecution.startStopDefaultEnvironment) moleExecution.defaultEnvironment.start()
        moleExecution.environments.values.foreach(_.start())
      }

      import moleExecution.executionContext.services._

      tmpDirectory.directory.mkdirs()
      moleExecution._started = true
      moleExecution._startTime = Some(System.currentTimeMillis)
      eventDispatcher.trigger(moleExecution, new MoleExecution.Started)
      startEnvironments()
      submit(moleExecution.rootSubMoleExecution, moleExecution.mole.root, context.getOrElse(Context.empty), nextTicket(moleExecution, moleExecution.rootTicket))
      checkAllWaiting(moleExecution)

  private def finish(moleExecution: MoleExecution, canceled: Boolean = false) =
    if !moleExecution._finished
    then
      import moleExecution.executionContext.services._
      LoggerService.fine(s"finish mole execution $moleExecution, canceled ${canceled}")

      moleExecution._finished = true
      moleExecution._endTime = Some(System.currentTimeMillis)
      moleExecution.executionContext.services.eventDispatcher.trigger(moleExecution, MoleExecution.Finished(canceled = canceled))
      moleExecution.finishedSemaphore.release()

      moleExecution.executionContext.services.threadProvider.enqueue(ThreadProvider.maxPriority) { () ⇒
        def stopEnvironments() =
          if (moleExecution.startStopDefaultEnvironment) moleExecution.defaultEnvironment.stop()
          moleExecution.environments.values.foreach(_.stop())

        try stopEnvironments()
        finally MoleExecutionMessage.send(moleExecution)(MoleExecutionMessage.CleanMoleExecution())
      }

  def clean(moleExecution: MoleExecution) =
    import moleExecution.executionContext.services._
    LoggerService.log(Level.FINE, s"clean mole execution $moleExecution")

    try if (moleExecution.cleanOnFinish) MoleServices.clean(moleExecution.executionContext.services)
    finally
      moleExecution._cleaned = true
      moleExecution.cleanedSemaphore.release()
      moleExecution.executionContext.services.eventDispatcher.trigger(moleExecution, MoleExecution.Cleaned())

  def cancel(moleExecution: MoleExecution, t: Option[MoleExecutionFailed]): Unit =
    if !moleExecution._canceled
    then
      import moleExecution.executionContext.services._
      LoggerService.log(Level.FINE, s"cancel mole execution $moleExecution, with error $t")

      moleExecution._exception = t
      cancel(moleExecution.rootSubMoleExecution)
      moleExecution._canceled = true
      finish(moleExecution, canceled = true)

  def nextTicket(moleExecution: MoleExecution, parent: Ticket): Ticket = {
    val ticket = Ticket(parent, moleExecution.ticketNumber)
    moleExecution.ticketNumber = moleExecution.ticketNumber + 1
    ticket
  }

  def nextJobId(moleExecution: MoleExecution) = {
    val id = moleExecution.moleId
    moleExecution.moleId += 1
    id
  }

  def group(moleExecution: MoleExecution, moleJob: Job, context: Context, capsule: MoleCapsule) = {
    moleExecution.grouping.get(capsule) match {
      case Some(strategy) ⇒
        val groups = moleExecution.waitingJobs.getOrElseUpdate(capsule, collection.mutable.Map())
        val category = strategy.apply(context, groups.toVector)(moleExecution.newGroup, moleExecution.executionContext.services.defaultRandom)
        val jobs = groups.getOrElseUpdate(category, ListBuffer())
        jobs.append(moleJob)
        moleExecution.nbWaiting += 1

        if (strategy.complete(jobs)) {
          groups -= category
          moleExecution.nbWaiting -= jobs.size
          Some(JobGroup(moleExecution, jobs.toVector) → capsule)
        }
        else None
      case None ⇒
        val job = JobGroup(moleExecution, Vector(moleJob))
        Some(job → capsule)
    }
  }.foreach { case (j, c) ⇒ submit(moleExecution, j, c) }

  def submit(moleExecution: MoleExecution, job: JobGroup, capsule: MoleCapsule) = {
    val env = moleExecution.environmentForCapsule.getOrElse(capsule, moleExecution.defaultEnvironment)
    Environment.submit(env, job)
    moleExecution.executionContext.services.eventDispatcher.trigger(moleExecution, MoleExecution.JobSubmitted(job, capsule, env))
  }

  def submitAll(moleExecution: MoleExecution) = {
    for {
      (capsule, groups) ← moleExecution.waitingJobs
      (_, jobs) ← groups.toList
    } submit(moleExecution, JobGroup(moleExecution, jobs), capsule)
    moleExecution.nbWaiting = 0
    moleExecution.waitingJobs.clear
  }

  def removeSubMole(subMoleExecutionState: SubMoleExecutionState) = {
    subMoleExecutionState.parent.foreach(s ⇒ s.children.remove(subMoleExecutionState.id))
    subMoleExecutionState.moleExecution.subMoleExecutions.remove(subMoleExecutionState.id)
  }

  def checkIfSubMoleIsFinished(state: SubMoleExecutionState) = {
    def hasMessages = state.moleExecution.messageQueue.all.exists(MoleExecutionMessage.msgForSubMole(_, state))

    if (state.nbJobs == 0 && !hasMessages) {
      state.onFinish.foreach(_(state))
      removeSubMole(state)
    }
  }

  def moleJobIsFinished(moleExecution: MoleExecution, id: JobId) = !moleExecution.jobs.contains(id)

  def checkAllWaiting(moleExecution: MoleExecution) =
    if (moleExecution.rootSubMoleExecution.nbJobs <= moleExecution.nbWaiting) MoleExecution.submitAll(moleExecution)

  def checkMoleExecutionIsFinished(moleExecution: MoleExecution) = {
    import moleExecution.executionContext.services._

    def jobs = if (moleExecution.rootSubMoleExecution.nbJobs <= 5) s": ${moleExecution.jobs}" else ""
    def subMoles = if (moleExecution.rootSubMoleExecution.nbJobs <= 5) s" - ${moleExecution.subMoleExecutions.map(s ⇒ s._2.canceled -> s._2.jobs)}" else ""
    LoggerService.log(Level.FINE, s"check if mole execution $moleExecution is finished, message queue empty ${moleExecution.messageQueue.isEmpty}, number of jobs ${moleExecution.rootSubMoleExecution.nbJobs}${jobs}${subMoles}")
    if (moleExecution.messageQueue.isEmpty && moleExecution.rootSubMoleExecution.nbJobs == 0) MoleExecution.finish(moleExecution)
  }

  def allJobIds(moleExecution: MoleExecution) = moleExecution.jobs.toVector

  def capsuleStatuses(
    moleExecution: MoleExecution,
    jobs:          Array[JobId],
    capsules:      Array[MoleCapsule],
    completed:     Map[MoleCapsule, Long]): CapsuleStatuses = {

    val runningSet: java.util.HashSet[Long] = {
      def submissionEnvironments = moleExecution.environments.values.toSeq.collect { case e: SubmissionEnvironment ⇒ e }
      def localEnvironments = moleExecution.environments.values.toSeq.collect { case e: LocalEnvironment ⇒ e } ++ Seq(moleExecution.defaultEnvironment)

      val set = new java.util.HashSet[Long](jobs.size + 1, 1.0f)

      for {
        env ← submissionEnvironments
        ej ← env.runningJobs
        id ← ej.moleJobIds
      } set.add(id)

      for {
        env ← localEnvironments
        ej ← env.runningJobs
      } set.add(ej.id)

      set
    }

    def isRunning(moleJob: JobId): Boolean = runningSet.contains(moleJob)

    val ready = collection.mutable.Map[MoleCapsule, Long]()
    val running = collection.mutable.Map[MoleCapsule, Long]()

    def increment(map: collection.mutable.Map[MoleCapsule, Long], key: MoleCapsule) = {
      val value = map.getOrElse(key, 0L)
      map.update(key, value + 1)
    }

    for {
      i ← 0 until jobs.size
    } {
      val moleJob = jobs(i)
      val capsule = capsules(i)
      if (isRunning(moleJob)) increment(running, capsule)
      else increment(ready, capsule)
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

    var nbJobs = 0L
    var children = collection.mutable.LongMap[SubMoleExecutionState]()
    var jobs = collection.mutable.HashSet[JobId]()

    @volatile var canceled = false

    val onFinish = collection.mutable.ListBuffer[(SubMoleExecutionState ⇒ Any)]()
    val masterCapsuleRegistry = new MasterCapsuleRegistry
    val aggregationTransitionRegistry = new AggregationTransitionRegistry
    val transitionRegistry = new TransitionRegistry
    lazy val masterCapsuleExecutor = Executors.newSingleThreadExecutor(threadProvider.threadFactory)
  }

  def cachedValidTypes(moleExecution: MoleExecution, transitionSlot: TransitionSlot) = {
    def f = TypeUtil.validTypes(moleExecution.mole, moleExecution.sources, moleExecution.hooks)(transitionSlot)
    moleExecution.validTypeCache.synchronized { moleExecution.validTypeCache.getOrElseUpdate(transitionSlot, f) }
  }

  def cachedCapsuleInputs(moleExecution: MoleExecution, capsule: MoleCapsule) = {
    def f = capsule.inputs(moleExecution.mole, moleExecution.sources, moleExecution.hooks)
    moleExecution.capsuleInputCache.synchronized { moleExecution.capsuleInputCache.getOrElseUpdate(capsule, f) }
  }

  object SynchronisationContext {
    implicit def default: Synchronized.type = Synchronized
    def apply[T](th: Any, op: ⇒ T)(implicit s: SynchronisationContext) =
      s match {
        case MoleExecution.Synchronized ⇒ synchronized(op)
        case MoleExecution.UnsafeAccess ⇒ op
      }

  }

  sealed trait SynchronisationContext
  case object Synchronized extends SynchronisationContext
  case object UnsafeAccess extends SynchronisationContext

  def validationErrors(moleExecution: MoleExecution) =
    import moleExecution.executionContext.services._
    given KeyValueCache = moleExecution.keyValueCache
    Validation(moleExecution.mole, moleExecution.implicits, moleExecution.sources, moleExecution.hooks)
}

sealed trait MoleExecutionMessage

object MoleExecutionMessage {
  case class PerformTransition(subMoleExecution: SubMoleExecution)(val operation: SubMoleExecutionState ⇒ Unit) extends MoleExecutionMessage
  case class JobFinished(subMoleExecution: SubMoleExecution)(val job: JobId, val result: Either[CompactedContext, Throwable], val capsule: MoleCapsule, val ticket: Ticket) extends MoleExecutionMessage
  case class WithMoleExecutionSate(operation: MoleExecution ⇒ Unit) extends MoleExecutionMessage
  case class StartMoleExecution(context: Option[Context]) extends MoleExecutionMessage
  case class CancelMoleExecution() extends MoleExecutionMessage
  case class CleanMoleExecution() extends MoleExecutionMessage
  case class MoleExecutionError(t: Throwable) extends MoleExecutionMessage

  def msgForSubMole(msg: MoleExecutionMessage, subMoleExecutionState: SubMoleExecutionState) = msg match {
    case msg: PerformTransition ⇒ msg.subMoleExecution == subMoleExecutionState.id
    case msg: JobFinished       ⇒ msg.subMoleExecution == subMoleExecutionState.id
    case _                      ⇒ false
  }

  def messagePriority(moleExecutionMessage: MoleExecutionMessage) =
    moleExecutionMessage match {
      case _: CancelMoleExecution ⇒ 100
      case _: PerformTransition   ⇒ 20
      case _                      ⇒ 1
    }

  def send(moleExecution: MoleExecution)(moleExecutionMessage: MoleExecutionMessage, priority: Option[Int] = None) =
    moleExecution.messageQueue.enqueue(moleExecutionMessage, priority getOrElse messagePriority(moleExecutionMessage))

  def dispatch(moleExecution: MoleExecution, msg: MoleExecutionMessage) = moleExecution.synchronized {
    import moleExecution.executionContext.services._
    LoggerService.log(Level.FINE, s"processing message $msg in mole execution $moleExecution")

    try
      msg match
        case msg: PerformTransition ⇒
          if !moleExecution._canceled
          then
            moleExecution.subMoleExecutions.get(msg.subMoleExecution) foreach { state ⇒
              if (!state.canceled) msg.operation(state)
              MoleExecution.checkIfSubMoleIsFinished(state)
            }
        case msg: JobFinished           ⇒ MoleExecution.processJobFinished(moleExecution, msg)
        case msg: StartMoleExecution    ⇒ MoleExecution.start(moleExecution, msg.context)
        case msg: CancelMoleExecution   ⇒ MoleExecution.cancel(moleExecution, None)
        case msg: WithMoleExecutionSate ⇒ msg.operation(moleExecution)
        case msg: CleanMoleExecution    ⇒ MoleExecution.clean(moleExecution)
        case msg: MoleExecutionError    ⇒ MoleExecution.cancel(moleExecution, Some(MoleExecution.MoleExecutionError(msg.t)))
    catch
      case t: Throwable ⇒ MoleExecution.cancel(moleExecution, Some(MoleExecution.MoleExecutionError(t)))

    MoleExecution.checkAllWaiting(moleExecution)
    MoleExecution.checkMoleExecutionIsFinished(moleExecution)
  }

  def dispatcher(moleExecution: MoleExecution) =
    while (!(moleExecution._cleaned)) {
      val msg = moleExecution.messageQueue.dequeue()
      dispatch(moleExecution, msg)
    }



}

class MoleExecution(
  val mole:                        Mole,
  val sources:                     Sources,
  val hooks:                       Hooks,
  val environmentProviders:        Map[MoleCapsule, EnvironmentProvider],
  val grouping:                    Map[MoleCapsule, Grouping],
  val defaultEnvironmentProvider:  LocalEnvironmentProvider,
  val cleanOnFinish:               Boolean,
  val implicits:                   Context,
  val executionContext:            MoleExecutionContext,
  val startStopDefaultEnvironment: Boolean,
  val id:                          MoleExecution.Id,
  val keyValueCache:               KeyValueCache,
  val lockRepository:              LockRepository[LockKey]
) { moleExecution ⇒

  val messageQueue = PriorityQueue[MoleExecutionMessage](fifo = false)

  private[mole] var _started = false
  private[mole] var _canceled = false
  private[mole] var _finished = false
  private[mole] var _cleaned = false

  private val finishedSemaphore = new Semaphore(0)
  private val cleanedSemaphore = new Semaphore(0)

  def sync[T](op: ⇒ T)(implicit s: MoleExecution.SynchronisationContext) = MoleExecution.SynchronisationContext(this, op)

  def started(implicit s: MoleExecution.SynchronisationContext) = sync(_started)
  def canceled(implicit s: MoleExecution.SynchronisationContext) = sync(_canceled)
  def finished(implicit s: MoleExecution.SynchronisationContext) = sync(_finished)

  def cleaned(implicit s: MoleExecution.SynchronisationContext) = sync(_cleaned)

  private[mole] var _startTime: Option[Long] = None
  private[mole] var _endTime: Option[Long] = None

  def startTime(implicit s: MoleExecution.SynchronisationContext) = sync(_startTime)
  def endTime(implicit s: MoleExecution.SynchronisationContext) = sync(_endTime)

  private[mole] var ticketNumber = 1L
  private[mole] val rootTicket = Ticket.root(0L)

  private[mole] var moleId = 0L

  private[mole] val newGroup = NewGroup()

  private[mole] val waitingJobs = collection.mutable.Map[MoleCapsule, collection.mutable.Map[MoleJobGroup, ListBuffer[Job]]]()
  private[mole] var nbWaiting = 0

  private[mole] val completed = {
    val map = collection.mutable.Map[MoleCapsule, Long]()
    map ++= mole.capsules.map(_ -> 0L)
    map
  }

  /* Caches to speedup workflow execution */
  private val validTypeCache = collection.mutable.HashMap[TransitionSlot, Iterable[TypeUtil.ValidType]]()
  private val capsuleInputCache = collection.mutable.HashMap[MoleCapsule, PrototypeSet]()
  
  
  lazy val partialTaskExecutionContext =
    import executionContext.services._

    TaskExecutionContext.partial(
      moleExecutionDirectory = moleExecutionDirectory,
      applicationExecutionDirectory = applicationExecutionDirectory,
      preference = preference,
      threadProvider = threadProvider,
      fileService = fileService,
      fileServiceCache = fileServiceCache,
      workspace = workspace,
      outputRedirection = outputRedirection,
      loggerService = loggerService,
      cache = keyValueCache,
      lockRepository = lockRepository,
      moleExecution = Some(moleExecution),
      serializerService = serializerService,
      networkService = networkService,
      timeService = timeService
    )

  lazy val environments = EnvironmentProvider.build(environmentProviders.values.toVector, executionContext.services, keyValueCache)
  lazy val environmentForCapsule = environmentProviders.toVector.map { case (k, v) ⇒ k → environments(v) }.toMap
  lazy val defaultEnvironment = EnvironmentProvider.buildLocal(defaultEnvironmentProvider, executionContext.services)

  lazy val runtimeTask = mole.capsules.map(c => c -> c.runtimeTask(mole, sources, hooks)).toMap
              
  def allEnvironments = (environments.values ++ Seq(defaultEnvironment)).toVector.distinct

  lazy val rootSubMoleExecution = MoleExecution.newSubMoleExecution(None, this)
  lazy val subMoleExecutions = collection.mutable.LongMap[SubMoleExecutionState]()

  private[mole] var currentSubMoleExecutionId = 0L

  private[mole] val jobs = collection.mutable.LongMap[MoleCapsule]()

  private[workflow] val dataChannelRegistry = new RegistryWithTicket[DataChannel, CompactedContext]
  private[mole] var _exception = Option.empty[MoleExecutionFailed]

  def exception(implicit s: MoleExecution.SynchronisationContext) = sync(_exception)

  def duration(implicit s: MoleExecution.SynchronisationContext): Option[Long] = sync {
    (startTime, endTime) match {
      case (None, _)          ⇒ None
      case (Some(t), None)    ⇒ Some(System.currentTimeMillis - t)
      case (Some(s), Some(e)) ⇒ Some(e - s)
    }
  }

  def run: Unit = run(None)

  def validate =
    val validationErrors = MoleExecution.validationErrors(this)
    if validationErrors.nonEmpty
    then throw new UserBadDataError(s"Formal validation has failed, ${validationErrors.size} error(s) has(ve) been found.\n" + validationErrors.mkString("\n") + s"\nIn mole: $mole")

  def run(context: Option[Context] = None, validate: Boolean = true) =
    if (!_started) {
      if (validate) this.validate
      MoleExecutionMessage.send(this)(MoleExecutionMessage.StartMoleExecution(context))
      MoleExecutionMessage.dispatcher(this)
      _exception.foreach(e ⇒ throw e.exception)
      this
    }
    else this

  def start(doValidation: Boolean) =
    import executionContext.services._
    if (doValidation) validate
    val t = threadProvider.newThread { () ⇒ run(None, validate = doValidation) }
    t.start()
    this

  def hangOn(cleaned: Boolean = true) =
    if (cleaned) cleanedSemaphore.acquireAndRelease()
    else finishedSemaphore.acquireAndRelease()
    this

  def cancel = MoleExecutionMessage.send(this)(MoleExecutionMessage.CancelMoleExecution())

  def capsuleStatuses(implicit s: MoleExecution.SynchronisationContext): MoleExecution.CapsuleStatuses =
    val (jobs, capsules, cmp) =
      sync {
        val (jobs, capsules) = moleExecution.jobs.toSeq.unzip
        (jobs.toArray, capsules.toArray, completed.toMap)
      }
    MoleExecution.capsuleStatuses(this, jobs, capsules, cmp)

}
