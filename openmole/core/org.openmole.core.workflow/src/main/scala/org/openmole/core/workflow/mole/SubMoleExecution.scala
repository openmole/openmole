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

import java.util.concurrent.{ Executors, locks }

import org.openmole.core.context._
import org.openmole.core.event._
import org.openmole.core.exception._
import org.openmole.core.workflow.job.State._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.transition._
import org.openmole.tool.lock._
import org.openmole.tool.logger._
import org.openmole.tool.random._
import org.openmole.tool.thread._

import scala.collection.mutable.Buffer
import scala.concurrent.stm._

object SubMoleExecution extends JavaLogger

import org.openmole.core.workflow.mole.SubMoleExecution.Log._

class SubMoleExecution(
  val parent:        Option[SubMoleExecution],
  val moleExecution: MoleExecution
) {

  import moleExecution.executionContext.services._

  @transient private lazy val _transitionLock = new locks.ReentrantLock()
  def transitionLock = synchronized(_transitionLock)

  private val _nbJobs = Ref(0)
  private val _children = TSet.empty[SubMoleExecution]
  private val _jobs = TMap[MoleJob, (Capsule, Ticket)]()
  private val _canceled = Ref(false)
  private val _onFinish = Ref(List[(SubMoleExecution, Ticket) ⇒ Any]())

  private lazy val masterCapsuleExecutor = Executors.newSingleThreadExecutor(threadProvider.threadFactory)

  val masterCapsuleRegistry = new RegistryWithTicket[MasterCapsule, Context]
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, Buffer[(Long, Variable[_])]]
  val transitionRegistry = new RegistryWithTicket[ITransition, Iterable[Variable[_]]]

  parentApply(_.+=(this))

  def canceled: Boolean = atomic { implicit txn ⇒
    _canceled() || (parent match {
      case Some(p) ⇒ p.canceled
      case None    ⇒ false
    })
  }

  private def rmJob(moleJob: MoleJob) = atomic { implicit txn ⇒
    _jobs.remove(moleJob)
    nbJobs_+=(-1)
  }

  private def nbJobs_+=(v: Int): Unit = atomic { implicit txn ⇒
    _nbJobs += v
    parentApply(_.nbJobs_+=(v))
  }

  def numberOfJobs = _nbJobs.single()

  def root = !parent.isDefined

  def cancel: this.type = {
    atomic { implicit txn ⇒
      _canceled() = true
      cancelJobs
      TSet.asSet(_children)
    }.foreach { _.cancel }
    parentApply(_.-=(this))
    this
  }

  def cancelJobs = _jobs.single.keys.foreach { _.cancel }

  def children = _children.single

  private def +=(submoleExecution: SubMoleExecution) =
    _children.single += submoleExecution

  private def -=(submoleExecution: SubMoleExecution) =
    _children.single -= submoleExecution

  def jobs: Seq[MoleJob] =
    atomic { implicit txn ⇒
      def allChildren(subMoleExecution: SubMoleExecution): Seq[SubMoleExecution] =
        subMoleExecution.children.toSeq ++ subMoleExecution.children.toSeq.flatMap(allChildren)

      (Seq(this) ++ allChildren(this)).flatMap(_._jobs.keys.toSeq)
    }

  private def jobFailedOrCanceled(job: MoleJob) = {
    val (capsule, ticket) = _jobs.single.get(job).getOrElse(throw new InternalProcessingError("Bug, job has not been registered."))
    val finished =
      atomic { implicit txn ⇒
        rmJob(job)
        isFinished
      }

    if (finished) finish(ticket)
    moleExecution.jobFailedOrCanceled(job, capsule)
  }

  private def jobFinished(job: MoleJob) = {
    val mole = moleExecution.mole
    val (capsule, ticket) = _jobs.single(job)
    def ctxForHooks = implicits + job.context

    def executeHook(h: Hook) =
      try h.perform(ctxForHooks, moleExecution.executionContext)
      catch {
        case e: Throwable ⇒
          val event = MoleExecution.HookExceptionRaised(h, capsule, job, e, SEVERE)
          eventDispatcher.trigger(moleExecution, event)
          moleExecution.cancel(event)
          logger.log(FINE, "Error in execution of misc " + h + "at the end of task " + job.task, e)
          throw e
      }

    try {
      val hooksVariables = moleExecution.hooks(capsule).flatMap(executeHook).unzip._2
      val context = job.context ++ hooksVariables
      mole.outputDataChannels(capsule).toSeq.foreach { _.provides(implicits + context, ticket, moleExecution) }

      transitionLock {
        for {
          transition ← mole.outputTransitions(capsule).toList.sortBy(t ⇒ mole.slots(t.end.capsule).size).reverse
        } transition.perform(implicits + context, ticket, this, moleExecution.executionContext)
      }
    }
    catch {
      case t: Throwable ⇒
        logger.log(FINE, "Error in submole execution", t)
        val event = MoleExecution.ExceptionRaised(job, capsule, t, SEVERE)
        eventDispatcher.trigger(moleExecution, event)
        moleExecution.cancel(event)
        throw t
    }
    finally {
      val finished =
        atomic { implicit txn ⇒
          rmJob(job)
          isFinished
        }

      if (finished) finish(ticket)
      moleExecution.jobFinished(job, capsule)
    }
  }

  private def isFinished = _nbJobs.single() == 0

  private def finish(ticket: Ticket) = {
    _onFinish.single().foreach(_(this, ticket))
    parentApply(_.-=(this))
  }

  def onFinish(f: (SubMoleExecution, Ticket) ⇒ Any) = atomic { implicit txn ⇒
    _onFinish() = f :: _onFinish()
  }

  def implicits =
    moleExecution.implicits

  def submit(capsule: Capsule, context: Context, ticket: Ticket) = {
    if (!canceled) {
      nbJobs_+=(1)

      def addJob(moleJob: MoleJob, capsule: Capsule, ticket: Ticket) = atomic { implicit txn ⇒
        _jobs.put(moleJob, (capsule, ticket))
      }

      val sourced =
        moleExecution.sources(capsule).foldLeft(Context.empty) {
          case (a, s) ⇒
            val ctx = try s.perform(implicits + context, moleExecution.executionContext)
            catch {
              case t: Throwable ⇒
                logger.log(FINE, "Error in submole execution", t)
                val event = MoleExecution.SourceExceptionRaised(s, capsule, t, SEVERE)
                eventDispatcher.trigger(moleExecution, event)
                moleExecution.cancel(event)
                throw new InternalProcessingError(t, s"Error in source execution that is plugged to $capsule")
            }
            a + ctx
        } + Variable(Variable.openMOLESeed, seeder.newSeed)

      capsule match {
        case c: MasterCapsule ⇒
          def stateChanged(job: MoleJob, oldState: State, newState: State) =
            eventDispatcher.trigger(moleExecution, new MoleExecution.JobStatusChanged(job, c, newState, oldState))

          masterCapsuleExecutor.submit {
            val savedContext = masterCapsuleRegistry.remove(c, ticket.parentOrException).getOrElse(Context.empty)
            val moleJob: MoleJob = MoleJob(capsule.task, implicits + sourced + context + savedContext, moleExecution.nextJobId, stateChanged)
            eventDispatcher.trigger(moleExecution, new MoleExecution.JobCreated(moleJob, capsule))
            addJob(moleJob, capsule, ticket)
            moleJob.perform(TaskExecutionContext(newFile.baseDir, moleExecution.defaultEnvironment, preference, threadProvider, fileService, workspace, moleExecution.taskCache, moleExecution.lockRepository))
            masterCapsuleRegistry.register(c, ticket.parentOrException, c.toPersist(moleJob.context))
            finalState(moleJob, moleJob.state)
          }
        case _ ⇒
          def stateChanged(job: MoleJob, oldState: State, newState: State) = {
            eventDispatcher.trigger(moleExecution, new MoleExecution.JobStatusChanged(job, capsule, newState, oldState))
            if (newState.isFinal) finalState(job, newState)
          }

          val moleJob: MoleJob = MoleJob(capsule.task, implicits + sourced + context, moleExecution.nextJobId, stateChanged)
          addJob(moleJob, capsule, ticket)
          eventDispatcher.trigger(moleExecution, new MoleExecution.JobCreated(moleJob, capsule))
          moleExecution.group(moleJob, capsule, this)
      }

    }
  }

  def newChild: SubMoleExecution = {
    val subMole = new SubMoleExecution(Some(this), moleExecution)
    if (canceled) subMole.cancel
    subMole
  }

  private def parentApply(f: SubMoleExecution ⇒ Unit) =
    parent match {
      case None    ⇒
      case Some(p) ⇒ f(p)
    }

  def finalState(job: MoleJob, state: State) = {
    job.exception match {
      case Some(e) ⇒
        val (capsule, _) = _jobs.single(job)
        val error = MoleExecution.JobFailed(job, capsule, e)
        moleExecution.cancel(error)
        logger.log(FINE, s"Error in user job execution for capsule $capsule, job state is FAILED.", e)
        eventDispatcher.trigger(moleExecution, error)
      case _ ⇒
    }

    if (state == COMPLETED) {
      val (capsule, _) = _jobs.single(job)
      eventDispatcher.trigger(moleExecution, MoleExecution.JobFinished(job, capsule))
    }

    state match {
      case COMPLETED         ⇒ jobFinished(job)
      case FAILED | CANCELED ⇒ jobFailedOrCanceled(job)
      case _                 ⇒
    }
  }

}
