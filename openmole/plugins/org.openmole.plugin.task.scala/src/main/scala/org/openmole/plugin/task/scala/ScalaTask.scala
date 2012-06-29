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

package org.openmole.plugin.task.scala

import java.io.File
import java.io.PrintWriter
import java.util.logging.Logger
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.tools.io.FileUtil.fileOrdering
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.PluginSet
import org.openmole.core.model.data.IContext
import org.openmole.misc.tools.script.ScalaREPL
import org.openmole.plugin.task.code._

object ScalaTask {

  def apply(
    name: String,
    code: String)(implicit plugins: IPluginSet = PluginSet.empty) = {
    val _plugins = plugins
    new CodeTaskBuilder { builder ⇒

      addImport("org.openmole.misc.tools.service.Random.newRNG")
      addImport("org.openmole.misc.workspace.Workspace.newFile")
      addImport("org.openmole.misc.workspace.Workspace.newDir")

      def toTask =
        new ScalaTask(name, code, builder.imports, builder.libraries) {
          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
          val inputFiles = builder.inputFiles
          val outputFiles = builder.outputFiles
          val resources = builder.resources
        }
    }
  }

}

sealed abstract class ScalaTask(
    val name: String,
    val code: String,
    imports: Iterable[String],
    libraries: Iterable[File])(implicit val plugins: IPluginSet) extends CodeTask {

  override def processCode(context: IContext) = {
    val interpreter = new ScalaREPL
    context.values.foreach { v ⇒ interpreter.bind(v.prototype.name, v.value) }
    interpreter.addImports(imports.toSeq: _*)
    libraries.foreach { l ⇒ interpreter.addClasspath(l.getAbsolutePath) }
    interpreter.interpret(code)
    Context.empty ++ outputs.map { o ⇒ new Variable(o.prototype.asInstanceOf[IPrototype[Any]], interpreter.valueOfTerm(o.prototype.name)) }
  }
}

