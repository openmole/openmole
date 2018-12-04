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
import monocle.macros._
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.NewFile

import scala.tools.nsc.io.AbstractFile
import org.openmole.tool.osgi._

import scala.reflect.ClassTag
import scala.tools.nsc

object ScalaREPL {

  def apply(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[JFile] = Seq.empty, quiet: Boolean = true)(implicit newFile: NewFile, fileService: FileService) = {
    val classDirectory = newFile.newDir("classDirectory")
    fileService.deleteWhenGarbageCollected(classDirectory)
    new ScalaREPL(priorityBundles, jars, quiet, classDirectory)
  }

  private def compilationMessage(errorMessages: List[ErrorMessage], code: String) = {
    def readableErrorMessages(error: ErrorMessage) =
      error.position.map(p ⇒ s"(line ${p.line}) ").getOrElse("") + error.decoratedMessage

    val (importsErrors, codeErrors) = errorMessages.partition(e ⇒ e.position.map(_.line < 0).getOrElse(false))

    (if (!codeErrors.isEmpty) codeErrors.map(readableErrorMessages).mkString("\n") + "\n" else "") +
      (if (!importsErrors.isEmpty) "Error in imports header:\n" + importsErrors.map(readableErrorMessages).mkString("\n") + "\n" else "") +
      s"""Compiling code:
        |${code}""".stripMargin
  }

  @Lenses case class CompilationError(cause: Throwable, errorMessages: List[ErrorMessage], code: String) extends Exception(compilationMessage(errorMessages, code), cause)

  @Lenses case class ErrorMessage(decoratedMessage: String, rawMessage: String, position: Option[ErrorPosition])
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

  //    def bundleFromReferencedClass(ref: ReferencedClasses, bundleName: String, bundleVersion: String, bundle: java.io.File) = {
  //      val classByteCode = ref.repl.distinct.map(c ⇒ ClassByteCode(c.path, c.byteCode))
  //
  //      def packageName(c: String) = c.reverse.dropWhile(_ != '.').drop(1).reverse
  //      def importPackages = ref.other.filter(_.bundle.isDefined).groupBy(_.bundle).toSeq.flatMap {
  //        case (b, cs) ⇒
  //          for {
  //            c ← cs
  //            p = packageName(c.name)
  //            if !p.isEmpty
  //          } yield VersionedPackage(p, b.map(_.getVersion.toString))
  //      }.distinct
  //
  //      createBundle(
  //        name = bundleName,
  //        version = bundleVersion,
  //        classes = classByteCode,
  //        exportedPackages = ref.repl.map(c ⇒ packageName(c.name)).distinct.filter(p ⇒ !p.isEmpty),
  //        importedPackages = importPackages,
  //        bundle = bundle
  //      )
  //    }

  class OMIMain(settings: Settings, priorityBundles: ⇒ Seq[Bundle], jars: Seq[JFile], quiet: Boolean) extends IMain(settings) {
    var storeErrors: Boolean = true
    var errorMessage: List[ErrorMessage] = Nil
    lazy val firstLineTag = "/*" + UUID.randomUUID().toString + "*/"

    override lazy val reporter = new ReplReporter(this) {

      override def error(pos: Position, msg: String): Unit = {
        if (storeErrors) {
          val error =
            pos match {
              case NoPosition ⇒ ErrorMessage(msg, msg, None)
              case _ ⇒
                val compiled = new String(pos.source.content).split("\n")
                val firstLine = compiled.zipWithIndex.find { case (l, _) ⇒ l.contains(firstLineTag) }.map(_._2 + 1).getOrElse(0)
                val offset = compiled.take(firstLine).map(_.length + 1).sum
                def errorPos = ErrorPosition(pos.line - firstLine, pos.start - offset, pos.end - offset, pos.point - offset)
                def decoratedMessage = {
                  val offsetOfError = pos.point - compiled.take(pos.line - 1).map(_.length + 1).sum
                  s"""$msg
                     |${compiled(pos.line - 1)}
                     |${(" " * offsetOfError)}^""".stripMargin
                }

                ErrorMessage(decoratedMessage, msg, Some(errorPos))
            }

          errorMessage ::= error
        }
        super.error(pos, msg)
      }

      //override def printMessage(msg: String) = println(msg)
      override def printMessage(msg: String) = if (!quiet) super.printMessage(msg)

    }

    override protected def newCompiler(settings: Settings, reporter: Reporter) = {
      //settings.Yreplclassbased.value = true
      //settings.exposeEmptyPackage.value = true
      //settings.outputDirs setSingleOutput replOutput.dir
      //println(settings.outputDirs.getSingleOutput)
      if (Activator.osgi) OSGiScalaCompiler(settings, reporter)
      else {
        //settings.usejavacp.value = true
        super.newCompiler(settings, reporter)
      }
    }

    override lazy val classLoader =
      if (Activator.osgi) {
        new REPLClassloader(
          replOutput.dir,
          new CompositeClassLoader(
            priorityBundles.map(_.classLoader) ++
              List(new URLClassLoader(jars.toArray.map(_.toURI.toURL))) ++
              List(classOf[OSGiScalaCompiler].getClassLoader, super.classLoader): _*
          )
        )
      }
      else super.classLoader

  }

  type Compiled = () ⇒ Any

  def call[U: ClassTag, T](o: AnyRef, name: String, args: Vector[Any]) = {
    val handle = implicitly[ClassTag[U]].runtimeClass.getDeclaredMethods.find(_.getName == name).get
    handle.setAccessible(true)
    handle.invoke(o, args.toArray.map(_.asInstanceOf[Object]): _*).asInstanceOf[T]
  }
}

import ScalaREPL._

class REPLClassloader(val file: AbstractFile, classLoader: ClassLoader) extends scala.reflect.internal.util.AbstractFileClassLoader(file, classLoader) { cl ⇒

  //  lazy val abstractFileLoader = new scala.reflect.internal.util.AbstractFileClassLoader(file, null)
  //  lazy val compositeClassLoader = new CompositeClassLoader(abstractFileLoader, classLoader)
  //
  //  override def loadClass(s: String, b: Boolean): Class[_] = {
  //    import org.openmole.tool.file._
  //    println(s"start load $s")
  //    val c =
  //      try compositeClassLoader.loadClass(s, b)
  //      catch {
  //        case e ⇒
  //          println(e)
  //          throw e
  //      }
  //
  //    c
  //  }
  //
  //  override def getResource(s: String) = compositeClassLoader.getResource(s)
  //  override def getResources(s: String) = compositeClassLoader.getResources(s)
  //  override def getResourceAsStream(s: String) = compositeClassLoader.getResourceAsStream(s)
  //  override def getPackage(name: String): Package = abstractFileLoader.getPackage(name)
  //  override def getPackages(): Array[Package] = abstractFileLoader.getPackages()

}

class ScalaREPL(priorityBundles: ⇒ Seq[Bundle], jars: Seq[JFile], quiet: Boolean, classDirectory: java.io.File, firstPackageLineNumber: Int = 100000000) extends ILoop { repl ⇒

  def storeErrors = omIMain.storeErrors
  def storeErrors_=(b: Boolean) = omIMain.storeErrors = b

  var loopExitCode = 0

  System.setProperty("jline.shutdownhook", "true")
  override val prompt = "\nOpenMOLE> "

  val globalFutureField = classOf[ILoop].getDeclaredFields.find(_.getName.contains("globalFuture")).get
  globalFutureField.setAccessible(true)
  globalFutureField.set(this, Future { true }.asInstanceOf[AnyRef])

  settings = OSGiScalaCompiler.createSettings(new Settings, priorityBundles, jars, classDirectory)

  //settings.Yreplclassbased.value = true
  //settings.Yreplsync.value = true
  //settings.verbose.value = true
  //settings.debug.value = true

  in = chooseReader(settings)

  private def messageToException(e: Throwable, messages: List[ErrorMessage], code: String): Throwable =
    CompilationError(e, messages.reverse, code)

  def eval(code: String) = compile(code).apply()

  //  def compile(script: String): ScalaREPL.Compiled = {
  //    omIMain.errorMessage = Nil
  //
  //    ScalaREPL.call[IMain, Either[IR.Result, omIMain.Request]](intp, "compile", Vector("\n" + omIMain.firstLineTag + "\n" + script, false)) match {
  //      case Right(req) ⇒ () ⇒ req.lineRep.evalEither.toTry.get
  //      case Left(IR.Incomplete) ⇒ throw new ScriptException(s"compile-time error, input was incomplete:\n$script")
  //      case e                   ⇒ throw messageToException(omIMain.errorMessage, script)
  //    }
  //  }

  def compile(code: String): ScalaREPL.Compiled = synchronized {
    omIMain.errorMessage = Nil
    val scripted = new OMScripted(new nsc.interpreter.Scripted.Factory, settings, out, omIMain)

    try {
      val compiled = scripted.compile("\n" + omIMain.firstLineTag + "\n" + code)
      () ⇒ compiled.eval()
    }
    catch {
      case e: Throwable ⇒ throw messageToException(e, omIMain.errorMessage, code)
    }
  }

  def loopWithExitCode = {
    loop()
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

  lazy val omIMain = new OMIMain(settings, priorityBundles, jars, quiet)

  /* To avoid name clash with remote environment scala code interpretation
     when repl classes are copied to a bundle and shipped away. If some
     package name $linex are exported from a bundle it prevents the compilation
     of the matching line in the interpreter.*/
  val clField = omIMain.naming.getClass.getDeclaredField("freshLineId")
  val freshLineId = {
    var x = firstPackageLineNumber
    () ⇒ { x += 1; x }
  }
  clField.setAccessible(true)
  clField.set(omIMain.naming, freshLineId)

  intp = omIMain

}

object Interpreter {

  def apply(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[JFile] = Seq.empty, quiet: Boolean = true)(implicit newFile: NewFile, fileService: FileService) = {
    val classDirectory = newFile.newDir("classDirectory")
    fileService.deleteWhenGarbageCollected(classDirectory)
    new Interpreter(priorityBundles, jars, quiet, classDirectory)
  }
}

class Interpreter(priorityBundles: ⇒ Seq[Bundle], jars: Seq[JFile], quiet: Boolean, classDirectory: java.io.File) {
  def eval(code: String) = compile(code).apply()
  def compile(code: String): ScalaREPL.Compiled = synchronized {
    val settings = OSGiScalaCompiler.createSettings(new Settings, priorityBundles, jars, classDirectory)
    val iMain = new OMIMain(settings, priorityBundles, jars, quiet)
    val s = new OMScripted(new nsc.interpreter.Scripted.Factory, settings, new NewLinePrintWriter(new ConsoleWriter, true), iMain)
    val compiled = s.compile(code)
    () ⇒ compiled.eval()
  }
}

