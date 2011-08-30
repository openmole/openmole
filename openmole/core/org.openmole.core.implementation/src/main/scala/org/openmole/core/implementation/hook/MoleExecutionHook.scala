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
import org.openmole.core.model.job.State.State
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.ICapsule
import org.openmole.misc.eventdispatcher.IObjectListener
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
    registerForObjectChanged(moleExecution(), HIGH, moleExecutionExecutionStartingAdapter, Starting)
    registerForObjectChanged(moleExecution(), HIGH, moleExecutionJobStateChangedAdapter, OneJobStatusChanged)
    registerForObjectChanged(moleExecution(), LOW, moleExecutionExecutionFinishedAdapter, Finished)
    registerForObjectChanged(moleExecution(), NORMAL, capsuleFinished, JobInCapsuleFinished)
    registerForObjectChanged(moleExecution(), NORMAL, capsuleStarting, JobInCapsuleStarting)
  }
  
  override def release = {
    unregisterListener(moleExecution(), moleExecutionExecutionStartingAdapter, Starting)
    unregisterListener(moleExecution(), moleExecutionJobStateChangedAdapter, OneJobStatusChanged)
    unregisterListener(moleExecution(), moleExecutionExecutionFinishedAdapter, Finished)
    unregisterListener(moleExecution(), capsuleFinished, JobInCapsuleFinished)
    unregisterListener(moleExecution(), capsuleStarting, JobInCapsuleStarting)
  }
  
  override def jobFinished(moleJob: IMoleJob, capsule: ICapsule) = {}
  override def jobStarting(moleJob: IMoleJob, capsule: ICapsule) = {}
  
  override def stateChanged(moleJob: IMoleJob, newState: State, oldState: State) = {}
  override def executionStarting = {}
  override def executionFinished = {}
  
  @transient lazy val moleExecutionJobStateChangedAdapter = new IMoleExecution.IOneJobStatusChanged  {
    override def oneJobStatusChanged(execution: IMoleExecution, moleJob: IMoleJob, newState: State, oldState: State) = stateChanged(moleJob, newState, oldState)
  }
  
  @transient lazy val capsuleStarting = new IMoleExecution.IJobInCapsuleStarting {
    override def jobInCapsuleStarting(execution: IMoleExecution, moleJob: IMoleJob, capsule: ICapsule) = jobStarting(moleJob, capsule)
  } 
  
  @transient lazy val capsuleFinished = new IMoleExecution.IJobInCapsuleFinished {
    override def jobInCapsuleFinished(execution: IMoleExecution, moleJob: IMoleJob, capsule: ICapsule) = jobFinished(moleJob, capsule)
  } 
  
  @transient lazy val moleExecutionExecutionStartingAdapter = new IMoleExecution.IStarting {
    override def starting(moleJob: IMoleExecution) = executionStarting
  }

  @transient lazy val moleExecutionExecutionFinishedAdapter = new IMoleExecution.IFinished {
    override def finished(moleJob: IMoleExecution) = executionFinished
  }
  
}
