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
import org.openmole.core.fileservice.FileService
import org.openmole.core.pluginmanager._
import org.openmole.core.tools.obj.ClassUtils._
import org.openmole.core.workspace.NewFile
import org.openmole.tool.cache._
import org.openmole.tool.random._

import scala.util._

object ScalaCompilation {

  //def plugins: Seq[File]
  //def libraries: Seq[File]
  def openMOLEImports = Seq(s"${CodePackage.namespace}._")

  def addImports(code: String) =
    s"""
    |${openMOLEImports.map("import " + _).mkString("\n")}
    |
    |$code""".stripMargin

  def priorityBundles(plugins: Seq[File]) = plugins.flatMap(PluginManager.bundle) ++ PluginManager.bundleForClass(this.getClass)

  def compile(code: String, plugins: Seq[File], libraries: Seq[File])(implicit newFile: NewFile, fileService: FileService) = {
    val osgiMode = org.openmole.core.console.Activator.osgi
    val interpreter =
      if (osgiMode) Interpreter(priorityBundles(plugins), libraries)
      else Interpreter(jars = libraries)

    Try[Any] {
      val evaluated = interpreter.eval(addImports(code))

      if (evaluated == null) throw new InternalProcessingError(
        s"""The return value of the script was null:
           |$code""".stripMargin
      )

      evaluated
    } match {
      case util.Success(s) ⇒ Success(s)
      case util.Failure(e) ⇒
        def msg = if (osgiMode) s"""in osgi mode with priority bundles ${priorityBundles(plugins).map(_.getSymbolicName).mkString(", ")} and libraries ${libraries.mkString(", ")}"""
        else s"""in non osgi mode with libraries ${libraries.mkString(", ")}"""
        util.Failure(new InternalProcessingError(s"Error while compiling with intepreter $msg", e))
    }
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

  //type RETURN
  //def returnType: ValType[_ <: RETURN]

  //def wrapping: OutputWrapping[RETURN]
  //def source: String

  def prefix = "_input_value_"

  def function[RETURN](inputs: Seq[Val[_]], source: String, plugins: Seq[File], libraries: Seq[File], wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN])(implicit newFile: NewFile, fileService: FileService) = {
    compile(script(inputs, source, wrapping, returnType), plugins, libraries).map { evaluated ⇒
      val m = evaluated.getClass.getMethods.find(_.getName.contains("apply")).get
      m.setAccessible(true)
      (evaluated, m) //evaluated.getClass.getMethod("apply", classOf[Context], classOf[RandomProvider], classOf[NewFile]))
    }
  }

  def closure[RETURN](inputs: Seq[Val[_]], source: String, plugins: Seq[File], libraries: Seq[File], wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN])(implicit newFile: NewFile, fileService: FileService) =
    function[RETURN](inputs, source, plugins, libraries, wrapping, returnType).map {
      case (evaluated, method) ⇒
        val closure: ContextClosure[RETURN] = (context: Context, rng: RandomProvider, newFile: NewFile) ⇒ method.invoke(evaluated, context, rng, newFile).asInstanceOf[RETURN]
        closure
    }

  def script[RETURN](inputs: Seq[Val[_]], source: String, wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN]) =
    s"""(${prefix}context: ${manifest[Context].toString}, ${prefix}RNG: ${manifest[RandomProvider].toString}, ${prefix}NewFile: ${manifest[NewFile].toString}) => {
       |  object $inputObject {
       |    ${inputs.toSeq.map(i ⇒ s"""var ${i.name} = ${prefix}context("${i.name}").asInstanceOf[${toScalaNativeType(i.`type`)}]""").mkString("; ")}
       |  }
       |  import ${inputObject}._
       |  implicit lazy val ${Variable.openMOLE("RNG").name}: util.Random = ${prefix}RNG()
       |  implicit lazy val ${Variable.openMOLE("NewFile").name} = ${prefix}NewFile
       |  $source
       |  ${wrapping.wrapOutput}
       |}: ${toScalaNativeType(returnType)}""".stripMargin

  //def apply[RETURN]()(implicit newFile: NewFile, fileService: FileService): FromContext[RETURN] = FromContext { p ⇒ compiled[RETURN](p.context).get(p.context, p.random, p.newFile) }

  //def compiled[RETURN](context: Context)(implicit newFile: NewFile, fileService: FileService): Try[ContextClosure[RETURN]]

  def inputObject = "input"

  def static[R](
    code:      String,
    inputs:    Seq[Val[_]],
    wrapping:  OutputWrapping[R] = RawOutput(),
    libraries: Seq[File]         = Seq.empty,
    plugins:   Seq[File]         = Seq.empty
  )(implicit m: Manifest[_ <: R], newFile: NewFile, fileService: FileService) =
    closure[R](inputs, code, plugins, libraries, wrapping, ValType(m)).get

  def dynamic[R: Manifest](code: String, wrapping: OutputWrapping[R] = RawOutput[R]()) = {
    //val _wrapping = wrapping

    class ScalaWrappedCompilation {
      def returnType = ValType.apply[R]

      val cache = Cache(collection.mutable.HashMap[Seq[Val[_]], Try[ContextClosure[R]]]())

      def compiled(context: Context)(implicit newFile: NewFile, fileService: FileService): Try[ContextClosure[R]] = {
        val contextPrototypes = context.toSeq.map { case (_, v) ⇒ v.prototype }
        compiled(contextPrototypes)
      }

      def compiled(inputs: Seq[Val[_]])(implicit newFile: NewFile, fileService: FileService): Try[ContextClosure[R]] =
        cache().synchronized {
          val allInputMap = inputs.groupBy(_.name)

          val duplicatedInputs = allInputMap.filter { _._2.size > 1 }.map(_._2)

          duplicatedInputs match {
            case Nil ⇒
              def sortedInputNames = inputs.map(_.name).distinct.sorted
              val scriptInputs = sortedInputNames.map(n ⇒ allInputMap(n).head)
              cache().getOrElseUpdate(
                scriptInputs,
                closure[R](scriptInputs, code, Seq.empty, Seq.empty, wrapping, returnType)
              )
            case duplicated ⇒ throw new UserBadDataError("Duplicated inputs: " + duplicated.mkString(", "))
          }
        }

      def validate(inputs: Seq[Val[_]])(implicit newFile: NewFile, fileService: FileService): Option[Throwable] = {
        compiled(inputs) match {
          case Success(_) ⇒ None
          case Failure(e) ⇒ Some(e)
        }
      }

      def apply()(implicit newFile: NewFile, fileService: FileService): FromContext[R] = FromContext { p ⇒ compiled(p.context).get(p.context, p.random, p.newFile) }

    }

    new ScalaWrappedCompilation()
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

