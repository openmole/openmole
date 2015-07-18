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

import scala.util.{ Random, Try }

trait ScalaCompilation {

  def plugins: Seq[File]
  def libraries: Seq[File]

  def compile(code: String) = Try {
    val interpreter = new ScalaREPL(plugins.flatMap(PluginManager.bundle) ++ Seq(PluginManager.bundleForClass(this.getClass)), libraries)

    val evaluated = interpreter.eval(code)

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
      override def plugins: Seq[File] = Seq.empty
      override def libraries: Seq[File] = Seq.empty
    }

  type ScalaClosure = (Context, RandomProvider) ⇒ Any

  trait CompiledScala {
    type RETURN
    def run(context: Context)(implicit rng: RandomProvider): RETURN
  }

}

import ScalaWrappedCompilation._

trait ScalaWrappedOutput <: ScalaWrappedCompilation { compilation ⇒

  type CS = WrappedScala

  def compiledScala(closure: ScalaClosure) =
    WrappedScala(
      compiled = closure.asInstanceOf[(Context, RandomProvider) ⇒ java.util.Map[String, Any]],
      outputs = compilation.outputs
    )

  case class WrappedScala(outputs: PrototypeSet, compiled: (Context, RandomProvider) ⇒ java.util.Map[String, Any]) extends CompiledScala {
    type RETURN = Context
    def run(context: Context)(implicit rng: RandomProvider): Context = {
      val map = compiled(context, rng)
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

  type CS = RawScala

  def compiledScala(closure: ScalaClosure) = RawScala(closure)

  case class RawScala(compiled: ScalaClosure) extends CompiledScala {
    type RETURN = Any
    def run(context: Context)(implicit rng: RandomProvider): Any = compiled(context, rng)
  }

}

trait ScalaWrappedCompilation <: ScalaCompilation { compilation ⇒

  type CS <: CompiledScala

  def source: String
  def openMOLEImports = Seq(s"${CodeTool.namespace}._")
  def imports: Seq[String]

  def prefix = "_input_value_"

  def function(inputs: Seq[Prototype[_]]) =
    compile(script(inputs)).map { evaluated ⇒
      (evaluated, evaluated.getClass.getMethod("apply", classOf[Context], classOf[RandomProvider]))
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
      s"""(${prefix}context: ${classOf[Context].getCanonicalName}, ${prefix}RNGProvider: ${classOf[RandomProvider].getCanonicalName}) => {
          |    object $inputObject {
          |      ${inputs.toSeq.map(i ⇒ s"""var ${i.name} = ${prefix}context("${i.name}").asInstanceOf[${toScalaNativeType(i.`type`)}]""").mkString("; ")}
          |    }
          |    import ${inputObject}._
          |    implicit lazy val ${Task.prefixedVariable("RNG")}: util.Random = ${prefix}RNGProvider()
          |    $source
          |    ${wrapOutput.getOrElse("")}
          |}
          |""".stripMargin

  def wrapOutput: Option[String] = None

  @transient lazy val cache = collection.mutable.HashMap[Seq[Prototype[_]], Try[CS]]()

  def compiled(inputs: Seq[Prototype[_]]): Try[CS] =
    cache.synchronized {
      val allInputMap = inputs.groupBy(_.name)

      val duplicatedInputs = allInputMap.filter { _._2.size > 1 }.map(_._2)

      duplicatedInputs match {
        case Nil ⇒
          def sortedInputNames = inputs.map(_.name).distinct.sorted
          val scriptInputs = sortedInputNames.map(n ⇒ allInputMap(n).head)
          cache.getOrElseUpdate(
            scriptInputs,
            function(scriptInputs).map {
              case (evaluated, method) ⇒
                val closure: ScalaClosure =
                  (context: Context, rng: RandomProvider) ⇒ method.invoke(evaluated, context, rng)
                compiledScala(closure)
            }
          )
        case duplicated ⇒ throw new UserBadDataError("Duplicated inputs: " + duplicated.mkString(", "))
      }
    }

  def compiled(context: Context): Try[CS] = {
    val contextPrototypes = context.toSeq.map { case (_, v) ⇒ v.prototype }
    compiled(contextPrototypes)
  }

  def run(context: Context)(implicit rng: RandomProvider) = compiled(context).get.run(context)(rng)

  def compiledScala(closure: ScalaClosure): CS

}