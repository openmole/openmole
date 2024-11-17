/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.task

import java.io.{ File, IOException, PrintStream }

import monocle.Lens
import org.apache.commons.exec.CommandLine
import org.openmole.core.context._
import org.openmole.core.exception._
import org.openmole.core.argument.FromContext
import org.openmole.core.tools.service.OS
import org.openmole.core.tools.service.ProcessUtil._
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.task.external._
import org.openmole.tool.random._
import org.openmole.tool.stream._
import org.openmole.core.argument._
import cats.implicits._

package systemexec {

  /**
   * Command line representation
   *
   * @param command the actual command line to be executed
   */
  case class Command(command: String)

  object Command {
    /** Make commands non-remote by default */
    implicit def stringToCommand(s: String): Command = Command(s)
  }

  implicit def seqStringToCommand(s: Seq[String]): Seq[Command] = s.map(s => Command(s))

  /**
   * Sequence of commands for a particular OS
   *
   * @param os    target Operating System
   * @param parts Sequence of commands to be executed
   * @see Command
   */
  case class OSCommands(os: OS, parts: Command*) {
    @transient lazy val expanded = parts.map(c ⇒ ExpandedString(c.command))
  }

  object OSCommands {
    /** A single command can be a sequence  */
    implicit def stringToCommands(s: String): OSCommands = OSCommands(OS(), s)

    /** A sequence of command lines is considered local (non-remote) by default */
    implicit def seqOfStringToCommands(s: Seq[String]): OSCommands = OSCommands(OS(), s.map(s ⇒ Command(s)) *)
  }

}

package object systemexec {

  private[systemexec] def pack = this

  object ExecutionResult {
    def empty = ExecutionResult(0, None, None)
    def append(e1: ExecutionResult, e2: ExecutionResult) =
      ExecutionResult(
        e2.returnCode,
        appendOption(e1.output, e2.output),
        appendOption(e1.errorOutput, e2.errorOutput)
      )

    private def appendOption(o1: Option[String], o2: Option[String]) =
      (o1, o2) match {
        case (None, None)         ⇒ None
        case (o1, None)           ⇒ o1
        case (None, o2)           ⇒ o2
        case (Some(v1), Some(v2)) ⇒ Some(v1 + v2)
      }
  }

  /**
   * Result of a system execution
   * @param returnCode
   * @param output
   * @param errorOutput
   */
  case class ExecutionResult(returnCode: Int, output: Option[String], errorOutput: Option[String])

  def commandLine(cmd: String) = parse(cmd).toArray

  /**
   * parse a command line
   * @param line
   * @return
   */
  def parse(line: String, keepQuotes: Boolean = false): Vector[String] = {
    var inDoubleQuote = false
    var inSingleQuote = false
    var blocked = false
    var afterSpace = false

    val arguments = collection.mutable.ListBuffer[String]()
    val currentArguments = new StringBuilder

    def addArgument() = {
      arguments += currentArguments.toString()
      currentArguments.clear()
    }

    for (character ← line) {
      (inDoubleQuote, inSingleQuote, blocked, character, afterSpace) match {
        case (_, _, true, c, _) ⇒
          currentArguments.append(c)
          blocked = false

        case (_, _, false, '\\', _) ⇒
          blocked = true

        case (false, false, false, ' ', _) ⇒
          if (!currentArguments.isEmpty) addArgument()

        case (false, false, false, '"', _) ⇒
          inDoubleQuote = true
          if (keepQuotes) currentArguments.append('"')

        case (true, false, false, '"', false) ⇒
          inDoubleQuote = false
          if (keepQuotes) currentArguments.append('"')

        case (false, false, false, '\'', _) ⇒
          inSingleQuote = true
          if (keepQuotes) currentArguments.append('\'')

        case (false, true, false, '\'', false) ⇒
          inSingleQuote = false
          if (keepQuotes) currentArguments.append(character)

        case (false, false, false, c, _) ⇒ currentArguments.append(c)
        case (true, false, false, c, _)  ⇒ currentArguments.append(c)
        case (false, true, false, c, _)  ⇒ currentArguments.append(c)
        case _                           ⇒ throw new RuntimeException(s"Error while parsing command line: $line")
      }

      afterSpace = character == ' '
    }

    if (!currentArguments.isEmpty) addArgument()

    arguments.toVector
  }

  /**
   * Execute a parsed command
   * @param command
   * @param workDir
   * @param environmentVariables
   * @param captureOutput
   * @param captureError
   * @param errorOnReturnValue
   * @param displayOutput
   * @param displayError
   * @param stdOut
   * @param stdErr
   * @return
   */
  def execute(
    command:              Array[String],
    workDir:              File,
    environmentVariables: Vector[(String, String)],
    captureOutput:        Boolean,
    captureError:         Boolean,
    errorOnReturnValue:   Boolean                  = true,
    displayOutput:        Boolean                  = true,
    displayError:         Boolean                  = true,
    stdOut:               PrintStream              = System.out,
    stdErr:               PrintStream              = System.err
  ) = {
    try {

      val outBuilder = new StringOutputStream
      val errBuilder = new StringOutputStream

      val out: PrintStream =
        (displayOutput, captureOutput) match {
          case (true, false)  ⇒ stdOut
          case (false, true)  ⇒ new PrintStream(outBuilder)
          case (true, true)   ⇒ new PrintStream(MultiplexedOutputStream(outBuilder, stdOut))
          case (false, false) ⇒ new PrintStream(new NullOutputStream)
        }

      val err =
        (displayError, captureError) match {
          case (true, false)  ⇒ stdErr
          case (false, true)  ⇒ new PrintStream(errBuilder)
          case (true, true)   ⇒ new PrintStream(MultiplexedOutputStream(errBuilder, stdErr))
          case (false, false) ⇒ new PrintStream(new NullOutputStream)
        }

      val runtime = Runtime.getRuntime

      import scala.jdk.CollectionConverters._
      val inheritedEnvironment: Array[String] = System.getenv.asScala.map { case (key, value) ⇒ s"$key=$value" }.toArray

      val openmoleEnvironment: Array[String] = environmentVariables.map { case (name, value) ⇒ name + "=" + value }.toArray

      //FIXES java.io.IOException: error=26
      val process = runtime.synchronized {
        runtime.exec(
          command,
          inheritedEnvironment ++ openmoleEnvironment,
          workDir
        )
      }

      // debugging - please do not remove
      //println("Running command:\n" + command.toList) //+ "\n" + inheritedEnvironment.mkString("\n") + "\n" + openmoleEnvironment.mkString("\n"))

      val returnCode = executeProcess(process, out, err)

      val result =
        ExecutionResult(
          returnCode,
          if (captureOutput) Some(outBuilder.toString) else None,
          if (captureError) Some(errBuilder.toString) else None
        )

      if (errorOnReturnValue && result.returnCode != 0) throw error(command.toVector, result)
      result
    }
    catch {
      case e: IOException ⇒ throw new InternalProcessingError(
        e,
        s"""Error executing: ${command.mkString(" ")}

            |The content of the working directory was ($workDir):
            |${directoryContentInformation(workDir)}
          """.stripMargin
      )
    }
  }

  /**
   * Throw an error for a command and its result
   * @param commandLine
   * @param executionResult
   * @return
   */
  def error(commandLine: Vector[String], executionResult: ExecutionResult) = {
    def output = executionResult.output.map(o ⇒ s"\nStandard output was:\n$o")
    def error = executionResult.errorOutput.map(e ⇒ s"\nError output was:\n$e")

    throw new InternalProcessingError(
      s"""Error executing command:
         |${commandLine.mkString(" ")}, return code was not 0 but ${executionResult.returnCode}""".stripMargin +
        output.getOrElse("") +
        error.getOrElse("")
    )
  }

  sealed trait ExecutionCommand
  object ExecutionCommand {

    /**
     * The raw command may be split depending on parsing level for quotes in different parts
     * @param command
     * @param keepQuotes
     */
    case class Raw(command: List[String], keepQuotes: List[Boolean]) extends ExecutionCommand
    case class Parsed(command: String*) extends ExecutionCommand

    object Raw {
      def apply(command: String): Raw = Raw(List(command), List(false))
      def apply(commands: List[String]): Raw = Raw(commands, List.fill(commands.size)(false))
      def apply(commands: List[String], keepQuotes: Boolean): Raw = Raw(commands, List.fill(commands.size)(keepQuotes))
    }

    def parse(executionCommand: ExecutionCommand): Vector[String] =
      executionCommand match {
        case c: Raw    ⇒ c.command.zip(c.keepQuotes).map { case (com, k) ⇒ systemexec.parse(com, k) }.reduce(_ ++ _)
        case p: Parsed ⇒ p.command.toVector
      }

    implicit def stringToRaw(c: String): Raw = Raw(c)
  }

  /**
   * Execute a list of commands
   * @param workDirectory
   * @param environmentVariables
   * @param commands
   * @param errorOnReturnValue
   * @param captureOutput
   * @param captureError
   * @param displayOutput
   * @param displayError
   * @param stdOut
   * @param stdErr
   * @return
   */
  def executeAll(
    workDirectory:        File,
    environmentVariables: Vector[(String, String)],
    commands:             List[ExecutionCommand],
    errorOnReturnValue:   Boolean                  = true,
    captureOutput:        Boolean                  = false,
    captureError:         Boolean                  = false,
    displayOutput:        Boolean                  = true,
    displayError:         Boolean                  = true,
    stdOut:               PrintStream              = System.out,
    stdErr:               PrintStream              = System.err
  ): ExecutionResult = {

    @annotation.tailrec
    def executeAll0(commands: List[ExecutionCommand], acc: ExecutionResult): ExecutionResult =
      commands match {
        case Nil ⇒ acc
        case cmd :: t ⇒
          val cl = ExecutionCommand.parse(cmd)
          val result = execute(cl.toArray, workDirectory, environmentVariables, captureOutput = captureOutput, captureError = captureError, errorOnReturnValue = false)
          if (errorOnReturnValue && result.returnCode != 0) throw error(cl, result)
          else executeAll0(t, ExecutionResult.append(acc, result))
      }

    executeAll0(commands, ExecutionResult.empty)
  }

  sealed trait Shell
  case object NoShell extends Shell
  case object Bash extends Shell

}
