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

import java.io.{ OutputStreamWriter, File }
import javax.script.SimpleScriptContext
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.misc.tools.io.StringBuilderOutputStream
import org.openmole.misc.tools.script._
import org.openmole.plugin.task.code._
import org.openmole.misc.console.ScalaREPL

object ScalaTask {

  def apply(
    name: String,
    code: String)(implicit plugins: PluginSet = PluginSet.empty) = {
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

  def prefix = "_input_value_"

  @transient lazy val compiledScript = {
    val interpreter = new ScalaREPL(false)
    libraries.foreach { l ⇒ interpreter.addClasspath(l.getAbsolutePath) }
    val evaluated =
      try interpreter.eval(script)
      catch {
        case e: Exception ⇒
          throw new InternalProcessingError(
            e,
            interpreter.firstErrorMessage.map {
              error ⇒
                s"""Error while compiling:
               |${error.error}
               |on line ${error.line} of script:
               |${script}""".stripMargin
            }.getOrElse("Error in compiler")
          )
      }

    if (evaluated == null) throw new InternalProcessingError(
      s"""The return value of the script was null:
         |${script}""".stripMargin
    )
    (evaluated, evaluated.getClass.getMethod("apply", inputs.toSeq.map(_.prototype.`type`.runtimeClass).toSeq: _*))
  }

  def script =
    imports.map("import " + _).mkString("\n") + "\n\n" +
      s"""(${inputs.toSeq.map(i ⇒ prefix + i.prototype.name + ": " + i.prototype.`type`).mkString(",")}) => {
       |    ${inputs.toSeq.map(i ⇒ "var " + i.prototype.name + " = " + prefix + i.prototype.name).mkString("; ")}
       |    ${code}
       |    Map[String, Any]( ${outputs.toSeq.map(o ⇒ "\"" + o.prototype.name + "\" -> " + o.prototype.name).mkString(",")} )
       |}
       |""".stripMargin

  override def processCode(context: Context) = {
    val args = inputs.toArray.map(i ⇒ context(i.prototype))
    val (evaluated, method) = compiledScript
    val result = method.invoke(evaluated, args.toSeq.map(_.asInstanceOf[AnyRef]): _*).asInstanceOf[Map[String, Any]]
    context ++ outputs.toSeq.map { o ⇒ Variable.unsecure(o.prototype, result(o.prototype.name)) }
  }

}

