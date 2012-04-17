/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.plugin.task.template

import java.io.File
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet


object TemplateFileFromInputTask {
  def apply(
    name: String,
    template: IPrototype[File],
    output: IPrototype[File]
  )(implicit plugins: IPluginSet) = new TaskBuilder { builder =>
    
    def toTask = new TemplateFileFromInputTask(name, template, output) {
      val inputs = builder.inputs + template
      val outputs = builder.outputs + output
      val parameters = builder.parameters
    }
  }
}

sealed abstract class TemplateFileFromInputTask(
  val name: String,
  template: IPrototype[File],
  val output: IPrototype[File])
(implicit val plugins: IPluginSet) extends AbstractTemplateFileTask {

  override def file(context: IContext) = context.valueOrException(template)
 
}