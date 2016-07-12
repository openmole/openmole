/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.console

import java.util

import jline.console.ConsoleReader
import org.openmole.core.console.ScalaREPL
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.project._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workspace._
import org.openmole.tool.file._

import scala.annotation.tailrec
import scala.util._

object Console {

  private def passwordReader = {
    val reader = new ConsoleReader()
    reader.setExpandEvents(false)
    reader
  }

  @tailrec def askPassword(msg: String = "password"): String = {
    val password = passwordReader.readLine(s"$msg             :", '*')
    val confirmation = passwordReader.readLine(s"$msg confirmation:", '*')
    if (password != confirmation) {
      println("Password and confirmation don't match.")
      askPassword(msg)
    }
    else password
  }

  def setPassword(password: String) =
    try {
      Workspace.setPassword(password)
      true
    }
    catch {
      case e: UserBadDataError ⇒
        println("Password incorrect.")
        false
    }

  @tailrec def initPassword: Unit = {
    if (Workspace.passwordChosen && Workspace.passwordIsCorrect("")) setPassword("")
    else {
      val password =
        if (Workspace.passwordChosen) passwordReader.readLine("Enter your OpenMOLE password (for preferences encryption): ", '*')
        else {
          println("OpenMOLE Password has not been set yet, choose a password.")
          askPassword("Preferences password")
        }
      if (!setPassword(password)) initPassword
    }
  }

  object ExitCodes {
    def ok = 0
    def incorrectPassword = 1
    def scriptDoesNotExist = 2
    def compilationError = 3
    def validationError = 5
    def executionError = 6
    def restart = 254
  }

}

import org.openmole.console.Console._

class Console(password: Option[String] = None, script: Option[String] = None) {
  console ⇒

  def workspace = "workspace"
  def registry = "registry"
  def logger = "logger"
  def serializer = "serializer"
  def commandsName = "_commands_"
  def pluginsName = "_plugins_"

  def run(args: ConsoleVariables, workDirectory: Option[File]): Int = {
    val correctPassword =
      password match {
        case None ⇒
          initPassword; true
        case Some(p) ⇒ setPassword(p)
      }

    correctPassword match {
      case false ⇒ ExitCodes.incorrectPassword
      case true ⇒

        script match {
          case None ⇒
            val newArgs = workDirectory.map(f ⇒ args.copy(workDirectory = f)).getOrElse(args)
            withREPL(newArgs) { loop ⇒
              loop.storeErrors = false
              loop.loopWithExitCode
            }
          case Some(script) ⇒
            ScalaREPL.warmup
            val scriptFile = new File(script)
            val project = new Project(workDirectory.getOrElse(scriptFile.getParentFileSafe))
            project.compile(scriptFile, args.args) match {
              case ScriptFileDoesNotExists() ⇒
                println("File " + scriptFile + " doesn't exist.")
                ExitCodes.scriptDoesNotExist
              case e: CompilationError ⇒
                println(e.error.stackString)
                ExitCodes.compilationError
              case compiled: Compiled ⇒
                Try(compiled.eval) match {
                  case Success(res) ⇒
                    val ex = res.toExecution()
                    Try(ex.start) match {
                      case Failure(e) ⇒
                        println(e.stackString)
                        ExitCodes.validationError
                      case Success(_) ⇒
                        Try(ex.waitUntilEnded) match {
                          case Success(_) ⇒ ExitCodes.ok
                          case Failure(e) ⇒
                            println("Error during script execution: ")
                            print(e.stackString)
                            ExitCodes.executionError
                        }
                    }
                  case Failure(e) ⇒
                    println(s"Error during script evaluation: ")
                    print(e.stackString)
                    ExitCodes.compilationError
                }

            }
        }

    }

  }

  def withREPL[T](args: ConsoleVariables)(f: ScalaREPL ⇒ T) = {
    val loop =
      OpenMOLEREPL.newREPL(
        args,
        quiet = false
      )

    loop.beQuietDuring {
      loop.bind(commandsName, new Command(loop, args))
      loop interpret s"import $commandsName._"
    }

    try f(loop)
    finally loop.close
  }

}
