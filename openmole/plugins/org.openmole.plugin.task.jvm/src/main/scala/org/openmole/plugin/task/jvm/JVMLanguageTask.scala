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

import org.openmole.core.serializer.plugin.Plugins
import org.openmole.core.workflow.data._
import org.openmole.core.workspace
import org.openmole.core.workspace.Workspace
import org.openmole.core.tools.service.Random
import org.openmole.plugin.task.external.ExternalTask
import java.io.File

object JVMLanguageTask {
  def newRNG(seed: Long) = Random.newRNG(seed)
  def newFile() = Workspace.newFile
  def newDir() = workspace.Workspace.newDir
  lazy val workDir = Prototype[File]("workDir")
}

trait JVMLanguageTask extends ExternalTask with Plugins {

  def imports: Seq[String]
  def libraries: Seq[File]

  override def process(context: Context) = {
    val pwd = Workspace.newDir
    val workDir = ""
    prepareInputFiles(context, pwd.getCanonicalFile, workDir)
    val newContext = context + Variable(JVMLanguageTask.workDir, pwd)
    fetchOutputFiles(processCode(newContext), pwd.getCanonicalFile, workDir)
  }

  def processCode(context: Context): Context

}
