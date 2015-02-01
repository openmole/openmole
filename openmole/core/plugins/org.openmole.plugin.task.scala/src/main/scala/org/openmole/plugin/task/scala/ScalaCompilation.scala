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

package org.openmole.plugin.task.scala

import java.io.File
import java.lang.reflect.Method
import java.util

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.Task
import org.openmole.misc.console._
import org.openmole.misc.exception._

import scala.util.Try

trait CompiledScala {

  type Compiled = Seq[Any] ⇒ java.util.Map[String, Any]

  def compiled: Compiled
  def prototypes: Seq[Prototype[_]]
  def outputs: DataSet

  def run(context: Context): Context = {
    val args = prototypes.map(i ⇒ context(i))
    val map = compiled(args)
    context ++
      outputs.toSeq.map {
        o ⇒
          Variable.unsecure(o.prototype, Option(map.get(o.prototype.name)).getOrElse(new InternalProcessingError(s"Not found output $o")))
      }
  }

}

object ScalaCompilation {
  def inputObject = "input"
}

import ScalaCompilation._

trait ScalaCompilation { compilation ⇒

  def usedClasses: Seq[Class[_]]
  def source: String
  def libraries: Seq[File]
  def imports: Seq[String]
  def outputs: DataSet

  def openMOLEImports =
    Seq(
      "org.openmole.misc.tools.service.Random.newRNG",
      "org.openmole.misc.workspace.Workspace.newFile",
      "org.openmole.misc.workspace.Workspace.newDir"
    )

  def prefix = "_input_value_"

  def compile(inputs: Seq[Prototype[_]]) = Try {
    val interpreter = new ScalaREPL(true, usedClasses, libraries)

    def exceptionMessage = {
      def error(error: interpreter.ErrorMessage) =
        s"""
           |${error.error}
           |on line  ${error.line}
           |""".stripMargin

      s"""
         |Errors while compiling:
         |${interpreter.errorMessages.map(error).mkString("\n")}
         |in script ${script(inputs)}
       """.stripMargin
    }

    val evaluated =
      try interpreter.eval(script(inputs))
      catch {
        case e: Exception ⇒
          throw new InternalProcessingError(e, exceptionMessage)
      }

    if (!interpreter.errorMessages.isEmpty) throw new InternalProcessingError(exceptionMessage)

    if (evaluated == null) throw new InternalProcessingError(
      s"""The return value of the script was null:
         |${script(inputs)}""".stripMargin
    )
    (evaluated, evaluated.getClass.getMethod("apply", inputs.map(_.`type`.runtimeClass).toSeq: _*))
  }

  //FIXME deal with optional outputs
  def script(inputs: Seq[Prototype[_]]) =
    (openMOLEImports ++ imports).map("import " + _).mkString("\n") + "\n\n" +
      s"""(${inputs.toSeq.map(i ⇒ prefix + i.name + ": " + i.`type`).mkString(",")}) => {
       |    object $inputObject {
       |      ${inputs.toSeq.map(i ⇒ "var " + i.name + " = " + prefix + i.name).mkString("; ")}
       |    }
       |    import input._
       |    implicit lazy val ${Task.prefixedVariable("RNG")}: util.Random = newRNG(${Task.openMOLESeed.name}).toScala;
       |    $source
       |    ${if (wrapOutput) outputMap else ""}
       |}
       |""".stripMargin

  def wrapOutput = true

  def outputMap =
    s"""
       |import scala.collection.JavaConversions.mapAsJavaMap
       |mapAsJavaMap(Map[String, Any]( ${outputs.toSeq.map(p ⇒ s""" "${p.prototype.name}" -> ${p.prototype.name}""").mkString(",")} ))
    """.stripMargin

  @transient lazy val cache = collection.mutable.HashMap[Seq[Prototype[_]], Try[(AnyRef, Method)]]()

  def compiled(inputs: Seq[Prototype[_]]): Try[CompiledScala] =
    cache.synchronized {

      val allInputs =
        if (inputs.exists(_ == Task.openMOLESeed)) inputs else Task.openMOLESeed :: inputs.toList

      val allInputMap = allInputs.groupBy(_.name)

      val duplicatedInputs = allInputMap.filter { _._2.size > 1 }.map(_._2)

      duplicatedInputs match {
        case Nil ⇒
          def sortedInputNames = allInputs.map(_.name).distinct.sorted
          lazy val scriptInputs = sortedInputNames.map(n ⇒ allInputMap(n).head)
          val compiled = cache.getOrElseUpdate(scriptInputs, compile(scriptInputs))
          compiled.map {
            case (evaluated, method) ⇒
              val closure = (args: Seq[Any]) ⇒ method.invoke(evaluated, args.toSeq.map(_.asInstanceOf[AnyRef]): _*).asInstanceOf[java.util.Map[String, Any]]
              new CompiledScala {
                override def compiled = closure
                override def outputs: DataSet = compilation.outputs
                override def prototypes: Seq[Prototype[_]] = scriptInputs
              }
          }
        case duplicated ⇒ throw new UserBadDataError("Duplicated inputs: " + duplicated.mkString(", "))
      }
    }

  def compiled(context: Context): Try[CompiledScala] = {
    val contextPrototypes = context.toSeq.map { case (_, v) ⇒ v.prototype }
    compiled(contextPrototypes)
  }

}
