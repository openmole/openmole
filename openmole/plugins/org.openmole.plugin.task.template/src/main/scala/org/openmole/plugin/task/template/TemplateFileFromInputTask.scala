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
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.VariableExpansion
import org.openmole.core.workspace.Workspace
import org.openmole.tool.file._

object TemplateFileFromInputTask {
  def apply(
    template: Prototype[File],
    output: Prototype[File]) = new TaskBuilder { builder ⇒

    addInput(template)
    addOutput(output)

    def toTask = new TemplateFileFromInputTask(template, output) with builder.Built
  }
}

sealed abstract class TemplateFileFromInputTask(
    val template: Prototype[File],
    val output: Prototype[File]) extends Task {

  override def process(context: Context)(implicit rng: RandomProvider) = {
    val expanded = context(template).withInputStream { is ⇒
      VariableExpansion(is).expand(context)
    }
    val file = Workspace.newFile("template", ".tmp")
    file.content = expanded
    context + (output -> file)
  }

}