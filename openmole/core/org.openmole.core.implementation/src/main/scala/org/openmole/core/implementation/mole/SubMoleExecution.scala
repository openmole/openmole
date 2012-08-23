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

package org.openmole.core.implementation.mole

import org.openmole.core.implementation.data._
import org.openmole.core.model.transition._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.job._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.mole._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.job.IMoleJob._
import org.openmole.core.model.job.State._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.implementation.job.MoleJob._
import scala.collection.immutable.TreeMap
import collection.JavaConversions._
import scala.collection.mutable.Buffer

import scala.concurrent.stm._

class SubMoleExecution(
    val parent: Option[SubMoleExecution],
    val moleExecution: MoleExecution) extends ISubMoleExecution {

  private val _nbJobs = Ref(0)
  private val _childs = TSet.empty[SubMoleExecution]
  private val _jobs = Ref(TreeMap.empty[IMoleJob, (ICapsule, ITicket)])
  private val _canceled = Ref(false)

  val masterCapsuleRegistry = new RegistryWithTicket[IMasterCapsule, IContext]
  val aggregationTransitionRegistry = new RegistryWithTicket[IAggregationTransition, Buffer[IVariable[_]]]
  val transitionRegistry = new RegistryWithTicket[ITransition, Buffer[IVariable[_]]]

  parrentApply(_.+=(this))

  override def canceled: Boolean = atomic { implicit txn ⇒
    _canceled() || (parent match {
      case Some(p) ⇒ p.canceled
      case None ⇒ false
    })
  }

  private def addJob(moleJob: IMoleJob, capsule: ICapsule, ticket: ITicket) = atomic { implicit txn ⇒
    _jobs() = _jobs() + (moleJob -> (capsule, ticket))
    nbJobs_+=(1)
  }

  private def rmJob(moleJob: IMoleJob) = atomic { implicit txn ⇒
    _jobs() = _jobs() - moleJob
    nbJobs_+=(-1)
  }

  private def nbJobs_+=(v: Int): Unit = atomic { implicit txn ⇒
    _nbJobs += v
    parrentApply(_.nbJobs_+=(v))
  }

  def numberOfJobs = _nbJobs.single()

  override def root = !parent.isDefined

  override def cancel = {
    atomic { implicit txn ⇒
      _canceled() = true
      cancelJobs
      TSet.asSet(_childs)
    }.foreach { _.cancel }
    parrentApply(_.-=(this))
  }

  def cancelJobs = _jobs.single().keys.foreach { _.cancel }

  override def childs = _childs.single

  private def +=(submoleExecution: SubMoleExecution) =
    _childs.single += submoleExecution

  private def -=(submoleExecution: SubMoleExecution) =
    _childs.single -= submoleExecution

  override def jobs =
    atomic { implicit txn ⇒ _jobs().keys.toList ::: TSet.asSet(_childs).flatMap(_.jobs.toList).toList }

  private def jobFailedOrCanceled(job: IMoleJob) = {
    val (capsule, ticket) = _jobs.single().get(job).getOrElse(throw new InternalProcessingError("Bug, job has not been registred."))
    rmJob(job)
    checkFinished(ticket)
    moleExecution.jobFailedOrCanceled(job, capsule)
  }

  private def jobFinished(job: IMoleJob) = {
    val (capsule, ticket) = _jobs.single().get(job).getOrElse(throw new InternalProcessingError("Bug, job has not been registred."))
    try {
      EventDispatcher.trigger(moleExecution, new IMoleExecution.JobInCapsuleFinished(job, capsule))

      capsule.outputDataChannels.foreach { _.provides(job.context, ticket, moleExecution) }
      capsule.outputTransitions.foreach { _.perform(job.context, ticket, this) }
    } catch {
      case e ⇒ throw new InternalProcessingError(e, "Error at the end of a MoleJob for capsule " + capsule)
    } finally {
      rmJob(job)
      checkFinished(ticket)
      moleExecution.jobOutputTransitionsPerformed(job, capsule)
    }
  }

  private def checkFinished(ticket: ITicket) =
    if (_nbJobs.single() == 0) {
      EventDispatcher.trigger(this, new ISubMoleExecution.Finished(ticket))
      parrentApply(_.-=(this))
    }

  override def submit(capsule: ICapsule, context: IContext, ticket: ITicket) = {
    if (!canceled) {
      def implicits =
        Context.empty ++
          moleExecution.mole.implicits.values.filter(v ⇒ capsule.taskOrException.inputs.contains(v.prototype.name)) +
          new Variable(Task.openMOLESeed, moleExecution.newSeed)

      //FIXME: Factorize code
      capsule match {
        case c: IMasterCapsule ⇒
          synchronized {
            val savedContext = masterCapsuleRegistry.remove(c, ticket.parentOrException).getOrElse(new Context)
            val moleJob: IMoleJob = new MoleJob(capsule.taskOrException, implicits + context + savedContext, moleExecution.nextJobId, stateChanged)
            EventDispatcher.trigger(moleExecution, new IMoleExecution.JobInCapsuleStarting(moleJob, capsule))
            EventDispatcher.trigger(moleExecution, new IMoleExecution.OneJobSubmitted(moleJob))
            val instant = moleExecution.instantRerun(moleJob, capsule)
            addJob(moleJob, capsule, ticket)
            if (!instant) moleJob.perform
            masterCapsuleRegistry.register(c, ticket.parentOrException, c.toPersist(moleJob.context))
          }
        case _ ⇒
          val moleJob: IMoleJob = new MoleJob(capsule.taskOrException, implicits + context, moleExecution.nextJobId, stateChanged)
          addJob(moleJob, capsule, ticket)
          EventDispatcher.trigger(moleExecution, new IMoleExecution.JobInCapsuleStarting(moleJob, capsule))
          EventDispatcher.trigger(moleExecution, new IMoleExecution.OneJobSubmitted(moleJob))
          val instant = moleExecution.instantRerun(moleJob, capsule)
          if (!instant) moleExecution.group(moleJob, capsule, this)
      }
    }
  }

  def newChild: ISubMoleExecution = {
    val subMole = new SubMoleExecution(Some(this), moleExecution)
    if (canceled) subMole.cancel
    subMole
  }

  private def parrentApply(f: SubMoleExecution ⇒ Unit) =
    parent match {
      case None ⇒
      case Some(p) ⇒ f(p)
    }

  def stateChanged(job: IMoleJob, oldState: State, newState: State) = {
    newState match {
      case COMPLETED ⇒ jobFinished(job)
      case FAILED | CANCELED ⇒ jobFailedOrCanceled(job)
      case _ ⇒
    }
    EventDispatcher.trigger(moleExecution, new IMoleExecution.OneJobStatusChanged(job, newState, oldState))
    job.exception match {
      case Some(e) ⇒
        logger.log(SEVERE, "Error in user job execution, job state is FAILED.", e)
        EventDispatcher.trigger(moleExecution, new IMoleExecution.ExceptionRaised(job, e, SEVERE))
      case _ ⇒
    }
  }

}
