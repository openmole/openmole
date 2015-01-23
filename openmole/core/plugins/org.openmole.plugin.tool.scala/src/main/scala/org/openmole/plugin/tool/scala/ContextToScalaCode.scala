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

package org.openmole.plugin.tool.scala

import java.io.File
import java.lang.reflect.Method
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.Task
import org.openmole.misc.console._
import org.openmole.misc.exception._

trait ContextToScalaCode {

  def usedClasses: Seq[Class[_]]
  def source: String
  def libraries: Seq[File]
  def imports: Seq[String]

  def prefix = "_input_value_"

  def compiledScript(inputs: Seq[Prototype[_]], outputs: DataSet) = {
    val interpreter = new ScalaREPL(false, usedClasses, libraries)
    val evaluated =
      try interpreter.eval(script(inputs, outputs))
      catch {
        case e: Exception ⇒
          throw new InternalProcessingError(
            e,
            interpreter.firstErrorMessage.map {
              error ⇒
                s"""Error while compiling:
               |${error.error}
               |on line ${error.line} of script:
               |${script(inputs, outputs)}""".stripMargin
            }.getOrElse("Error in compiler")
          )
      }

    if (evaluated == null) throw new InternalProcessingError(
      s"""The return value of the script was null:
         |${script(inputs, outputs)}""".stripMargin
    )
    (evaluated, evaluated.getClass.getMethod("apply", inputs.map(_.`type`.runtimeClass).toSeq: _*))
  }

  //FIXME deal with optional outputs
  def script(inputs: Seq[Prototype[_]], outputs: DataSet) =
    imports.map("import " + _).mkString("\n") + "\n\n" +
      s"""(${inputs.toSeq.map(i ⇒ prefix + i.name + ": " + i.`type`).mkString(",")}) => {
       |    object input {
       |      ${inputs.toSeq.map(i ⇒ "var " + i.name + " = " + prefix + i.name).mkString("; ")}
       |    }
       |    import input._
       |    implicit lazy val ${Task.prefixedVariable("RNG")}: util.Random = newRNG(oMSeed).toScala;
       |    ${source}
       |    import scala.collection.JavaConversions.mapAsJavaMap
       |    mapAsJavaMap(Map[String, Any]( ${outputs.toSeq.map(p ⇒ s""" "${p.prototype.name}" -> ${p.prototype.name}""").mkString(",")} ))
       |}
       |""".stripMargin

  @transient lazy val cache = collection.mutable.HashMap[(Seq[Prototype[_]], DataSet), (AnyRef, Method)]()

  def cachedCompiledScript(inputs: Seq[Prototype[_]], outputs: DataSet) = cache.synchronized {
    cache.getOrElseUpdate((inputs, outputs), compiledScript(inputs, outputs))
  }

  def execute(context: Context, outputs: DataSet): Context = {
    val contextPrototypes = context.toSeq.map { case (_, v) ⇒ v.prototype }.sortBy(_.name)
    val args = contextPrototypes.map(i ⇒ context(i))
    val (evaluated, method) = cachedCompiledScript(contextPrototypes, outputs)
    val result = method.invoke(evaluated, args.toSeq.map(_.asInstanceOf[AnyRef]): _*)
    val map = result.asInstanceOf[java.util.Map[String, Any]]
    context ++
      outputs.toSeq.map {
        o ⇒
          Variable.unsecure(o.prototype, Option(map.get(o.prototype.name)).getOrElse(new InternalProcessingError(s"Not found output $o")))
      }
  }

}
