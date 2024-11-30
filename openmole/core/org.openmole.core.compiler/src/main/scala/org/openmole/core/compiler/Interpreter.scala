/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.compiler

import org.openmole.core.compiler

import java.net.URLClassLoader
import java.util
import java.util.UUID
import javax.script.*
import org.openmole.core.exception.*
import org.openmole.core.pluginmanager.*
import org.osgi.framework.Bundle

import scala.annotation.tailrec
//import scala.reflect.internal.util.{ NoPosition, Position }
//import scala.tools.nsc._
//import scala.tools.nsc.reporters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
//import scala.tools.nsc.interpreter._
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.TmpDirectory

//import scala.tools.nsc.io.AbstractFile
import org.openmole.tool.osgi._
import org.openmole.tool.file._
import java.io.{ File ⇒ JFile }

import scala.reflect.ClassTag
//import scala.tools.nsc
//import scala.tools.nsc.interpreter.shell.{ ReplReporterImpl, ShellConfig }
import monocle.macros._
import org.openmole.tool.outputredirection.OutputRedirection
import dotty.tools.dotc.semanticdb.Diagnostic.Severity

object Interpreter {
  import java.io.File
  import java.net.URL

  def isInterpretedClass(c: Class[?]) =
    c.getClassLoader != null && classOf[dotty.tools.repl.AbstractFileClassLoader].isAssignableFrom(c.getClassLoader.getClass)

  def compilationMessage(errorMessages: List[ErrorMessage], code: String, lineOffset: Int = 0, fullCode: Option[String] = None) =
    def readableErrorMessages(error: ErrorMessage) =
      error.position.map(p ⇒ s"(line ${p.line - lineOffset}) ").getOrElse("") + error.decoratedMessage

    val (importsErrors, codeErrors) = errorMessages.partition(e ⇒ e.position.map(_.line < 0).getOrElse(false))

    def fullCodePart =
      fullCode.map: fc =>
        s"""\nComplete encapsulated code was:
          |$fc""".stripMargin

    (if codeErrors.nonEmpty then codeErrors.map(readableErrorMessages).mkString("\n") + "\n" else "") +
      (if importsErrors.filter(_.error).nonEmpty then "Error in imports header:\n" + importsErrors.filter(_.error).map(readableErrorMessages).mkString("\n") + "\n" else "") +
      s"""Compiling code:
        |${code}""".stripMargin +
      fullCodePart.getOrElse("")

  def errorMessagesToException(messages: List[ErrorMessage], code: String): Throwable = CompilationError(messages, code)

  case class CompilationError(errorMessages: List[ErrorMessage], code: String) extends Exception(compilationMessage(errorMessages, code))
  case class ErrorMessage(decoratedMessage: String, rawMessage: String, position: Option[ErrorPosition], error: Boolean)
  case class ErrorPosition(line: Int, start: Int, end: Int, point: Int)

  case class CompletionCandidate(label: String, symbols: List[dotty.tools.dotc.core.Symbols.Symbol], description: String)

  case class HeaderInfo(file: String)
  def firstLine(file: String) = HeaderInfo(file)

  def diagnosticToErrorMessage(diagnostic: dotty.tools.dotc.interfaces.Diagnostic): ErrorMessage = 
    import scala.jdk.OptionConverters._
    diagnostic.position.toScala match 
      case None => ErrorMessage(diagnostic.message, diagnostic.message, None, diagnostic.level() == dotty.tools.dotc.interfaces.Diagnostic.ERROR)
      case Some(pos) =>
        val firstLine = 0 //compiled.zipWithIndex.find { case (l, _) ⇒ l.contains(firstLineTag) }.map(_._2 + 1).getOrElse(0)

        def errorPos = ErrorPosition(pos.line - firstLine, pos.startColumn(), pos.endColumn(), pos.column())
        def decoratedMessage =
          val offsetOfError = pos.column() // - compiled.take(pos.line).map(_.length + 1).sum
          s"""${diagnostic.message}
                |${pos.lineContent()}
                |${(" " * offsetOfError)}^""".stripMargin

        ErrorMessage(decoratedMessage, diagnostic.message, Some(errorPos), diagnostic.level() == dotty.tools.dotc.interfaces.Diagnostic.ERROR)
        

     /* error.position match {
        case None                   ⇒ errorMessage ::= error
        case Some(p) if p.line >= 0 ⇒ errorMessage ::= error
        case _                      ⇒
      }*/


  def classLoader(priorityBundles: ⇒ Seq[Bundle], jars: Seq[JFile]) =
    new CompositeClassLoader(
      priorityBundles.map(_.classLoader) ++
        List(new URLClassLoader(jars.toArray.map(_.toURI.toURL))) ++
        List(classOf[Interpreter].getClassLoader) *
    )

  def classPath(priorityBundles: Seq[Bundle], jars: Seq[File]) = {
    def toPath(b: Bundle) = new URL(b.getLocation).getPath

    priorityBundles.map(toPath) ++
      jars.map(_.getCanonicalPath) ++
      PluginManager.bundlesForClass(Interpreter.getClass).map(toPath) ++
      Activator.context.get.getBundles.filter(!_.isSystem).map(toPath)
  }.distinct

  def driver(classDirectory: File, priorityBundles: Seq[Bundle], jars: Seq[File], quiet: Boolean): (repl.REPLDriver, ClassLoader) =
    def commonOptions = Seq("-language:postfixOps", "-language:implicitConversions", "-nowarn")

    classDirectory.mkdirs()

    val classLoaderValue = classLoader(priorityBundles, jars)

    val driver =
      Activator.context match
        case Some(_) =>
          new repl.REPLDriver(
            Array(
              "-classpath", classPath(priorityBundles, jars).mkString(":"),
              //"-usejavacp",
              //"-color:never",
              "-d", classDirectory.getAbsolutePath
            ) ++ (if quiet then Seq("-Xrepl-disable-display") else Seq()) ++ commonOptions,
            Console.out,
            Some(classLoaderValue)
          )
        case None =>
          new repl.REPLDriver(
            Array(
              "-classpath", System.getProperty("java.class.path"),
              //"-usejavacp",
              // "-color:never",
              "-d", classDirectory.getAbsolutePath,
            ) ++ (if quiet then Seq("-Xrepl-disable-display") else Seq()) ++ commonOptions,
            Console.out,
            Some(classLoaderValue)
            //Some (classOf[Test].getClassLoader)
          )

    (driver, classLoaderValue)

  type Compiled = () ⇒ Any
  case class RawCompiled(compiled: repl.REPLDriver.Compiled, classDirectory: java.io.File)

  def apply(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[JFile] = Seq.empty, quiet: Boolean = true)(implicit newFile: TmpDirectory, fileService: FileService) = {
    val classDirectory = fileService.wrapRemoveOnGC(TmpDirectory.newDirectory("classDirectory"))
    val (drv, cl) = driver(classDirectory, priorityBundles, jars, quiet = quiet)
    //    val settings = OSGiScalaCompiler.createSettings(new Settings, priorityBundles, jars, classDirectory)
    //    new Interpreter(priorityBundles, jars, quiet, classDirectory, settings)
    new Interpreter(drv, classDirectory, cl)
  }
}

class Interpreter(val driver: repl.REPLDriver, val classDirectory: java.io.File, val classLoaderValue: ClassLoader) {

  def initialState = driver.initialState

  def eval(code: String) = compile(code)()
  def compile(code: String): Interpreter.Compiled =
    val compiled = dottyCompile(code)
    () => run(compiled)._1

  def dottyCompile(code: String, state: repl.REPLDriver.CompilerState = initialState) =
     synchronized {
      import dotty.tools.dotc.reporting.Diagnostic._

      val compiled = driver.justCompile(code, state)

      compiled match 
        case c: repl.REPLDriver.Compiled =>
          c._1 match 
            case Left(diagnostics) => 
              throw Interpreter.errorMessagesToException(diagnostics.map(Interpreter.diagnosticToErrorMessage), code)
            case Right(_) => Interpreter.RawCompiled(c, classDirectory)
        case errors: dotty.tools.repl.SyntaxErrors =>
            throw Interpreter.errorMessagesToException(errors.errors.map(Interpreter.diagnosticToErrorMessage), code)
     }

  def run(c: Interpreter.RawCompiled) =
    val runResult = driver.justRun(c.compiled)

    def getResult(state: dotty.tools.repl.State): Any =
      //(1 to (state.valIndex + 4)).map(i => scala.util.Try(resultClass(state, Some(i)).getDeclaredMethods.toList)).map(println)
      if(state.valIndex > c.compiled._2.valIndex)
        val m = resultClass(state).getDeclaredMethods.head //(s"res${state.valIndex - 1}")
        m.invoke(null)
      else ()

    (getResult(runResult), runResult)

  def completion(code: String, position: Int, state: repl.REPLDriver.CompilerState) = synchronized {
    driver.completions(position, code, state).map(c => Interpreter.CompletionCandidate(c.label, c.symbols, c.description)).toVector
  }

  def resultClass(state: dotty.tools.repl.State, index: Option[Int] = None) =
    Class.forName(s"rs$$line$$${index.getOrElse(state.objectIndex)}", true, classLoader(state.context))

  def classLoader(context: dotty.tools.dotc.core.Contexts.Context) = driver.rederingValue.classLoader()(using context)

  def close = classDirectory.recursiveDelete

  //  def compile(code: String): ScalaREPL.Compiled = synchronized {
  //    omIMain.omReporter.errorMessage = Nil
  //    val scripted = new OMScripted(new interpreter.shell.Scripted.Factory, omIMain)
  //
  //    try {
  //      val compiled = scripted.compile("\n" + omIMain.firstLineTag + "\n" + code)
  //      () ⇒ compiled.eval()
  //    }
  //    catch {
  //      case e: Throwable ⇒ throw messageToException(e, omIMain.omReporter.errorMessage, code)
  //    }
  //  }

}



