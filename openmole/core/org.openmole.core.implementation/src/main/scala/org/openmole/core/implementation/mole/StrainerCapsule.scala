/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.implementation.mole

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.task.ITask
import org.openmole.core.implementation.data.DataSet._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.tools.TypeUtil._

object StrainerCapsule {
  class StrainerTaskDecorator(task: ITask) extends Task(task.name) {
    override def inputs = task.inputs
    override def outputs = task.outputs
    override def perform(context: IContext) = process(context)
    override def process(context: IContext) = context ++ task.perform(context)
    override def parameters = task.parameters
  }
}

class StrainerCapsule(task: Option[ITask] = None) extends Capsule(task.map(new StrainerCapsule.StrainerTaskDecorator(_))) {
  
  def this(t: ITask) = this(Some(t))

  override def userInputs = 
    (receivedTypes(defaultInputSlot) ++ 
      (task match {
        case Some(t) => t.inputs
        case None => Iterable.empty
      })).toDataSet
  
  override def userOutputs = 
    (receivedTypes(defaultInputSlot) ++ 
      (task match {
        case Some(t) => t.outputs
        case None => Iterable.empty
      })).toDataSet
  
}
