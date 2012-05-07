/*
 * Copyright (C) 2010 reuillon
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
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import org.openmole.plugin.task.code._
import org.openmole.core.implementation.task.PluginSet
import org.openmole.core.model.data.IContext
import org.openmole.plugin.tools.groovy.ContextToGroovyCode

object GroovyTask {

  def apply(
    name: String,
    code: String,
    libs: Iterable[File] = List.empty)(implicit plugins: IPluginSet) =
    new CodeTaskBuilder { builder ⇒

      addImport("static org.openmole.misc.tools.service.Random.newRNG")
      addImport("static org.openmole.misc.workspace.Workspace.newFile")
      addImport("static org.openmole.misc.workspace.Workspace.newDir")

      def toTask =
        new GroovyTask(name, code, builder.imports, libs) {
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
    libs: Iterable[File])(implicit val plugins: IPluginSet) extends CodeTask {

  @transient lazy val contextToCode = new ContextToGroovyCode(codeWithImports, libs)

  def processCode(context: IContext) = contextToCode.execute(context, outputs)

  private def codeWithImports = imports.map("import " + _).mkString("\n") + "\n" + code

}
