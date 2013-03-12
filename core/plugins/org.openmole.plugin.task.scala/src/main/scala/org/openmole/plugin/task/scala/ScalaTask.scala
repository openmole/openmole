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

import java.io.File
import java.io.PrintWriter
import java.util.logging.Logger
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.misc.tools.script._
import org.openmole.plugin.task.code._
import reflect.ClassTag
import org.openmole.misc.tools.service.ObjectPool
import scala.tools.nsc.interpreter.NamedParam
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.exception.UserBadDataError

object ScalaTask {

  def apply(
    name: String,
    code: String)(implicit plugins: PluginSet = PluginSet.empty) = {
    val _plugins = plugins
    new CodeTaskBuilder { builder ⇒

      addImport("org.openmole.misc.tools.service.Random.newRNG")
      addImport("org.openmole.misc.workspace.Workspace.newFile")
      addImport("org.openmole.misc.workspace.Workspace.newDir")

      def toTask =
        new ScalaTask(name, code, builder.imports, builder.libraries) with builder.Built
    }
  }

}

sealed abstract class ScalaTask(
    val name: String,
    val code: String,
    imports: Iterable[String],
    libraries: Iterable[File]) extends CodeTask {

  def script = {
    "def run(" + inputs.map { i ⇒ i.prototype.name + ": " + i.prototype.`type`.toString }.mkString(",") + ") = {\n" +
      code + "\n" +
      "Map[String, Any](" + outputs.map(o ⇒ "\"" + o.prototype.name + "\" -> " + o.prototype.name).mkString(",") + ")\n" +
      "}\n" +
      "var " + resVariable + ": Map[String, Any] = null"
  }

  @transient lazy val resVariable = Workspace.preference(Task.OpenMOLEVariablePrefix) + "ScalaTaskResult"

  def interpreter = {
    val interpreter = new ScalaREPL
    interpreter.beSilentDuring {
      interpreter.addImports(imports.toSeq: _*)
      libraries.foreach { l ⇒ interpreter.addClasspath(l.getAbsolutePath) }
      val res = interpreter.interpret(script)
      if (res != tools.nsc.interpreter.Results.Success) throw new UserBadDataError("Error in script: " + script)
    }
    interpreter
  }

  @transient lazy val interpreterPool = new ObjectPool(interpreter)

  override def processCode(context: Context) = interpreterPool.exec {
    interpreter ⇒
      val scalaTaskResult = interpreter.beSilentDuring {
        try {
          context.values.foreach {
            v ⇒ interpreter.bindValue(v.prototype.name, v.value)
          }
          val code = resVariable + " = run(" + inputs.map { i ⇒ i.prototype.name }.mkString(",") + ")"
          interpreter.interpret(code)
          interpreter.valueOfTerm(resVariable).getOrElse(throw new UserBadDataError("Error in execution of " + code)).asInstanceOf[Map[String, Any]]
        } finally {
          interpreter.interpret(resVariable + " = null")
          context.values.foreach {
            v ⇒ interpreter.bindValue(v.prototype.name, null)
          }
        }
      }

      Context.empty ++ outputs.map { o ⇒ Variable.unsecure(o.prototype, scalaTaskResult(o.prototype.name)) }
  }
}

