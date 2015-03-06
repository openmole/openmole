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

import org.openmole.core.console.ScalaREPL
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.tools.obj.ClassUtils
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.Task
import ClassUtils._

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

object ScalaWrappedCompilation {
  def inputObject = "input"
}

import ScalaWrappedCompilation._

trait ScalaWrappedCompilation <: ScalaCompilation { compilation ⇒
  def source: String
  def imports: Seq[String]
  def outputs: DataSet

  def prefix = "_input_value_"

  def function(inputs: Seq[Prototype[_]]) =
    compile(script(inputs)).map { evaluated ⇒
      (evaluated, evaluated.getClass.getMethod("apply", inputs.map(c ⇒ toScalaNativeType(c.`type`).runtimeClass).toSeq: _*))
    }

  def toScalaNativeType(m: Manifest[_]): Manifest[_] =
    classEquivalences.find(_.native == m.runtimeClass).map(_.scalaManifest) getOrElse (m)

  //FIXME deal with optional outputs
  def script(inputs: Seq[Prototype[_]]) =
    imports.map("import " + _).mkString("\n") + "\n\n" +
      s"""(${inputs.toSeq.map(i ⇒ prefix + i.name + ": " + toScalaNativeType(i.`type`)).mkString(",")}) => {
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
          val compiled = cache.getOrElseUpdate(scriptInputs, function(scriptInputs))
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
