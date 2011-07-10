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

import org.openmole.core.model.task.IGenericTask
import org.openmole.core.model.data.IContext

object IMoleJob {
  
  final val TransitionPerformed = "TransitionPerformed"
  final val JobFailedOrCanceled = "JobFailedOrCanceled"
  final val StateChanged = "StateChanged"
    
  implicit val moleJobOrdering = new Ordering[IMoleJob] {
    
    override def compare(left: IMoleJob, right: IMoleJob) = {
      MoleJobId.moleJobIdOrdering.compare(left.id, right.id)
    }
  }
}

trait IMoleJob {
  def task: IGenericTask
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
