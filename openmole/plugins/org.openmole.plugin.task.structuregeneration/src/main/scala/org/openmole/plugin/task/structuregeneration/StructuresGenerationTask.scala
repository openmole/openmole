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

package org.openmole.plugin.task.structuregeneration

import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import scala.collection.mutable.ListBuffer

class StructuresGenerationTask(name: String) extends Task(name) {

  val toGenerate = new ListBuffer[(IPrototype[T],Class[T]) forSome{type T}] 
  
  def addStructure[T](proto: IPrototype[T], clazz: Class[T]) = {
    toGenerate += proto -> clazz
    addOutput(proto)
  }

  override def process(context: IContext) = context ++ toGenerate.map{
    case(proto, clazz) => new Variable(proto, clazz.newInstance)
  }
        
}
