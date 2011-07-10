/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.task.serialization

import java.io.File
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IContext
import org.openmole.core.serializer.SerializerService
import org.openmole.misc.workspace.Workspace

class SerializeXMLTask(name: String, var convert: List[(IPrototype[_], IPrototype[File])]) extends Task(name) {
  
  convert.foreach{case(input, output) => addInput(input); addOutput(output)}
  
  def serialize(input: IPrototype[_], output: IPrototype[File]) = {
    addInput(input)
    addOutput(output)
    convert :+= input -> output
  }
  
  override def process(context: IContext) =
    context ++ convert.map {
      case(input, output) =>
        val file = Workspace.newFile
        SerializerService.serialize(context.value(input).get, file)
        new Variable(output, file)
    } 

}
