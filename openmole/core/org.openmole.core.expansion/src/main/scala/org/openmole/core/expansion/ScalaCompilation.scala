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
package org.openmole.core.expansion

import java.io.File

import org.openmole.core.console._
import org.openmole.core.context._
import org.openmole.core.exception._
import org.openmole.core.pluginmanager._
import org.openmole.core.tools.obj.ClassUtils._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.cache._
import org.openmole.tool.random._

import scala.util._

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

  def toScalaNativeType(t: ValType[_]): ValType[_] = {
    def native = {
      val (contentType, level) = ValType.unArrayify(t)
      for {
        m ← classEquivalence(contentType.runtimeClass).map(_.manifest)
      } yield (0 until level).foldLeft(ValType.unsecure(m)) {
        (c, _) ⇒ c.toArray.asInstanceOf[ValType[Any]]
      }
    }
    native getOrElse t
  }

}

object ScalaWrappedCompilation {
  def inputObject = "input"

  def static[R](
    code:      String,
    inputs:    Seq[Val[_]],
    wrapping:  OutputWrapping[R] = RawOutput(),
    libraries: Seq[File]         = Seq.empty,
    plugins:   Seq[File]         = Seq.empty
  )(implicit m: Manifest[_ <: R]) = {
    val (_inputs, _wrapping, _libraries, _plugins) = (inputs, wrapping, libraries, plugins)

    val compilation =
      new ScalaWrappedCompilation with StaticHeader {
        type RETURN = R
        def returnType: ValType[_ <: R] = ValType(m)
        override def inputs = _inputs
        override val wrapping = _wrapping
        override def source: String = code
        override def plugins: Seq[File] = _plugins
        override def libraries: Seq[File] = _libraries
      }

    compilation.functionCode().get
    compilation
  }

  def dynamic[R: Manifest](code: String, wrapping: OutputWrapping[R] = RawOutput[R]()) = {
    val _wrapping = wrapping

    new ScalaWrappedCompilation with DynamicHeader {
      type RETURN = R
      def returnType = ValType.apply[R]
      override val wrapping = _wrapping
      override def source: String = code
      override def plugins: Seq[File] = Seq.empty
      override def libraries: Seq[File] = Seq.empty
    }
  }

  type ContextClosure[+R] = (Context, RandomProvider, NewFile) ⇒ R

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

import org.openmole.core.expansion.ScalaWrappedCompilation._

trait ScalaWrappedCompilation <: ScalaCompilation { compilation ⇒

  type RETURN
  def returnType: ValType[_ <: RETURN]

  def wrapping: OutputWrapping[RETURN]
  def source: String

  def prefix = "_input_value_"

  def function(inputs: Seq[Val[_]]) =
    compile(script(inputs)).map { evaluated ⇒
      (evaluated, evaluated.getClass.getMethod("apply", classOf[Context], classOf[RandomProvider], classOf[NewFile]))
    }

  def closure(inputs: Seq[Val[_]]) =
    function(inputs).map {
      case (evaluated, method) ⇒
        val closure: ContextClosure[RETURN] = (context: Context, rng: RandomProvider, newFile: NewFile) ⇒ method.invoke(evaluated, context, rng, newFile).asInstanceOf[RETURN]
        closure
    }

  def script(inputs: Seq[Val[_]]) =
    s"""(${prefix}context: ${manifest[Context].toString}, ${prefix}RNG: ${manifest[RandomProvider].toString}, ${prefix}NewFile: ${manifest[NewFile].toString}) => {
          |  object $inputObject {
          |    ${inputs.toSeq.map(i ⇒ s"""var ${i.name} = ${prefix}context("${i.name}").asInstanceOf[${toScalaNativeType(i.`type`)}]""").mkString("; ")}
          |  }
          |  import ${inputObject}._
          |  implicit lazy val ${Variable.openMOLE("RNG").name}: util.Random = ${prefix}RNG()
          |  implicit lazy val ${Variable.openMOLE("NewFile").name} = ${prefix}NewFile
          |  $source
          |  ${wrapping.wrapOutput}
          |}: ${toScalaNativeType(returnType)}
          |""".stripMargin

  def apply(): FromContext[RETURN] = FromContext { p ⇒ compiled(p.context).get(p.context, p.random, p.newFile) }

  def compiled(context: Context): Try[ContextClosure[RETURN]]
}

trait DynamicHeader { this: ScalaWrappedCompilation ⇒

  val cache = Cache(collection.mutable.HashMap[Seq[Val[_]], Try[ContextClosure[RETURN]]]())

  def compiled(context: Context): Try[ContextClosure[RETURN]] = {
    val contextPrototypes = context.toSeq.map { case (_, v) ⇒ v.prototype }
    compiled(contextPrototypes)
  }

  def compiled(inputs: Seq[Val[_]]): Try[ContextClosure[RETURN]] =
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

  def validate(inputs: Seq[Val[_]]): Option[Throwable] = {
    compiled(inputs) match {
      case Success(_) ⇒ None
      case Failure(e) ⇒ Some(e)
    }
  }
}

trait StaticHeader { this: ScalaWrappedCompilation ⇒
  def inputs: Seq[Val[_]]
  val functionCode = Cache { closure(inputs) }
  def compiled(context: Context): Try[ContextClosure[RETURN]] = functionCode()
}