/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.plugin.task.template

import java.io.File

import monocle.Focus
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion.{ ExpandedString, FromContext }
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object TemplateTask {

  implicit def isBuilder: InputOutputBuilder[TemplateTask] = InputOutputBuilder(Focus[TemplateTask](_.config))
  implicit def isInfo: InfoBuilder[TemplateTask] = InfoBuilder(Focus[TemplateTask](_.info))

  def apply(
    template: String,
    output:   Val[File]
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = new TemplateTask(template, output, InputOutputConfig(), InfoConfig()) set (outputs += output)

}

case class TemplateTask(
  template: String,
  output:   Val[File],
  config:   InputOutputConfig,
  info:     InfoConfig
) extends Task {

  val expanded = ExpandedString(template)

  override protected def process(executionContext: TaskExecutionContext) = FromContext { parameters ⇒
    import parameters._
    val outputFile = executionContext.moleExecutionDirectory.newFile("output", "template")
    outputFile.content = expanded.from(context)
    Context.empty + (output, outputFile)
  }
}
