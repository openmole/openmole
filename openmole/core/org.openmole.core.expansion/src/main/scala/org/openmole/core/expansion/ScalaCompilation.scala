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

trait CompilationClosure[+T] extends ScalaCompilation.ContextClosure[T] {
  def apply(context: Context, rng: RandomProvider, newFile: NewFile): T
}

/**
 * Methods for compiling scala code
 */
object ScalaCompilation {

  /**
   * OpenMOLE namespace to import
   * @return
   */
  def openMOLEImports = Seq(s"${org.openmole.core.code.CodePackage.namespace}._")

  /**
   * Prepend OpenMOLE imports to a script
   * @param code
   * @return
   */
  def addImports(code: String) =
    s"""
    |${openMOLEImports.map("import " + _).mkString("\n")}
    |
    |$code""".stripMargin

  /**
   * Get osgi bundles given a sequence of plugins
   * @param plugins
   * @return
   */
  def priorityBundles(plugins: Seq[File]) = {
    val pluginBundles = plugins.flatMap(PluginManager.bundle)
    pluginBundles ++ pluginBundles.flatMap(PluginManager.allPluginDependencies) ++ PluginManager.bundleForClass(this.getClass)
  }

  /**
   * Compile scala code using a [[org.openmole.core.console.Interpreter]]
   *
   * @param code
   * @param plugins
   * @param libraries
   * @param newFile
   * @param fileService
   * @tparam RETURN
   * @return
   */
  def compile[RETURN](code: String, plugins: Seq[File] = Seq.empty, libraries: Seq[File] = Seq.empty)(implicit newFile: NewFile, fileService: FileService) = {
    val osgiMode = org.openmole.core.console.Activator.osgi
    val interpreter =
      if (osgiMode) Interpreter(priorityBundles(plugins), libraries)
      else Interpreter(jars = libraries)

    Try[RETURN] {
      val evaluated = interpreter.eval(addImports(code))

      if (evaluated == null) throw new InternalProcessingError(
        s"""The return value of the script was null:
           |$code""".stripMargin
      )

      evaluated.asInstanceOf[RETURN]
    } match {
      case util.Success(s) ⇒ Success(s)
      case util.Failure(e) ⇒
        def msg = if (osgiMode) s"""in osgi mode with priority bundles [${priorityBundles(plugins).map(b ⇒ s"${b.getSymbolicName}").mkString(", ")}], libraries [${libraries.mkString(", ")}], classpath [${OSGiScalaCompiler.classPath(priorityBundles(plugins), libraries).mkString(", ")}]."""
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

  def function[RETURN](inputs: Seq[Val[_]], source: String, plugins: Seq[File], libraries: Seq[File], wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN])(implicit newFile: NewFile, fileService: FileService) = {
    val s = script(inputs, source, wrapping, returnType)
    compile[CompilationClosure[RETURN]](s, plugins, libraries)
  }

  def closure[RETURN](inputs: Seq[Val[_]], source: String, plugins: Seq[File], libraries: Seq[File], wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN])(implicit newFile: NewFile, fileService: FileService) =
    function[RETURN](inputs, source, plugins, libraries, wrapping, returnType)

  /**
   * prefix used for input values in [[script]] construction
   * @return
   */
  def prefix = "_input_value_"

  /**
   * name of the input object in [[script]] construction
   * @return
   */
  def inputObject = "input"

  /**
   * Embed script elements in a compilable String.
   *  - an object of the runtime class of the CompilationClosure, parametrized with return type forced to scala native, is created by this code.
   * @param inputs input prototypes
   * @param source the source code in itself
   * @param wrapping how outputs are wrapped as code string
   * @param returnType the return type of the script
   * @tparam RETURN
   * @return
   */
  def script[RETURN](inputs: Seq[Val[_]], source: String, wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN]) =
    s"""new ${classOf[CompilationClosure[_]].getName}[${toScalaNativeType(returnType)}] {
       |  def apply(${prefix}context: ${manifest[Context].toString}, ${prefix}RNG: ${manifest[RandomProvider].toString}, ${prefix}NewFile: ${manifest[NewFile].toString}) = {
       |    object $inputObject {
       |      ${inputs.toSeq.map(i ⇒ s"""var ${i.name} = ${prefix}context("${i.name}").asInstanceOf[${toScalaNativeType(i.`type`)}]""").mkString("; ")}
       |    }
       |    import ${inputObject}._
       |    implicit def ${Val.name(Variable.openMOLENameSpace, "RNGProvider")} = ${prefix}RNG
       |    implicit def ${Val.name(Variable.openMOLENameSpace, "NewFile")} = ${prefix}NewFile
       |
       |    $source
       |    ${wrapping.wrapOutput}
       |  }: ${toScalaNativeType(returnType)}
       |}""".stripMargin

  def static[R](
    code:      String,
    inputs:    Seq[Val[_]],
    wrapping:  OutputWrapping[R] = RawOutput(),
    libraries: Seq[File]         = Seq.empty,
    plugins:   Seq[File]         = Seq.empty
  )(implicit m: Manifest[_ <: R], newFile: NewFile, fileService: FileService) =
    closure[R](inputs, code, plugins, libraries, wrapping, ValType(m)).get

  def dynamic[R: Manifest](code: String, wrapping: OutputWrapping[R] = RawOutput[R]()) = {

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

  /**
   * Wraps a prototype set as compilable code (used to build the [[script]])
   * @param outputs
   */
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

