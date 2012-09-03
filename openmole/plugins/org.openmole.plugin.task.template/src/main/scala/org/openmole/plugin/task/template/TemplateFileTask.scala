/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.task._

object TemplateFileTask {
  def apply(
    name: String,
    template: File,
    output: Prototype[File])(implicit plugins: PluginSet) = new TaskBuilder { builder ⇒

    addOutput(output)

    def toTask = new TemplateFileTask(name, template, output) {
      val inputs = builder.inputs
      val outputs = builder.outputs
      val parameters = builder.parameters
    }
  }
}

sealed abstract class TemplateFileTask(
    val name: String,
    template: File,
    val output: Prototype[File])(implicit val plugins: PluginSet) extends AbstractTemplateFileTask {

  override def file(context: Context) = template
}