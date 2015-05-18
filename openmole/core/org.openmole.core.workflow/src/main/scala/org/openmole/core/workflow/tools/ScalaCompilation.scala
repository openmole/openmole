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
package org.openmole.core.workflow.tools

import java.io.File
import java.lang.reflect.Method

import org.openmole.core.console._
import org.openmole.core.exception._
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.tools.obj.ClassUtils._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.Task
import org.openmole.core.workflow.validation.TypeUtil
import org.osgi.framework.Bundle

import scala.util.Try

trait ScalaCompilation {
  def usedBundles: Seq[File]
  def libraries: Seq[File]

  def compile(code: String) = Try {
    val interpreter = new ScalaREPL(usedBundles.flatMap(PluginManager.bundle) ++ Seq(PluginManager.bundleForClass(this.getClass)), libraries)

    val evaluated =
      try interpreter.eval(code)
      catch {
        case e: Throwable ⇒ throw new InternalProcessingError(e, s"Error compiling $code")
      }

    if (evaluated == null) throw new InternalProcessingError(
      s"""The return value of the script was null:
         |$code""".stripMargin
    )

    evaluated
  }

}

object ScalaWrappedCompilation {
  def inputObject = "input"

  def raw(code: String) =
    new ScalaWrappedCompilation with ScalaRawOutput {
      override def source: String = code
      override def imports: Seq[String] = Seq.empty
      override def usedBundles: Seq[File] = Seq.empty
      override def libraries: Seq[File] = Seq.empty
    }

}

import org.openmole.core.workflow.tools.ScalaWrappedCompilation._

trait ScalaWrappedOutput <: ScalaWrappedCompilation { compilation ⇒

  type CompiledScala = WrappedScala

  def compiledScala(closure: Context ⇒ Any): CompiledScala =
    WrappedScala(
      compiled = closure.asInstanceOf[Context ⇒ java.util.Map[String, Any]],
      outputs = compilation.outputs
    )

  case class WrappedScala(outputs: PrototypeSet, compiled: Context ⇒ java.util.Map[String, Any]) {

    def run(context: Context): Context = {
      val map = compiled(context)
      context ++
        outputs.toSeq.map {
          o ⇒ Variable.unsecure(o, Option(map.get(o.name)).getOrElse(new InternalProcessingError(s"Not found output $o")))
        }
    }

  }

  def outputs: PrototypeSet
  override def wrapOutput =
    Some(
      s"""
         |import scala.collection.JavaConversions.mapAsJavaMap
         |mapAsJavaMap(Map[String, Any]( ${outputs.toSeq.map(p ⇒ s""" "${p.name}" -> ${p.name}""").mkString(",")} ))
    """.stripMargin
    )

}

trait ScalaRawOutput <: ScalaCompilation { compilation ⇒

  type CompiledScala = RawScala

  def compiledScala(closure: Context ⇒ Any): CompiledScala = RawScala(closure)

  case class RawScala(compiled: Context ⇒ Any) {
    def run(context: Context): Any = compiled(context)
  }

}

trait ScalaWrappedCompilation <: ScalaCompilation { compilation ⇒

  type CompiledScala

  def source: String
  def openMOLEImports = Seq(s"${CodeTool.namespace}._")
  def imports: Seq[String]

  def prefix = "_input_value_"

  def function(inputs: Seq[Prototype[_]]) =
    compile(script(inputs)).map { evaluated ⇒
      (evaluated, evaluated.getClass.getMethod("apply", classOf[Context]))
    }

  def toScalaNativeType(t: PrototypeType[_]): PrototypeType[_] = {
    def native = {
      val (contentType, level) = TypeUtil.unArrayify(t)
      for {
        m ← classEquivalence(contentType.runtimeClass).map(_.manifest)
      } yield (0 until level).foldLeft(PrototypeType.unsecure(m)) {
        (c, _) ⇒ c.toArray.asInstanceOf[PrototypeType[Any]]
      }
    }
    native getOrElse t
  }

  def script(inputs: Seq[Prototype[_]]) =
    (openMOLEImports ++ imports).map("import " + _).mkString("\n") + "\n\n" +
      s"""(${prefix}context: ${classOf[Context].getCanonicalName}) => {
          |    object $inputObject {
          |      ${inputs.toSeq.map(i ⇒ s"""var ${i.name} = ${prefix}context("${i.name}").asInstanceOf[${toScalaNativeType(i.`type`)}]""").mkString("; ")}
          |    }
          |    import input._
          |    implicit lazy val ${Task.prefixedVariable("RNG")}: util.Random = newRNG(${Task.openMOLESeed.name}).toScala;
          |    $source
          |    ${wrapOutput.getOrElse("")}
          |}
          |""".stripMargin

  def wrapOutput: Option[String] = None

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
              val closure = (context: Context) ⇒ method.invoke(evaluated, context)
              compiledScala(closure)
          }
        case duplicated ⇒ throw new UserBadDataError("Duplicated inputs: " + duplicated.mkString(", "))
      }
    }

  def compiled(context: Context): Try[CompiledScala] = {
    val contextPrototypes = context.toSeq.map { case (_, v) ⇒ v.prototype }
    compiled(contextPrototypes)
  }

  def compiledScala(closure: Context ⇒ Any): CompiledScala

}