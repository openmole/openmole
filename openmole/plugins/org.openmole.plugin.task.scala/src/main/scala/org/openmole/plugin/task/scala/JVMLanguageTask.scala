/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.task.scala

import org.openmole.core.context.{Context, PrototypeSet, Val, Variable}
import org.openmole.core.argument.FromContext
import org.openmole.core.serializer.plugin.Plugins
import org.openmole.core.workflow.dsl.*
import org.openmole.core.workflow.task.{Task, TaskExecutionContext}
import org.openmole.plugin.task.external.External

import java.io.File

object JVMLanguageTask:
  lazy val workDirectory = Val[File]("workDirectory")

  def process(executionContext: TaskExecutionContext, libraries: Seq[File], external: External, processCode: FromContext[Context], outputs: PrototypeSet) = FromContext: p =>
    import p.*

    val pwd = executionContext.taskExecutionDirectory.newDirectory("jvmpwd")
    val preparedContext = External.deployInputFilesAndResources(external, p.context, External.relativeResolver(pwd)) + Variable(JVMLanguageTask.workDirectory, pwd)
    val resultContext = processCode(preparedContext)
    val resultContextWithFiles = External.fetchOutputFiles(external, outputs, resultContext, External.relativeResolver(pwd), Seq(pwd))
    resultContextWithFiles


