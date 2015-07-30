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

import jline.console.ConsoleReader
import java.util.concurrent.Executors
import org.openmole.core.console.ScalaREPL
import org.openmole.core.dsl.{ DSLPackage, Serializer }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.logging.LoggerService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.puzzle.{ PuzzleBuilder, Puzzle }
import org.openmole.tool.file._
import org.openmole.core.workflow.tools.PluginInfo
import org.openmole.core.workspace._
import scala.annotation.tailrec
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{ NamedParam, ILoop, JLineCompletion, JLineReader }
import org.openmole.core.workflow.task._
import java.util.concurrent.TimeUnit
import org.openmole.core.tools.io.Prettifier._
import org.openmole.tool.file._

import scala.util._

object Console {

  @tailrec def askPassword(msg: String = "password"): String = {
    val password = new ConsoleReader().readLine(s"$msg             :", '*')
    val confirmation = new ConsoleReader().readLine(s"$msg confirmation:", '*')
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
        if (Workspace.passwordChosen) new ConsoleReader().readLine("Enter your OpenMOLE password (for preferences encryption): ", '*')
        else {
          println("OpenMOLE Password for preferences encryption has not been set yet, choose a  password.")
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
    def notAPuzzle = 4
    def validationError = 5
    def executionError = 6
    def restart = 254
  }

}

import Console._

object ConsoleVariables {
  def empty = ConsoleVariables()

  def bindVariables(loop: ScalaREPL, variables: ConsoleVariables, variablesName: String = "_variables_") =
    loop.beQuietDuring {
      loop.bind(variablesName, variables)
      loop.eval(s"import $variablesName._")
    }

}

case class ConsoleVariables(
  args: Seq[String] = Seq.empty,
  workDirectory: File = currentDirectory)

class Console(password: Option[String] = None, script: Option[String] = None) {
  console ⇒

  def workspace = "workspace"
  def registry = "registry"
  def logger = "logger"
  def serializer = "serializer"
  def commandsName = "_commands_"
  def pluginsName = "_plugins_"

  def autoImports: Seq[String] = PluginInfo.pluginsInfo.toSeq.flatMap(_.namespaces).map(n ⇒ s"$n._")
  def keywordNamespace = "om"

  def keywordNamespaceCode =
    s"""
       |object $keywordNamespace extends ${classOf[DSLPackage].getCanonicalName} with ${PluginInfo.pluginsInfo.flatMap(_.keywordTraits).mkString(" with ")}
     """.stripMargin

  def imports =
    Seq(
      "org.openmole.core.dsl._",
      s"$commandsName._"
    ) ++ autoImports

  def initialisationCommands =
    Seq(
      imports.map("import " + _).mkString("; "),
      keywordNamespaceCode
    )

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
              case CompilationError(e) ⇒
                println(e.stackString)
                ExitCodes.compilationError
              case Compiled(compiled) ⇒
                compiled.eval() match {
                  case res: PuzzleBuilder ⇒
                    val ex = res.buildPuzzle.toExecution()
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
                  case _ ⇒
                    println(s"Script $scriptFile doesn't end with a puzzle")
                    ExitCodes.notAPuzzle
                }

            }
        }

    }

  }

  def initialise(loop: ScalaREPL, variables: ConsoleVariables) = {
    variables.workDirectory.mkdirs()
    loop.beQuietDuring {
      loop.bind(commandsName, new Command(loop, variables))
      initialisationCommands.foreach {
        loop.interpret
      }
      ConsoleVariables.bindVariables(loop, variables)
    }
    loop
  }

  def newREPL(args: ConsoleVariables) = {
    val loop = new ScalaREPL()
    initialise(loop, args)
  }

  def withREPL[T](args: ConsoleVariables)(f: ScalaREPL ⇒ T) = {
    val loop = newREPL(args)
    try f(loop)
    finally loop.close
  }

}
