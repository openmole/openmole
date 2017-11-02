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
import org.openmole.core.context.Val
import org.openmole.core.expansion.{ ExpandedString, FromContext }
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object TemplateFileFromInputTask {

  implicit def isTask: InputOutputBuilder[TemplateFileFromInputTask] = InputOutputBuilder(TemplateFileFromInputTask.config)

  def apply(
    template: Val[File],
    output:   Val[File]
  )(implicit name: sourcecode.Name) =
    new TemplateFileFromInputTask(template, output, InputOutputConfig()) set (
      dsl.inputs += template,
      dsl.outputs += output
    )

}

@Lenses case class TemplateFileFromInputTask(
  template: Val[File],
  output:   Val[File],
  config:   InputOutputConfig
) extends Task {

  override protected def process(executionContext: TaskExecutionContext) = FromContext { parameters ⇒
    import parameters._
    val expanded = context(template).withInputStream { is ⇒ ExpandedString(is).from(context) }
    val file = executionContext.tmpDirectory.newFile("template", ".tmp")
    file.content = expanded
    context + (output → file)
  }
}
