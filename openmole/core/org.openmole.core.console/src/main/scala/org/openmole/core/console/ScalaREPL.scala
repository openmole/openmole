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
import javax.script.ScriptEngineManager

import org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader
import org.openmole.core.exception.{ UserBadDataError, InternalProcessingError }
import org.openmole.core.pluginmanager._
import org.osgi.framework.Bundle

import scala.annotation.tailrec
import scala.reflect.internal.util.{ NoPosition, Position }
import scala.tools.nsc.interpreter._
import scala.tools.nsc._
import scala.tools.nsc.reporters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class ScalaREPL(priorityBundles: ⇒ Seq[Bundle] = Nil, jars: Seq[JFile] = Seq.empty) extends ILoop {

  case class ErrorMessage(error: String, line: Int)
  var storeErrors: Boolean = true
  var errorMessage: List[ErrorMessage] = Nil
  var loopExitCode = 0

  System.setProperty("jline.shutdownhook", "true")
  //System.setProperty("scala.repl.debug", "true")
  override val prompt = "OpenMOLE>"

  in = new JLineReader(new JLineCompletion(this))

  super.getClass.getMethods.find(_.getName.contains("globalFuture_$eq")).get.invoke(this, Future { true }.asInstanceOf[AnyRef])

  settings = new Settings
  settings.Yreplsync.value = true
  settings.verbose.value = false

  def eval(code: String) = synchronized {
    errorMessage = Nil
    try intp.eval(code)
    catch {
      case e: Throwable ⇒
        def readableErrorMessages(error: ErrorMessage) =
          s"""Error while compiling:
            |${error.error}
            |on line ${error.line}""".stripMargin
        errorMessage match {
          case Nil ⇒ throw e
          case l ⇒
            def messages = l.map(readableErrorMessages).mkString("\n")
            throw new UserBadDataError(messages)
        }
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
          val linesLength = compiled.take(pos.line - 1).flatten.size + (pos.line - 1)

          val error = pos match {
            case NoPosition ⇒ ErrorMessage(msg, pos.line)
            case _ ⇒
              val offset = pos.start - linesLength

              println("POSstart " + pos.start)
              println("POSpoint " + pos.point)
              println("POSend " + pos.end)

              println("XX " + pos.finalPosition.start)
              println("POS " + new String(pos.source.content))
              ErrorMessage(
                s"""|$msg
                    |${compiled(pos.line - 1)}
                    |${new String((0 until offset).map(_ ⇒ ' ').toArray)}^""".stripMargin, pos.line)
          }

          errorMessage ::= error
        }
        super.error(pos, msg)
      }

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
              List(classOf[OSGiScalaCompiler].getClassLoader): _*)
        )
      }
      else super.classLoader
  }

}
