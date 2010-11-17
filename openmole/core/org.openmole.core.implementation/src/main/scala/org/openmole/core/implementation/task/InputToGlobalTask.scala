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

package org.openmole.core.implementation.task

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.implementation.data.{Data,DataSet}
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.IProgress

class InputToGlobalTask(name: String, inputData: Iterable[IData[_]]) extends Task(name) {

  addInput(new DataSet(inputData))
  
  def this(name: String, head: IData[_], inputData: IData[_]*) = {
    this(name, List(head) ++ inputData)
  }
  
  def this(name: String, head: IPrototype[_], prototypes: IPrototype[_]*) = {
    this(name, (List(head) ++ prototypes).map{new Data(_)})
  }

  override protected def process(global: IContext, context: IContext, progress: IProgress) = {
    for (data <- inputData) {
      val p = data.prototype
      context.variable(p) match {
        case None =>
        case Some(variable) => global += variable
      }
    }
  }
}
