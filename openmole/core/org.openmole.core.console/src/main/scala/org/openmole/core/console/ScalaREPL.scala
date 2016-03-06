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

import java.io.{ PrintStream, PrintWriter, Writer }
import java.net.URLClassLoader
import java.util.UUID

import org.openmole.core.exception._
import org.openmole.core.pluginmanager._
import org.osgi.framework.Bundle

import scala.annotation.tailrec
import scala.reflect.internal.util.{ NoPosition, Position }
import scala.tools.nsc._
import scala.tools.nsc.reporters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import java.io.PrintWriter
import scala.tools.nsc.interpreter._

object ScalaREPL {
  def warmup = new ScalaREPL().eval("def warmup() = {}")
  case class HeaderInfo(file: String)
  def firstLine(file: String) = HeaderInfo(file)
  lazy val firstLinePrefix = "##header"

}

class ScalaREPL(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[JFile] = Seq.empty, quiet: Boolean = true) extends ILoop {

  case class ErrorMessage(error: String, line: Int)
  var storeErrors: Boolean = true
  var errorMessage: List[ErrorMessage] = Nil
  var loopExitCode = 0

  System.setProperty("jline.shutdownhook", "true")
  override val prompt = "OpenMOLE>"

  lazy val firstLine = "/*" + UUID.randomUUID().toString + "*/"

  super.getClass.getMethods.find(_.getName.contains("globalFuture_$eq")).get.invoke(this, Future { true }.asInstanceOf[AnyRef])

  settings = new Settings
  settings.Yreplsync.value = true
  settings.verbose.value = false

  in = chooseReader(settings)

  private def messageToException(e: Throwable, messages: List[ErrorMessage], code: String): Throwable = {
    def readableErrorMessages(error: ErrorMessage) =
      s"""${error.error}
         |on line ${error.line}""".stripMargin

    errorMessage match {
      case Nil ⇒ e
      case l ⇒
        def messages =
          l.reverse.map(readableErrorMessages).mkString("\n") + "\n" +
            s"""Compiling code:
                |${code}
                |""".stripMargin
        new UserBadDataError(messages)
    }
  }

  def eval(code: String) = synchronized {
    errorMessage = Nil
    try intp.eval(firstLine + "\n" + code)
    catch {
      case e: Throwable ⇒
        throw messageToException(e, errorMessage, code)
    }
  }

  def compile(code: String) = synchronized {
    errorMessage = Nil
    try intp.compile(firstLine + "\n" + code)
    catch {
      case e: Throwable ⇒
        throw messageToException(e, errorMessage, code)
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

  intp = new IMain {

    override lazy val reporter = new ReplReporter(this) {

      override def error(pos: Position, msg: String): Unit = {
        if (storeErrors) {
          val compiled = new String(pos.source.content).split("\n")
          val first = compiled.zipWithIndex.find { case (l, _) ⇒ l.contains(firstLine) }.map(_._2).getOrElse(0)
          val error = pos match {
            case NoPosition ⇒ ErrorMessage(msg, pos.line - first)
            case _ ⇒
              ErrorMessage(Position.formatMessage(pos, msg, true), pos.line - first)
          }

          errorMessage ::= error
        }
        super.error(pos, msg)
      }

      override def printMessage(msg: String) = if (!quiet) super.printMessage(msg)

    }

    override protected def newCompiler(settings: Settings, reporter: Reporter) = {
      settings.exposeEmptyPackage.value = true
      if (Activator.osgi) {
        settings.outputDirs setSingleOutput replOutput.dir
        new OSGiScalaCompiler(settings, reporter, replOutput.dir, priorityBundles, jars)
      }
      else {
        case class Plop()
        settings.embeddedDefaults[Plop]
        //settings.usejavacp.value = true
        super.newCompiler(settings, reporter)
      }
    }

    override lazy val classLoader =
      if (Activator.osgi) {
        new scala.reflect.internal.util.AbstractFileClassLoader(
          replOutput.dir,
          new CompositeClassLoader(
            priorityBundles.map(_.classLoader) ++
              List(new URLClassLoader(jars.toArray.map(_.toURI.toURL))) ++
              List(classOf[OSGiScalaCompiler].getClassLoader): _*
          )
        )
      }
      else super.classLoader
  }

}
