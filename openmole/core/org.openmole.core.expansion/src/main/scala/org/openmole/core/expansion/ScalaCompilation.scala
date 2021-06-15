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

import org.openmole.core.compiler.ScalaREPL.CompilationError
import org.openmole.core.compiler._
import org.openmole.core.context._
import org.openmole.core.exception._
import org.openmole.core.fileservice.FileService
import org.openmole.core.pluginmanager._
import org.openmole.tool.types.ClassUtils._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.cache._
import org.openmole.tool.random._

import scala.util._

trait CompilationClosure[+T] extends ScalaCompilation.ContextClosure[T] {
  def apply(context: Context, rng: RandomProvider, newFile: TmpDirectory): T
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
   * Compile scala code using a [[org.openmole.core.compiler.Interpreter]]
   *
   * @param script
   * @param plugins
   * @param libraries
   * @param newFile
   * @param fileService
   * @tparam RETURN
   * @return
   */
  private def compile[RETURN](script: Script, plugins: Seq[File] = Seq.empty, libraries: Seq[File] = Seq.empty)(implicit newFile: TmpDirectory, fileService: FileService) = {
    val osgiMode = org.openmole.core.compiler.Activator.osgi
    val interpreter =
      if (osgiMode) Interpreter(priorityBundles(plugins), libraries)
      else Interpreter(jars = libraries)

    def errorMsg =
      if (osgiMode) s"""in osgi mode with priority bundles [${priorityBundles(plugins).map(b ⇒ s"${b.getSymbolicName}").mkString(", ")}], libraries [${libraries.mkString(", ")}], classpath [${OSGiScalaCompiler.classPath(priorityBundles(plugins), libraries).mkString(", ")}]."""
      else s"""in non osgi mode with libraries ${libraries.mkString(", ")}"""

    Try[RETURN] {
      val evaluated = interpreter.eval(addImports(script.code))

      if (evaluated == null) throw new InternalProcessingError(
        s"""The return value of the script was null:
           |${script.code}""".stripMargin
      )

      evaluated.asInstanceOf[RETURN]
    } match {
      case util.Success(s) ⇒ Success(s)
      case util.Failure(e: CompilationError) ⇒
        val errors = ScalaREPL.compilationMessage(e.errorMessages.filter(_.error), script.originalCode, lineOffset = script.headerLines + 2)
        val userBadDataError =
          new UserBadDataError(
            s"""${errors}
               |With interpreter $errorMsg"
               |""".stripMargin
          )
        util.Failure(userBadDataError)
      case util.Failure(e) ⇒ util.Failure(new InternalProcessingError(s"Error while compiling with interpreter $errorMsg", e))
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

  def toTypeString(t: ValType[_]): String = {
    toScalaNativeType(t).manifest.toString.
      replace(".package$", ".").
      replace("$", ".")
  }

  def function[RETURN](inputs: Seq[Val[_]], source: String, plugins: Seq[File], libraries: Seq[File], wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN])(implicit newFile: TmpDirectory, fileService: FileService) = {
    val s = script(inputs, source, wrapping, returnType)
    compile[CompilationClosure[RETURN]](s, plugins, libraries)
  }

  def closure[RETURN](inputs: Seq[Val[_]], source: String, plugins: Seq[File], libraries: Seq[File], wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN])(implicit newFile: TmpDirectory, fileService: FileService) =
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
  def script[RETURN](inputs: Seq[Val[_]], source: String, wrapping: OutputWrapping[RETURN], returnType: ValType[_ <: RETURN]) = {
    val header =
      s"""new ${classOf[CompilationClosure[_]].getName}[${toTypeString(returnType)}] {
         |  def apply(${prefix}context: ${manifest[Context].toString}, ${prefix}RNG: ${manifest[RandomProvider].toString}, ${prefix}NewFile: ${manifest[TmpDirectory].toString}) = {
         |    object $inputObject {
         |      ${inputs.toSeq.map(i ⇒ s"""var ${i.name} = ${prefix}context("${i.name}").asInstanceOf[${toTypeString(i.`type`)}]""").mkString("; ")}
         |    }
         |    import ${inputObject}._
         |    implicit def ${Val.name(Variable.openMOLENameSpace, "RNGProvider")} = ${prefix}RNG
         |    implicit def ${Val.name(Variable.openMOLENameSpace, "NewFile")} = ${prefix}NewFile
         |"""

    val code =
      s"""$header
       |    $source
       |    ${wrapping.wrapOutput}
       |  }: ${toTypeString(returnType)}
       |}""".stripMargin

    Script(code, source, header.split("\n").size + 1)
  }

  case class Script(code: String, originalCode: String, headerLines: Int)

  def static[R](
    code:      String,
    inputs:    Seq[Val[_]],
    wrapping:  OutputWrapping[R] = RawOutput(),
    libraries: Seq[File]         = Seq.empty,
    plugins:   Seq[File]         = Seq.empty
  )(implicit m: Manifest[_ <: R], newFile: TmpDirectory, fileService: FileService) =
    closure[R](inputs, code, plugins, libraries, wrapping, ValType(m)).get

  def dynamic[R: Manifest](code: String, wrapping: OutputWrapping[R] = RawOutput[R]()) = {

    class ScalaWrappedCompilation {
      def returnType = ValType.apply[R]

      val cache = Cache(collection.mutable.HashMap[Seq[Val[_]], Try[ContextClosure[R]]]())

      def compiled(context: Context)(implicit newFile: TmpDirectory, fileService: FileService): Try[ContextClosure[R]] = {
        val contextPrototypes = context.values.map { _.prototype }.toSeq
        compiled(contextPrototypes)
      }

      def compiled(inputs: Seq[Val[_]])(implicit newFile: TmpDirectory, fileService: FileService): Try[ContextClosure[R]] =
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

      def validate = Validate { p ⇒
        import p._

        compiled(inputs) match {
          case Success(_) ⇒ Seq()
          case Failure(e) ⇒ Seq(e)
        }
      }

      def apply()(implicit newFile: TmpDirectory, fileService: FileService): FromContext[R] = FromContext { p ⇒
        val closure = compiled(p.context).get
        try closure.apply(p.context, p.random, p.newFile)
        catch {
          case t: Throwable ⇒ throw new UserBadDataError(t, s"Error in execution of compiled closure in context: ${p.context}")
        }
      }
    }

    new ScalaWrappedCompilation()
  }

  type ContextClosure[+R] = (Context, RandomProvider, TmpDirectory) ⇒ R

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
         |scala.jdk.CollectionConverters.MapHasAsJava(Map[String, Any]( ${outputs.toSeq.map(p ⇒ s""" "${p.name}" -> ${p.name}""").mkString(",")} )).asJava
         |""".stripMargin

  }

  case class RawOutput[T]() extends OutputWrapping[T] { compilation ⇒
    def wrapOutput = ""
  }

}

