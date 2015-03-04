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

import java.util.concurrent.locks.Lock

import org.openmole.core.eventdispatcher.{ Event, EventDispatcher }
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.tools.service.{ Logger, ThreadUtil, LockUtil }
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.job.State._
import MoleJob._
import ThreadUtil.background
import scala.collection.mutable.Buffer

import scala.concurrent.stm._
import java.util.concurrent.{ Semaphore, locks, Executors }
import LockUtil._

object SubMoleExecution extends Logger {
  case class Finished(val ticket: Ticket, canceled: Boolean) extends Event[SubMoleExecution]
}

import SubMoleExecution.Log._

class SubMoleExecution(
    val parent: Option[SubMoleExecution],
    val moleExecution: MoleExecution) {

  @transient lazy val transitionLock = new locks.ReentrantLock()
  @transient lazy val masterCapsuleSemaphore = new Semaphore(1)

  private val _nbJobs = Ref(0)
  private val _children = TSet.empty[SubMoleExecution]
  private val _jobs = TMap[MoleJob, (Capsule, Ticket)]()
  private val _canceled = Ref(false)

  val masterCapsuleRegistry = new RegistryWithTicket[MasterCapsule, Context]
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, Buffer[Variable[_]]]
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
    atomic {
      implicit txn ⇒
        (_jobs.keys ++ TSet.asSet(_children).toSeq.flatMap(_.jobs)).toSeq
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
    try {
      val ctxForHooks = moleExecution.implicits + job.context

      def executeHook(h: Hook) =
        try h.perform(ctxForHooks, moleExecution.executionContext)
        catch {
          case e: Throwable ⇒
            EventDispatcher.trigger(moleExecution, new MoleExecution.HookExceptionRaised(h, job, e, SEVERE))
            logger.log(SEVERE, "Error in execution of misc " + h + "at the end of task " + job.task, e)
            throw e
        }

      val context = job.context ++ moleExecution.hooks(capsule).flatMap(executeHook).unzip._2
      mole.outputDataChannels(capsule).foreach { _.provides(context, ticket, moleExecution) }

      transitionLock {
        mole.outputTransitions(capsule).toList.sortBy(t ⇒ mole.slots(t.end.capsule).size).reverse.foreach { _.perform(context, ticket, this) }
      }
    }
    catch {
      case t: Throwable ⇒
        logger.log(SEVERE, "Error in submole execution", t)
        EventDispatcher.trigger(moleExecution, new MoleExecution.ExceptionRaised(job, t, SEVERE))
        throw t
    }
    finally {
      val finished =
        atomic { implicit txn ⇒
          rmJob(job)
          isFinished
        }

      if (finished) finish(ticket)
      moleExecution.jobOutputTransitionsPerformed(job, capsule)
    }
  }

  private def isFinished = _nbJobs.single() == 0

  private def finish(ticket: Ticket) = {
    EventDispatcher.trigger(this, new SubMoleExecution.Finished(ticket, canceled = _canceled.single()))
    parentApply(_.-=(this))
  }

  def submit(capsule: Capsule, context: Context, ticket: Ticket) = {
    if (!canceled) {
      nbJobs_+=(1)

      def addJob(moleJob: MoleJob, capsule: Capsule, ticket: Ticket) = atomic { implicit txn ⇒
        _jobs.put(moleJob, (capsule, ticket))
      }

      def implicits =
        moleExecution.implicits + Variable(Task.openMOLESeed, moleExecution.newSeed)

      val sourced =
        moleExecution.sources(capsule).foldLeft(Context.empty) {
          case (a, s) ⇒
            val ctx = try s.perform(implicits + context, moleExecution.executionContext)
            catch {
              case t: Throwable ⇒
                logger.log(SEVERE, "Error in submole execution", t)
                EventDispatcher.trigger(moleExecution, new MoleExecution.SourceExceptionRaised(s, capsule, t, SEVERE))
                throw new InternalProcessingError(t, s"Error in source execution that is plugged to $capsule")
            }
            a + ctx
        }

      //FIXME: Factorize code
      capsule match {
        case c: MasterCapsule ⇒
          def stateChanged(job: MoleJob, oldState: State, newState: State) =
            EventDispatcher.trigger(moleExecution, new MoleExecution.JobStatusChanged(job, c, newState, oldState))

          background {
            masterCapsuleSemaphore {
              val savedContext = masterCapsuleRegistry.remove(c, ticket.parentOrException).getOrElse(Context.empty)
              val moleJob: MoleJob = MoleJob(capsule.task, implicits + sourced + context + savedContext, moleExecution.nextJobId, stateChanged)
              EventDispatcher.trigger(moleExecution, new MoleExecution.JobCreated(moleJob, capsule))
              addJob(moleJob, capsule, ticket)
              moleJob.perform
              masterCapsuleRegistry.register(c, ticket.parentOrException, c.toPersist(moleJob.context))
              finalState(moleJob, moleJob.state)
            }
          }
        case _ ⇒
          def stateChanged(job: MoleJob, oldState: State, newState: State) = {
            EventDispatcher.trigger(moleExecution, new MoleExecution.JobStatusChanged(job, capsule, newState, oldState))
            if (newState.isFinal) finalState(job, newState)
          }

          val moleJob: MoleJob = MoleJob(capsule.task, implicits + sourced + context, moleExecution.nextJobId, stateChanged)
          addJob(moleJob, capsule, ticket)
          EventDispatcher.trigger(moleExecution, new MoleExecution.JobCreated(moleJob, capsule))
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
        moleExecution.cancel(e)
        val (capsule, _) = _jobs.single(job)
        logger.log(SEVERE, s"Error in user job execution for capsule $capsule, job state is FAILED.", e)
        EventDispatcher.trigger(moleExecution, MoleExecution.JobFailed(job, capsule, e))
      case _ ⇒
    }

    if (state == COMPLETED) {
      val (capsule, _) = _jobs.single(job)
      EventDispatcher.trigger(moleExecution, MoleExecution.JobFinished(job, capsule))
    }

    state match {
      case COMPLETED         ⇒ jobFinished(job)
      case FAILED | CANCELED ⇒ jobFailedOrCanceled(job)
      case _                 ⇒
    }
  }

}
