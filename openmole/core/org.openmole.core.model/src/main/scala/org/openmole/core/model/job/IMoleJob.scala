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

package org.openmole.core.model.job

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.task.ITask
import org.openmole.core.model.data.IContext
import org.openmole.misc.eventdispatcher.IObjectListener
import org.openmole.misc.eventdispatcher.Event

object IMoleJob {
  
  trait IStateChanged extends IObjectListener[IMoleJob] {
    override def eventOccured(obj: IMoleJob, args: Array[Any]) = 
      stateChanged(obj, args(0).asInstanceOf[State.State], args(1).asInstanceOf[State.State])
    
    def stateChanged(moleJob: IMoleJob, newState: State.State, oldStade: State.State)
  }
  
  trait IJobFailedOrCanceled extends IObjectListener[IMoleJob] {
    override def eventOccured(obj: IMoleJob, args: Array[Any]) =
      jobFailedOrCanceled(obj, args(0).asInstanceOf[ICapsule])
    
    def jobFailedOrCanceled(moleJob: IMoleJob, capsule: ICapsule)
  }
  
  trait ITransitionPerformed extends IObjectListener[IMoleJob] {
    override def eventOccured(obj: IMoleJob, args: Array[Any]) = 
      transitionPerformed(obj, args(0).asInstanceOf[ICapsule])
    
    def transitionPerformed(moleJob: IMoleJob, caspule: ICapsule)
  }
  
  final val TransitionPerformed = new Event[IMoleJob, ITransitionPerformed]
  final val JobFailedOrCanceled = new Event[IMoleJob, IJobFailedOrCanceled]
  final val StateChanged = new Event[IMoleJob, IStateChanged]
  
  implicit val moleJobOrdering = new Ordering[IMoleJob] {
    
    override def compare(left: IMoleJob, right: IMoleJob) = {
      MoleJobId.moleJobIdOrdering.compare(left.id, right.id)
    }
  }
}

trait IMoleJob {
  def task: ITask
  def state: State.State
  def isFinished: Boolean
  def context: IContext
  def exception: Option[Throwable]
  def timeStamps: Seq[ITimeStamp]
  def finished(context: IContext, timeStamps: Seq[ITimeStamp])
  def perform
  def id: MoleJobId
  def cancel 
}
