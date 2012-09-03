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

package org.openmole.plugin.task.groovy

import java.io.File
import org.openmole.core.model.task._
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import org.openmole.plugin.task.code._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.plugin.tools.groovy.ContextToGroovyCode

object GroovyTask {

  def newRNG(seed: Long) = org.openmole.misc.tools.service.Random.newRNG(seed)
  def newFile() = org.openmole.misc.workspace.Workspace.newFile
  def newDir() = org.openmole.misc.workspace.Workspace.newDir

  /**
   * Instanciate a builder for the groovy task
   *
   * val hello = GroovyTask("hello", "println('Hello world! ' + i + ' ' + j)")
   *
   * @see CodeTaskBuilder for more info on addImport, addLib...
   *
   * @param name the task name
   * @param code the groovy source code
   */
  def apply(
    name: String,
    code: String)(implicit plugins: PluginSet) =
    new CodeTaskBuilder { builder ⇒

      addImport("static org.openmole.plugin.task.groovy.GroovyTask.newRNG")
      addImport("static org.openmole.plugin.task.groovy.GroovyTask.newFile")
      addImport("static org.openmole.plugin.task.groovy.GroovyTask.newDir")

      def toTask =
        new GroovyTask(name, code, builder.imports, builder.libraries) {
          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
          val inputFiles = builder.inputFiles
          val outputFiles = builder.outputFiles
          val resources = builder.resources
        }
    }

}

sealed abstract class GroovyTask(
    val name: String,
    code: String,
    imports: Iterable[String],
    libs: Iterable[File])(implicit val plugins: PluginSet) extends CodeTask {

  @transient lazy val contextToCode = new ContextToGroovyCode(codeWithImports, libs)

  def processCode(context: Context) = contextToCode.execute(context, outputs)

  private def codeWithImports = imports.map("import " + _).mkString("\n") + "\n" + code

}
