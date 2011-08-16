/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.hook

import org.openmole.core.model.hook.IMoleExecutionHook
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.ICapsule
import org.openmole.misc.eventdispatcher.IObjectListener
import org.openmole.misc.eventdispatcher.IObjectListenerWithArgs
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Priority
import scala.ref.WeakReference

class MoleExecutionHook(private val moleExecution: WeakReference[IMoleExecution]) extends IMoleExecutionHook {
  
  def this(moleExecution: IMoleExecution) = this(new WeakReference(moleExecution))
  
  import Priority._
  import IMoleExecution._
  import EventDispatcher._
  
  resume
  
  override def resume = {
    registerForObjectChangedSynchronous(moleExecution(), HIGH, moleExecutionExecutionStartingAdapter, Starting)
    registerForObjectChangedSynchronous(moleExecution(), HIGH, moleExecutionJobStateChangedAdapter, OneJobStatusChanged)
    registerForObjectChangedSynchronous(moleExecution(), LOW, moleExecutionExecutionFinishedAdapter, Finished)
    registerForObjectChangedSynchronous(moleExecution(), NORMAL, capsuleFinished, JobInCapsuleFinished)
    registerForObjectChangedSynchronous(moleExecution(), NORMAL, capsuleStarting, JobInCapsuleStarting)
  }
  
  override def release = {
    unregisterSynchronousListener(moleExecution(), moleExecutionExecutionStartingAdapter, Starting)
    unregisterSynchronousListener(moleExecution(), moleExecutionJobStateChangedAdapter, OneJobStatusChanged)
    unregisterSynchronousListener(moleExecution(), moleExecutionExecutionFinishedAdapter, Finished)
    unregisterSynchronousListener(moleExecution(), capsuleFinished, JobInCapsuleFinished)
    unregisterSynchronousListener(moleExecution(), capsuleStarting, JobInCapsuleStarting)
  }
  
  def jobInCapsuleFinished(moleJob: IMoleJob, capsule: ICapsule) = {}
  def jobInCapsuleStarting(moleJob: IMoleJob, capsule: ICapsule) = {}
  
  def stateChanged(moleJob: IMoleJob) = {}
  def executionStarting = {}
  def executionFinished = {}
  
  @transient lazy val moleExecutionJobStateChangedAdapter = new IObjectListenerWithArgs[IMoleExecution]  {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = {
      val moleJob = args(0).asInstanceOf[IMoleJob]
      stateChanged(moleJob)
    }
  }
  
  @transient lazy val capsuleStarting = new CapsuleChangedAdapter(jobInCapsuleStarting)
  @transient lazy val capsuleFinished = new CapsuleChangedAdapter(jobInCapsuleFinished)
  @transient lazy val moleExecutionExecutionStartingAdapter = new IObjectListener[IMoleExecution]  {
    override def eventOccured(obj: IMoleExecution) = executionStarting
  }

  @transient lazy val moleExecutionExecutionFinishedAdapter = new IObjectListener[IMoleExecution]  {
    override def eventOccured(obj: IMoleExecution) =  executionFinished
  }
  
  class CapsuleChangedAdapter(listener: (IMoleJob, ICapsule) => Unit) extends IObjectListenerWithArgs[IMoleExecution]  {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = {
      val moleJob = args(0).asInstanceOf[IMoleJob]
      val capsule = args(1).asInstanceOf[ICapsule]
      listener(moleJob, capsule)
    }
  }
  
}
