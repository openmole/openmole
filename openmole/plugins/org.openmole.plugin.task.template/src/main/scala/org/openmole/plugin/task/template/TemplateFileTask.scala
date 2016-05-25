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

import monocle.macros.Lenses
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.VariableExpansion
import org.openmole.core.workspace.Workspace
import org.openmole.core.workflow.dsl
import dsl._

object TemplateFileTask {

  implicit def isBuilder = TaskBuilder[TemplateFileTask].from(this)

  def apply(
    template: File,
    output:   Prototype[File]
  ) = new TemplateFileTask(template, output) set (dsl.outputs += output)

}

@Lenses case class TemplateFileTask(
    template: File,
    output:   Prototype[File],
    inputs:   PrototypeSet    = PrototypeSet.empty,
    outputs:  PrototypeSet    = PrototypeSet.empty,
    defaults: DefaultSet      = DefaultSet.empty,
    name:     Option[String]  = None
) extends Task {

  @transient lazy val expanded = template.withInputStream { is ⇒
    VariableExpansion(is)
  }

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    val file = executionContext.tmpDirectory.newFile(template.getName, ".tmp")
    file.content = expanded.expand(context)
    context + (output → file)
  }
}