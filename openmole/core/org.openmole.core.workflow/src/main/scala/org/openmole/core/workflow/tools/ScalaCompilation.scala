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
import org.openmole.core.workspace.Workspace
import org.osgi.framework.Bundle
import org.openmole.core.workflow.dsl._
import org.openmole.tool.cache._

import scala.util.{ Random, Try }

trait ScalaCompilation {

  def plugins: Seq[File]
  def libraries: Seq[File]
  def openMOLEImports = Seq(s"${CodePackage.namespace}._")

  def addImports(code: String) =
    s"""
    |${openMOLEImports.map("import " + _).mkString("\n")}
    |
    |$code""".stripMargin

  def compile(code: String) = Try[Any] {
    val interpreter = new ScalaREPL(plugins.flatMap(PluginManager.bundle) ++ PluginManager.bundleForClass(this.getClass), libraries)

    val evaluated = interpreter.eval(addImports(code))

    if (evaluated == null) throw new InternalProcessingError(
      s"""The return value of the script was null:
         |$code""".stripMargin
    )

    evaluated
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

}

object ScalaWrappedCompilation {
  def inputObject = "input"

  def static[R](
    code:      String,
    inputs:    Seq[Prototype[_]],
    wrapping:  OutputWrapping[R] = RawOutput(),
    libraries: Seq[File]         = Seq.empty,
    plugins:   Seq[File]         = Seq.empty
  )(implicit m: Manifest[_ <: R]) = {
    val (_inputs, _wrapping, _libraries, _plugins) = (inputs, wrapping, libraries, plugins)

    val compilation =
      new ScalaWrappedCompilation with StaticHeader {
        type RETURN = R
        def returnType: PrototypeType[_ <: R] = PrototypeType(m)
        override def inputs = _inputs
        override val wrapping = _wrapping
        override def source: String = code
        override def plugins: Seq[File] = _plugins
        override def libraries: Seq[File] = _libraries
      }

    compilation.functionCode.get
    compilation
  }

  def dynamic[R: Manifest](code: String, wrapping: OutputWrapping[R] = RawOutput[R]()) = {
    val _wrapping = wrapping

    new ScalaWrappedCompilation with DynamicHeader {
      type RETURN = R
      def returnType = PrototypeType.apply[R]
      override val wrapping = _wrapping
      override def source: String = code
      override def plugins: Seq[File] = Seq.empty
      override def libraries: Seq[File] = Seq.empty
    }
  }

  type ContextClosure[+R] = (Context, RandomProvider) ⇒ R

  trait OutputWrapping[+R] {
    def wrapOutput: String
  }

  case class WrappedOutput(outputs: PrototypeSet) extends OutputWrapping[java.util.Map[String, Any]] {

    def wrapOutput =
      s"""
         |import scala.collection.JavaConversions.mapAsJavaMap
         |mapAsJavaMap(Map[String, Any]( ${outputs.toSeq.map(p ⇒ s""" "${p.name}" -> ${p.name}""").mkString(",")} ))
         |""".stripMargin

  }

  case class RawOutput[T]() extends OutputWrapping[T] { compilation ⇒
    def wrapOutput = ""
  }

}

import ScalaWrappedCompilation._

trait ScalaWrappedCompilation <: ScalaCompilation { compilation ⇒

  type RETURN
  def returnType: PrototypeType[_ <: RETURN]

  def wrapping: OutputWrapping[RETURN]
  def source: String

  def prefix = "_input_value_"

  def function(inputs: Seq[Prototype[_]]) =
    compile(script(inputs)).map { evaluated ⇒
      (evaluated, evaluated.getClass.getMethod("apply", classOf[Context], classOf[RandomProvider]))
    }

  def closure(inputs: Seq[Prototype[_]]) =
    function(inputs).map {
      case (evaluated, method) ⇒
        val closure: ContextClosure[RETURN] = (context: Context, rng: RandomProvider) ⇒ method.invoke(evaluated, context, rng).asInstanceOf[RETURN]
        closure
    }

  def script(inputs: Seq[Prototype[_]]) =
    s"""(${prefix}context: ${classOf[Context].getCanonicalName}, ${prefix}RNGProvider: ${classOf[RandomProvider].getCanonicalName}) => {
          |  object $inputObject {
          |    ${inputs.toSeq.map(i ⇒ s"""var ${i.name} = ${prefix}context("${i.name}").asInstanceOf[${toScalaNativeType(i.`type`)}]""").mkString("; ")}
          |  }
          |  import ${inputObject}._
          |  implicit lazy val ${Task.prefixedVariable("RNG")}: util.Random = ${prefix}RNGProvider()
          |  $source
          |  ${wrapping.wrapOutput}
          |}: ${toScalaNativeType(returnType)}
          |""".stripMargin

  def apply(): FromContext[RETURN] = FromContext { (context, rng) ⇒ compiled(context).get(context, rng) }

  def compiled(context: Context): Try[ContextClosure[RETURN]]
}

trait DynamicHeader { this: ScalaWrappedCompilation ⇒

  val cache = Cache(collection.mutable.HashMap[Seq[Prototype[_]], Try[ContextClosure[RETURN]]]())

  def compiled(context: Context): Try[ContextClosure[RETURN]] = {
    val contextPrototypes = context.toSeq.map { case (_, v) ⇒ v.prototype }
    compiled(contextPrototypes)
  }

  def compiled(inputs: Seq[Prototype[_]]): Try[ContextClosure[RETURN]] =
    cache().synchronized {
      val allInputMap = inputs.groupBy(_.name)

      val duplicatedInputs = allInputMap.filter { _._2.size > 1 }.map(_._2)

      duplicatedInputs match {
        case Nil ⇒
          def sortedInputNames = inputs.map(_.name).distinct.sorted
          val scriptInputs = sortedInputNames.map(n ⇒ allInputMap(n).head)
          cache().getOrElseUpdate(
            scriptInputs,
            closure(scriptInputs)
          )
        case duplicated ⇒ throw new UserBadDataError("Duplicated inputs: " + duplicated.mkString(", "))
      }
    }
}

trait StaticHeader { this: ScalaWrappedCompilation ⇒
  def inputs: Seq[Prototype[_]]
  @transient lazy val functionCode = closure(inputs)
  def compiled(context: Context): Try[ContextClosure[RETURN]] = functionCode
}