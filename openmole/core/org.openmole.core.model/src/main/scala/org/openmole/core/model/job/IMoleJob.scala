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

package org.openmole.core.model.job


import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.task.IGenericTask
import org.openmole.core.model.data.IContext

object IMoleJob {
  final val TransitionPerformed = "TransitionPerformed"
  final val StateChanged = "StateChanged"
    
  implicit val ordering = new Ordering[IMoleJob] {
    
    override def compare(left: IMoleJob, right: IMoleJob) = {
      MoleJobId.ordering.compare(left.id, right.id)
    }
  }
}

trait IMoleJob {
  def task: IGenericTask
  def state: State.State
  def isFinished: Boolean
  def context: IContext
  def perform   
  def finished(executionJob: IContext)
  def rethrowException(context: IContext)
  def progress: IProgress
  def id: MoleJobId
  def cancel 
}
