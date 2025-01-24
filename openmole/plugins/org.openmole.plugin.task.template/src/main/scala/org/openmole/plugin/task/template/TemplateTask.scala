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
import org.openmole.core.argument.{ ExpandedString, FromContext }
import org.openmole.core.setter._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object TemplateTask:

  def apply(
    template: String,
    output:   Val[File]
  )(using sourcecode.Name, DefinitionScope) =
    val expanded = ExpandedString(template)

    Task("TemplateTask"): p =>
      import p.*
      val outputFile = executionContext.moleExecutionDirectory.newFile("output", "template")
      outputFile.content = expanded.from(context)
      Context.empty + (output, outputFile)
    .set (outputs += output)
