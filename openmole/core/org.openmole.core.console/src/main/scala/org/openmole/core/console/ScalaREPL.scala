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

package org.openmole.core.console

import java.net.URLClassLoader
import java.util
import java.util.UUID

import javax.script._
import org.openmole.core.exception._
import org.openmole.core.pluginmanager._
import org.osgi.framework.Bundle

import scala.annotation.tailrec
import scala.reflect.internal.util.{ NoPosition, Position }
import scala.tools.nsc._
import scala.tools.nsc.reporters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.tools.nsc.interpreter._
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.TmpDirectory

import scala.tools.nsc.io.AbstractFile
import org.openmole.tool.osgi._
import java.io.{ File ⇒ JFile }

import scala.reflect.ClassTag
import scala.tools.nsc
import scala.tools.nsc.interpreter.shell.{ ReplReporterImpl, ShellConfig }
import monocle.macros._
import org.openmole.tool.outputredirection.OutputRedirection

object ScalaREPL {

  def apply(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[JFile] = Seq.empty, quiet: Boolean = true)(implicit newFile: TmpDirectory, fileService: FileService) = {
    val classDirectory = newFile.newDir("classDirectory")
    fileService.deleteWhenGarbageCollected(classDirectory)
    val settings = OSGiScalaCompiler.createSettings(new Settings, priorityBundles, jars, classDirectory)
    new ScalaREPL(priorityBundles, jars, quiet, classDirectory, settings)
  }

  def compilationMessage(errorMessages: List[ErrorMessage], code: String, lineOffset: Int = 0) = {
    def readableErrorMessages(error: ErrorMessage) =
      error.position.map(p ⇒ s"(line ${p.line - lineOffset}) ").getOrElse("") + error.decoratedMessage

    val (importsErrors, codeErrors) = errorMessages.partition(e ⇒ e.position.map(_.line < 0).getOrElse(false))

    (if (!codeErrors.isEmpty) codeErrors.map(readableErrorMessages).mkString("\n") + "\n" else "") +
      (if (!importsErrors.filter(_.error).isEmpty) "Error in imports header:\n" + importsErrors.filter(_.error).map(readableErrorMessages).mkString("\n") + "\n" else "") +
      s"""Compiling code:
        |${code}""".stripMargin
  }

  @Lenses case class CompilationError(cause: Throwable, errorMessages: List[ErrorMessage], code: String) extends Exception(compilationMessage(errorMessages, code), cause)

  @Lenses case class ErrorMessage(decoratedMessage: String, rawMessage: String, position: Option[ErrorPosition], error: Boolean)
  @Lenses case class ErrorPosition(line: Int, start: Int, end: Int, point: Int)

  case class HeaderInfo(file: String)
  def firstLine(file: String) = HeaderInfo(file)

  case class BundledClass(name: String, bundle: Option[Bundle])
  case class REPLClass(name: String, path: String, classLoader: REPLClassloader) {
    //    def byteCode = classLoader.findClassFile(name).getOrElse(throw new InternalProcessingError("Class not found in this classloader")).toByteArray
  }
  case class ReferencedClasses(repl: Vector[REPLClass], other: Vector[BundledClass]) {
    def plugins = other.flatMap(_.bundle).flatMap(PluginManager.allPluginDependencies).distinct.map(_.file)
  }

  object ReferencedClasses {
    def merge(rc1: ReferencedClasses, rc2: ReferencedClasses) =
      ReferencedClasses(rc1.repl ++ rc2.repl, rc1.other ++ rc2.other)
    def empty = ReferencedClasses(Vector.empty, Vector.empty)
  }

  class OMReporter(settings: Settings, quiet: Boolean) extends ReplReporterImpl(ShellConfig(settings), settings) {
    var storeErrors: Boolean = true
    var errorMessage: List[ErrorMessage] = Nil

    lazy val firstLineTag = "/*" + UUID.randomUUID().toString + "*/"

    override def doReport(pos: Position, msg: String, severity: Severity): Unit = {
      if (storeErrors) {
        val error =
          pos match {
            case NoPosition ⇒ ErrorMessage(msg, msg, None, severity == ERROR)
            case _ ⇒
              val compiled = new String(pos.source.content).split("\n")

              val firstLine = compiled.zipWithIndex.find { case (l, _) ⇒ l.contains(firstLineTag) }.map(_._2 + 2).getOrElse(0)
              val offset = compiled.take(firstLine).map(_.length + 1).sum

              def errorPos = ErrorPosition(pos.line - firstLine, pos.start - offset, pos.end - offset, pos.point - offset)
              def decoratedMessage = {
                val offsetOfError = pos.point - compiled.take(pos.line - 1).map(_.length + 1).sum
                s"""$msg
                     |${compiled(pos.line - 1)}
                     |${(" " * offsetOfError)}^""".stripMargin
              }

              ErrorMessage(decoratedMessage, msg, Some(errorPos), severity == ERROR)
          }

        error.position match {
          case None                   ⇒ errorMessage ::= error
          case Some(p) if p.line >= 0 ⇒ errorMessage ::= error
          case _                      ⇒
        }
      }

      if (severity == ERROR) super.doReport(pos, msg, severity)
    }

    override def printMessage(msg: String) = if (!quiet) super.printMessage(msg)

  }

  object OMIMain {
    def apply(settings: Settings, omReporter: OMReporter, priorityBundles: ⇒ Seq[Bundle], jars: Seq[JFile], classDirectory: JFile, firstPackageLineNumber: Int) = {
      val osgiSettings = OSGiScalaCompiler.createSettings(settings, priorityBundles, jars, classDirectory)

      val omIMain = new OMIMain(osgiSettings, classLoader(priorityBundles, jars), omReporter)
      /* To avoid name clash with remote environment scala code interpretation
       when repl classes are copied to a bundle and shipped away. If some
       package name $linex are exported from a bundle it prevents the compilation
       of the matching line in the interpreter.*/
      val clField = omIMain.naming.getClass.getDeclaredFields.find(_.getName.contains("_freshLineId")).get
      //  val freshLineId = {
      //    var x = firstPackageLineNumber
      //    () ⇒ { x += 1; x }
      //  }
      clField.setAccessible(true)
      clField.set(omIMain.naming, firstPackageLineNumber)

      omIMain
    }

    def classLoader(priorityBundles: ⇒ Seq[Bundle], jars: Seq[JFile]) = {
      new CompositeClassLoader(
        priorityBundles.map(_.classLoader) ++
          List(new URLClassLoader(jars.toArray.map(_.toURI.toURL))) ++
          List(classOf[OSGiScalaCompiler].getClassLoader): _*
      )
    }
  }

  class OMIMain(settings: Settings, parentClassLoader: ClassLoader, val omReporter: OMReporter) extends IMain(settings, Some(parentClassLoader), settings, omReporter) {
    def firstLineTag = omReporter.firstLineTag
  }

  type Compiled = () ⇒ Any

}

import ScalaREPL._

class REPLClassloader(val file: AbstractFile, classLoader: ClassLoader) extends scala.reflect.internal.util.AbstractFileClassLoader(file, null) { cl ⇒

  lazy val abstractFileLoader = new scala.reflect.internal.util.AbstractFileClassLoader(file, null)
  lazy val compositeClassLoader = new CompositeClassLoader(abstractFileLoader, classLoader)

  override def loadClass(s: String, b: Boolean): Class[_] = compositeClassLoader.loadClass(s, b)
  override def getResource(s: String) = compositeClassLoader.getResource(s)
  override def getResources(s: String) = compositeClassLoader.getResources(s)
  override def getResourceAsStream(s: String) = compositeClassLoader.getResourceAsStream(s)
  override def getPackage(name: String): Package = abstractFileLoader.getPackage(name)
  override def getPackages(): Array[Package] = abstractFileLoader.getPackages()

}

import shell._

class ScalaREPL(
  priorityBundles:        ⇒ Seq[Bundle],
  jars:                   Seq[JFile],
  quiet:                  Boolean,
  classDirectory:         java.io.File,
  settings:               Settings,
  firstPackageLineNumber: Int           = 100000000)
  extends ILoop(
    ShellConfig.apply(settings)
  ) { repl ⇒

  def storeErrors = omIMain.omReporter.storeErrors
  def storeErrors_=(b: Boolean) = omIMain.omReporter.storeErrors = b

  var loopExitCode = 0

  System.setProperty("jline.shutdownhook", "true")
  override lazy val prompt = "\nOpenMOLE> "

  //  val globalFutureField = classOf[ILoop].getDeclaredFields.find(_.getName.contains("interpreterInitialized")).get
  //  globalFutureField.setAccessible(true)
  //  globalFutureField.set(this, (new java.util.concurrent.CountDownLatch(0)).asInstanceOf[AnyRef])

  //settings = OSGiScalaCompiler.createSettings(new Settings, priorityBundles, jars, classDirectory)
  // in = chooseReader(settings)

  //settings.Yreplclassbased.value = true
  //settings.Yreplsync.value = true
  //settings.verbose.value = true
  //settings.debug.value = true

  private def messageToException(e: Throwable, messages: List[ErrorMessage], code: String): Throwable =
    CompilationError(e, messages.reverse, code)

  def eval(code: String) = compile(code).apply()

  def compile(code: String): ScalaREPL.Compiled = synchronized {
    omIMain.omReporter.errorMessage = Nil
    val scripted = new OMScripted(new interpreter.shell.Scripted.Factory, omIMain)

    try {
      val compiled = scripted.compile("\n" + omIMain.firstLineTag + "\n" + code)
      () ⇒ compiled.eval()
    }
    catch {
      case e: Throwable ⇒ throw messageToException(e, omIMain.omReporter.errorMessage, code)
    }
  }

  def loopWithExitCode = {
    run(settings)
    loopExitCode
  }

  def interpretAllFromWithExitCode(file: reflect.io.File) = {
    interpretAllFrom(file)
    loopExitCode
  }

  override def commands: List[LoopCommand] = super.commands.filter(_.name != "quit") ++ List(
    LoopCommand.cmd("quit", "[code]", "exit the application with a return code", exit)
  )

  def exit(s: String) = {
    if (!s.isEmpty) loopExitCode = s.toInt
    Result(keepRunning = false, None)
  }

  lazy val reporter = new OMReporter(settings, quiet)
  lazy val omIMain = OMIMain(settings, reporter, priorityBundles, jars, classDirectory, firstPackageLineNumber)

  intp = omIMain
  override def Repl(config: ShellConfig, interpreterSettings: Settings, out: java.io.PrintWriter) = omIMain

}

object Interpreter {

  def apply(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[JFile] = Seq.empty, quiet: Boolean = true)(implicit newFile: TmpDirectory, fileService: FileService) = {
    val classDirectory = newFile.newDir("classDirectory")
    fileService.deleteWhenGarbageCollected(classDirectory)
    val settings = OSGiScalaCompiler.createSettings(new Settings, priorityBundles, jars, classDirectory)
    new Interpreter(priorityBundles, jars, quiet, classDirectory, settings)
  }
}

class Interpreter(priorityBundles: ⇒ Seq[Bundle], jars: Seq[JFile], quiet: Boolean, classDirectory: java.io.File, settings: Settings) {
  def eval(code: String) = compile(code).apply()
  def compile(code: String): ScalaREPL.Compiled = synchronized {
    if (org.openmole.core.console.Activator.osgi) {
      val s = new ScalaREPL(priorityBundles, jars, quiet, classDirectory, settings, 1)
      s.compile(code)
    }
    else {
      import scala.reflect.runtime.universe
      import scala.tools.reflect.ToolBox
      val tb = universe.runtimeMirror(getClass.getClassLoader).mkToolBox()
      tb.compile(tb.parse(code))
    }
  }
}

