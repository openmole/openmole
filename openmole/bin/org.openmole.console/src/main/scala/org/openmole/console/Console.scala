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
import org.openmole.core.console.ScalaREPL
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.project._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.services._
import org.openmole.core.workflow.mole._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.core.dsl._

import scala.annotation.tailrec
import scala.util._

object Console extends JavaLogger {

  def consoleSplash = org.openmole.core.buildinfo.consoleSplash

  lazy val consoleUsage = "(Type :q to quit)"

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

  def testPassword(password: String)(implicit preference: Preference): Boolean = {
    val cypher = Cypher(password)
    Preference.passwordIsCorrect(cypher, preference)
  }

  @tailrec def initPassword(implicit preference: Preference): String =
    if (Preference.passwordChosen(preference) && Preference.passwordIsCorrect(Cypher(""), preference)) ""
    else if (Preference.passwordChosen(preference)) {
      val password = passwordReader.readLine("Enter your OpenMOLE password (for preferences encryption): ", '*')
      val cypher = Cypher(password)
      if (!Preference.passwordIsCorrect(cypher, preference)) initPassword(preference)
      else password
    }
    else {
      println("OpenMOLE Password has not been set yet, choose a password.")
      val password = askPassword("Preferences password")
      val cypher = Cypher(password)
      Preference.setPasswordTest(preference, cypher)
      password
    }

  object ExitCodes {
    def ok = 0
    def incorrectPassword = 1
    def scriptDoesNotExist = 2
    def compilationError = 3
    def executionError = 6
    def restart = 254
  }

  def dealWithLoadError(load: ⇒ Iterable[(File, Throwable)], interactive: Boolean) = {
    val res = load
    res.foreach { case (f, e) ⇒ Log.logger.log(Log.WARNING, s"Error loading bundle $f", e) }
    if (interactive && !res.isEmpty) {
      print(s"Would you like to remove the failing bundles (${res.unzip._1.map(_.getName).mkString(", ")})? [y/N] ")
      val reader = new ConsoleReader()
      val c = reader.readCharacter('y', 'n', 'Y', 'N').asInstanceOf[Char]
      if (c.toLower == 'y') res.unzip._1.foreach(_.delete())
      println()
    }
    res
  }

}

import org.openmole.console.Console._

class Console(script: Option[String] = None) {
  console ⇒

  def commandsName = "_commands_"

  def run(args: Seq[String], workDirectory: Option[File], splash: Boolean = true)(implicit services: Services): Int = {
    import services._

    if (splash) {
      println(consoleSplash)
      println(consoleUsage)
    }

    script match {
      case None ⇒
        val variables = ConsoleVariables(args = args, workDirectory = workDirectory.getOrElse(currentDirectory), experiment = ConsoleVariables.Experiment("console"))
        withREPL(variables) { loop ⇒
          loop.storeErrors = false
          loop.loopWithExitCode
        }
      case Some(script) ⇒
        val scriptFile = new File(script)

        Project.compile(workDirectory.getOrElse(scriptFile.getParentFileSafe), scriptFile, args) match {
          case ScriptFileDoesNotExists() ⇒
            println("File " + scriptFile + " doesn't exist.")
            ExitCodes.scriptDoesNotExist
          case e: CompilationError ⇒
            tmpDirectory.directory.recursiveDelete
            println(e.error.stackString)
            ExitCodes.compilationError
          case compiled: Compiled ⇒
            Try(compiled.eval) match {
              case Success(res) ⇒
                val moleServices = MoleServices.create(applicationExecutionDirectory = services.workspace.tmpDirectory)
                val ex = dslToPuzzle(res).toExecution()(moleServices)
                Try(ex.run) match {
                  case Failure(e) ⇒
                    println(e.stackString)
                    ExitCodes.executionError
                  case Success(_) ⇒
                    ExitCodes.ok
                }
              case Failure(e) ⇒
                tmpDirectory.directory.recursiveDelete
                println(s"Error during script evaluation: ")
                print(e.stackString)
                ExitCodes.compilationError
            }

        }
    }
  }

  def withREPL[T](args: ConsoleVariables)(f: ScalaREPL ⇒ T)(implicit newFile: TmpDirectory, fileService: FileService) = {
    val loop =
      OpenMOLEREPL.newREPL(
        args,
        quiet = false
      )

    loop.beQuietDuring {
      loop.bind(commandsName, new Command(loop, args))
      loop interpret s"import $commandsName._"
    }

    f(loop)
    //    try f(loop)
    //    finally loop.close
  }

}
