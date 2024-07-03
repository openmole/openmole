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

import org.jline.reader.*
import org.jline.terminal.*
import org.jline.keymap.*
import org.openmole.core.compiler.*
import org.openmole.core.fileservice.{FileService, FileServiceCache}
import org.openmole.core.preference.Preference
import org.openmole.core.project.*
import org.openmole.core.tools.io.Prettifier.*
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.services.*
import org.openmole.core.workflow.mole.*
import org.openmole.core.workspace.TmpDirectory
import org.openmole.core.dsl.*

import scala.annotation.tailrec
import scala.util.*

object Console extends JavaLogger {

  def consoleSplash = org.openmole.core.buildinfo.consoleSplash

  lazy val consoleUsage = "(Type :q to quit)"

  private def withReader[T](f: LineReader => T) =
    val terminal = TerminalBuilder.builder.build()
    val reader = LineReaderBuilder.builder().terminal(terminal).build()
    try f(reader)
    finally terminal.close()

  @tailrec def askPassword(msg: String = "password"): String = 
    val (password, confirmation) = 
      withReader { reader =>
        val password = reader.readLine(s"$msg             :", '*')
        val confirmation = reader.readLine(s"$msg confirmation:", '*')
        (password, confirmation)
      }
    if (password != confirmation) {
      println("Password and confirmation don't match.")
      askPassword(msg)
    } else password

  def testPassword(password: String)(implicit preference: Preference): Boolean = {
    val cypher = Cypher(password)
    Preference.passwordIsCorrect(cypher, preference)
  }

  def chosePassword(password: String)(implicit preference: Preference) =
    if (!Preference.passwordChosen(preference)) {
      val cypher = Cypher(password)
      Preference.setPasswordTest(preference, cypher)
    }

  @tailrec def initPassword(implicit preference: Preference): String =
    if (Preference.passwordChosen(preference) && Preference.passwordIsCorrect(Cypher(""), preference)) ""
    else if (Preference.passwordChosen(preference)) {
      val password = withReader(_.readLine("Enter your OpenMOLE password (for preferences encryption): ", '*'))
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

  object ExitCodes:
    def ok = 0
    def scriptDoesNotExist = 2
    def compilationError = 3
    def executionError = 6
    def restart = 254

  def dealWithLoadError(load: ⇒ Iterable[(File, Throwable)], interactive: Boolean) = {
    val res = load
    res.foreach { case (f, e) ⇒ Log.logger.log(Log.WARNING, s"Error loading bundle $f", e) }
    if (interactive && !res.isEmpty) {
      print(s"Would you like to remove the failing bundles (${res.unzip._1.map(_.getName).mkString(", ")})? [y/N] ")
      val terminal = TerminalBuilder.terminal()
      val reader = new BindingReader(terminal.reader())
      
      try 
        @scala.annotation.tailrec def readChar: Char =
          val c = reader.readCharacter().asInstanceOf[Char]
          if(Set('y', 'n', 'Y', 'N').contains(c)) c  else readChar

        val c = readChar
        if (c.toLower == 'y') res.unzip._1.foreach(_.delete())

        println()
      finally terminal.close()
        
    }
    res
  }

  class CompiledDSL(val dsl: DSL, val compilationContext: CompilationContext, val raw: Interpreter.RawCompiled)

}

import org.openmole.console.Console._

class Console(script: Option[String] = None) {
  console ⇒

  def commandsName = "_commands_"

  def run(args: Seq[String], workDirectory: Option[File], splash: Boolean = true)(implicit services: Services): Int = {
    if (splash) {
      println(consoleSplash)
      println(consoleUsage)
    }

    script match {
      case None ⇒
        import services._
        val variables = ConsoleVariables(args = args, workDirectory = workDirectory.getOrElse(currentDirectory), experiment = ConsoleVariables.Experiment("console"))
        withREPL(variables) { loop ⇒
          //loop.storeErrors = false
          //loop.loopWithExitCode
          loop.loop
          0
        }
      case Some(script) ⇒
        val scriptFile = new File(script)
        load(scriptFile, args, workDirectory) match
          case Right(dsl) =>
            Try(Command.start(dsl.dsl, dsl.compilationContext).hangOn()) match
              case Failure(e) ⇒
                println(e.stackString)
                ExitCodes.executionError
              case Success(_) ⇒
                ExitCodes.ok
          case Left(c) => c
    }
  }

  def load(script: File, args: Seq[String], workDirectory: Option[File])(implicit services: Services): Either[Int, Console.CompiledDSL] =
    val runServices = {
      import services._
      Services.copy(services)(fileServiceCache = FileServiceCache())
    }

    Project.compile(workDirectory.getOrElse(script.getParentFileSafe), script)(runServices) match {
      case ScriptFileDoesNotExists() ⇒
        println("File " + script + " doesn't exist.")
        Left(ExitCodes.scriptDoesNotExist)
      case e: CompilationError ⇒
        services.tmpDirectory.directory.recursiveDelete
        println(e.error.stackString)
        Left(ExitCodes.compilationError)
      case compiled: Compiled ⇒
        Try(compiled.eval(args)) match {
          case Success(res) ⇒ Right(Console.CompiledDSL(res, compiled.compilationContext, compiled.result))
          case Failure(e) ⇒
            services.tmpDirectory.directory.recursiveDelete
            println(s"Error during script evaluation: ")
            print(e.stackString)
            Left(ExitCodes.compilationError)
        }

    }

  def withREPL[T](args: ConsoleVariables)(f: REPL ⇒ T)(implicit newFile: TmpDirectory, fileService: FileService) = {
    args.workDirectory.mkdirs()

    val loop = OpenMOLEREPL.newREPL(quiet = false)

    ConsoleVariables.bindVariables(loop, args)
    loop.bind(commandsName, new Command(loop, args))
    loop.eval(s"import $commandsName._")

    try f(loop)
    finally loop.close
  }

}
