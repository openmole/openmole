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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.hook

import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IHook
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.misc.eventdispatcher.IObjectListener
import org.openmole.misc.eventdispatcher.IObjectListenerWithArgs
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Priority
import scala.ref.WeakReference

class MoleExecutionHook(private val moleExecution: WeakReference[IMoleExecution]) extends IHook {
  
  def this(moleExecution: IMoleExecution) = this(new WeakReference(moleExecution))
  
  import Priority._
  import IMoleExecution._
  import EventDispatcher._
  
  registerForObjectChangedSynchronous(moleExecution(), HIGH, new MoleExecutionExecutionStartingAdapter, Starting)
  registerForObjectChangedSynchronous(moleExecution(), HIGH, new MoleExecutionJobStateChangedAdapter, OneJobStatusChanged)
  registerForObjectChangedSynchronous(moleExecution(), LOW, new MoleExecutionExecutionFinishedAdapter, Finished)
  registerForObjectChangedSynchronous(moleExecution(), NORMAL, new CapsuleChangedAdapter(jobInCapsuleFinished), JobInCapsuleFinished)
  registerForObjectChangedSynchronous(moleExecution(), NORMAL, new CapsuleChangedAdapter(jobInCapsuleStarting), JobInCapsuleStarting)
  
  def jobInCapsuleFinished(moleJob: IMoleJob, capsule: IGenericCapsule) = {}
  def jobInCapsuleStarting(moleJob: IMoleJob, capsule: IGenericCapsule) = {}
  
  def stateChanged(moleJob: IMoleJob) = {}
  def executionStarting = {}
  def executionFinished = {}
  
  class MoleExecutionJobStateChangedAdapter extends IObjectListenerWithArgs[IMoleExecution]  {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = {
      val moleJob = args(0).asInstanceOf[IMoleJob]
      stateChanged(moleJob)
    }
  }
  
  class CapsuleChangedAdapter(listener: (IMoleJob, IGenericCapsule) => Unit) extends IObjectListenerWithArgs[IMoleExecution]  {
    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = {
      val moleJob = args(0).asInstanceOf[IMoleJob]
      val capsule = args(1).asInstanceOf[IGenericCapsule]
      listener(moleJob, capsule)
    }
  }
  
  class MoleExecutionExecutionStartingAdapter extends IObjectListener[IMoleExecution]  {
    override def eventOccured(obj: IMoleExecution) = executionStarting
  }

  class MoleExecutionExecutionFinishedAdapter extends IObjectListener[IMoleExecution]  {
    override def eventOccured(obj: IMoleExecution) =  executionFinished
  }
}
