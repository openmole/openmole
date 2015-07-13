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
import org.openmole.core.dsl.Serializer
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.logging.LoggerService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workflow.puzzle.Puzzle
import org.openmole.tool.file._
import org.openmole.core.workflow.tools.PluginInfo
import org.openmole.core.workspace._
import scala.annotation.tailrec
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.{ NamedParam, ILoop, JLineCompletion, JLineReader }
import org.openmole.core.workflow.task._
import java.util.concurrent.TimeUnit
import scala.tools.nsc.io.{ File ⇒ SFile }
import java.io.File
import org.openmole.core.tools.io.Prettifier._

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

    def executionError = 5
  }

}

import Console._

object ConsoleVariables {
  def empty = ConsoleVariables()

  def apply(
    args: Seq[String] = Seq.empty,
    workDirectory: File = currentDirectory): ConsoleVariables =
    ConsoleVariables(args, workDirectory, workDirectory, workDirectory)
}

case class ConsoleVariables(
  args: Seq[String],
  workDirectory: File,
  inputDirectory: File,
  outputDirectory: File)

class Console(plugins: PluginSet = PluginSet.empty, password: Option[String] = None, script: List[String] = Nil) {
  console ⇒

  def workspace = "workspace"

  def registry = "registry"

  def logger = "logger"

  def serializer = "serializer"

  def commandsName = "_commands_"

  def pluginsName = "_plugins_"

  def variablesName = "_variables_"

  def autoImports: Seq[String] = PluginInfo.pluginsInfo.toSeq.flatMap(_.namespaces).map(n ⇒ s"$n._")

  def imports =
    Seq(
      "org.openmole.core.dsl._",
      s"$commandsName._"
    ) ++ autoImports

  def initialisationCommands =
    Seq(
      s"implicit lazy val plugins = $pluginsName",
      imports.map("import " + _).mkString("; ")
    )

  def run(args: ConsoleVariables, workDirectory: Option[File] = None): Int = {
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
          case Nil ⇒
            val newArgs = workDirectory.map(f ⇒ args.copy(workDirectory = f)).getOrElse(args)
            withREPL(newArgs) { loop ⇒
              loop.storeErrors = false
              loop.loopWithExitCode
            }
          case scripts ⇒
            def execute(script: List[File]): Int = {
              if (script.isEmpty) ExitCodes.ok
              else {
                val ret: Int = {
                  val scriptFile = script.head
                  if (scriptFile.exists) {
                    val wd = workDirectory.getOrElse(scriptFile.getParentFile)
                    val newArgs: ConsoleVariables =
                      args.copy(workDirectory = wd)
                    withREPL(newArgs) { loop ⇒
                      val compiled = loop.compile(scriptFile.content)
                      if (!loop.errorMessage.isEmpty) ExitCodes.compilationError
                      compiled.eval() match {
                        case res: Puzzle ⇒
                          val ex = res.toExecution()
                          ex.start
                          Try(ex.waitUntilEnded) match {
                            case Success(_) ⇒ ExitCodes.ok
                            case Failure(e) ⇒
                              println("Error during script execution: " + e.getMessage)
                              print(e.stackString)
                              ExitCodes.executionError
                          }
                        case _ ⇒
                          println(s"Script $scriptFile doesn't end with a puzzle")
                          ExitCodes.notAPuzzle
                      }
                    }
                  }
                  else {
                    println("File " + scriptFile + " doesn't exist.")
                    ExitCodes.scriptDoesNotExist
                  }
                }
                if (ret != ExitCodes.ok) ret else execute(script.tail)
              }
            }
            execute(scripts.map(new File(_)))
        }

    }
  }

  def initialise(loop: ScalaREPL, variables: ConsoleVariables) = {
    variables.workDirectory.mkdirs()
    variables.outputDirectory.mkdirs()
    loop.beQuietDuring {
      loop.bind(commandsName, new Command)
      loop.bind(pluginsName, plugins)
      initialisationCommands.foreach {
        loop.interpret
      }
      loop.bind(variablesName, variables)
      loop.eval(s"import $variablesName._")
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
