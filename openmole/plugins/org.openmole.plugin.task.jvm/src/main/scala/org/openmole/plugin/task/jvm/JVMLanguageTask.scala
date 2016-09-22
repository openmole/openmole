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

package org.openmole.plugin.task.jvm

import java.io.File

import org.openmole.core.context.{ Context, Prototype, Variable }
import org.openmole.core.serializer.plugin.Plugins
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task.{ Task, TaskExecutionContext }
import org.openmole.plugin.task.external.External
import org.openmole.tool.random.RandomProvider

object JVMLanguageTask {
  lazy val workDirectory = Prototype[File]("workDirectory")
}

trait JVMLanguageTask extends Task with Plugins {

  def libraries: Seq[File]
  def external: External

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    val pwd = executionContext.tmpDirectory.newDir("jvmtask")
    val preparedContext = external.prepareInputFiles(context, external.relativeResolver(pwd.getCanonicalFile)) + Variable(JVMLanguageTask.workDirectory, pwd)
    val resultContext = processCode(preparedContext)
    val resultContextWithFiles = external.fetchOutputFiles(resultContext, external.relativeResolver(pwd.getCanonicalFile))
    external.checkAndClean(this, resultContextWithFiles, pwd.getCanonicalFile)
    resultContextWithFiles
  }

  def processCode(context: Context)(implicit rng: RandomProvider): Context

}
