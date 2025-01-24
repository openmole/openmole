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

import monocle.Focus
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import org.openmole.core.argument.{ ExpandedString, FromContext }
import org.openmole.core.setter._
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.setter.InfoBuilder

object TemplateFileTask:

  def apply(
    template: File,
    output:   Val[File]
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextTask =
    TemplateFileTask(
      template -> output
    )

  def apply(
    template: Val[File],
    output: Val[File])(using sourcecode.Name, DefinitionScope): FromContextTask =
    TemplateFileTask(
      variable = Seq(template -> output)
    )

  def apply(
    file: (File, Val[File])*)(using sourcecode.Name, DefinitionScope): FromContextTask =
    TemplateFileTask(file)

  def apply(
   file: Seq[(File, Val[File])] = Seq(),
   variable: Seq[(Val[File], Val[File])] = Seq())(using sourcecode.Name, DefinitionScope): FromContextTask =
    val expanded = file.map: (template, v) =>
      template.withInputStream: is ⇒
        (template.getName, ExpandedString(is), v)

    Task("TemplateFileTask"): p =>
      import p.*

      val variablesFromFiles =
        for
          (name, template, output) <- expanded
        yield
          val file = executionContext.moleExecutionDirectory.newFile(name, ".tmp")
          file.content = template.from(context)
          Variable(output, file)

      val variablesFromVariables =
        for
          (v, output) <- variable
        yield
          val expanded = context(v).withInputStream { is ⇒ ExpandedString(is).from(context) }
          val file = executionContext.moleExecutionDirectory.newFile("template", ".tmp")
          file.content = expanded
          Variable(output, file)

      context ++ variablesFromFiles ++ variablesFromVariables
    .set(
      dsl.outputs ++= file.map(_._2),
      dsl.inputs ++= variable.map(_._1),
      dsl.outputs ++= variable.map(_._2)
    )
