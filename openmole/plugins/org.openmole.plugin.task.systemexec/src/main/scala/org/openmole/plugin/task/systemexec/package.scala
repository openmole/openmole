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
import org.openmole.core.expansion.FromContext
import org.openmole.core.tools.service.OS
import org.openmole.core.tools.service.ProcessUtil._
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.task.external._
import org.openmole.tool.random._
import org.openmole.tool.stream._
import org.openmole.core.expansion._
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
    implicit def stringToCommand(s: String) = Command(s)
  }

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
    implicit def stringToCommands(s: String) = OSCommands(OS(), s)

    /** A sequence of command lines is considered local (non-remote) by default */
    implicit def seqOfStringToCommands(s: Seq[String]): OSCommands = OSCommands(OS(), s.map(s ⇒ Command(s)): _*)
  }

  import org.openmole.core.context.Val
  import org.openmole.core.workflow.builder.InputOutputBuilder
  import org.openmole.plugin.task.external.ExternalPackage

  trait ReturnValue[T] {
    def returnValue: Lens[T, Option[Val[Int]]]
  }

  trait ErrorOnReturnValue[T] {
    def errorOnReturnValue: Lens[T, Boolean]
  }

  trait StdOutErr[T] {
    def stdOut: Lens[T, Option[Val[String]]]
    def stdErr: Lens[T, Option[Val[String]]]
  }

  trait WorkDirectory[T] {
    def workDirectory: Lens[T, Option[String]]
  }

  trait SystemExecPackage {

    @deprecated("Use task arguments", "8")
    lazy val errorOnReturnValue =
      new {
        def :=[T: ErrorOnReturnValue](b: Boolean) =
          implicitly[ErrorOnReturnValue[T]].errorOnReturnValue.set(b)
      }

    @deprecated("Use task arguments", "8")
    lazy val returnValue =
      new {
        def :=[T: ReturnValue](v: OptionalArgument[Val[Int]]) =
          implicitly[ReturnValue[T]].returnValue.set(v)
      }

    @deprecated("Use task arguments", "8")
    lazy val stdOut =
      new {
        def :=[T: StdOutErr](v: OptionalArgument[Val[String]]) =
          implicitly[StdOutErr[T]].stdOut.set(v)
      }

    @deprecated("Use task arguments", "8")
    lazy val stdErr =
      new {
        def :=[T: StdOutErr](v: OptionalArgument[Val[String]]) =
          implicitly[StdOutErr[T]].stdErr.set(v)
      }

    @deprecated("Use task arguments", "8")
    lazy val commands = new {
      def +=[T: SystemExecTaskBuilder](os: OS, cmd: Command*): T ⇒ T =
        (implicitly[SystemExecTaskBuilder[T]].commands add OSCommands(os, cmd: _*))
    }

    @deprecated("Use task arguments", "8")
    lazy val customWorkDirectory =
      new {
        def :=[T: WorkDirectory](s: OptionalArgument[String]) =
          implicitly[WorkDirectory[T]].workDirectory.set(s)
      }

  }
}

package object systemexec extends SystemExecPackage {

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

  case class ExecutionResult(returnCode: Int, output: Option[String], errorOutput: Option[String])

  def commandLine(cmd: String) = parse(cmd).toArray

  def parse(line: String) = {
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
        // currentArguments.append(character)

        case (true, false, false, '"', false) ⇒
          inDoubleQuote = false
        //currentArguments.append(character)

        case (false, false, false, ''', _) ⇒
          inSingleQuote = true
        //currentArguments.append(character)

        case (false, true, false, ''', false) ⇒
          inSingleQuote = false
        //currentArguments.append(character)

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

      import collection.JavaConverters._
      val inheritedEnvironment = System.getenv.asScala.map { case (key, value) ⇒ s"$key=$value" }.toArray

      val openmoleEnvironment = environmentVariables.map { case (name, value) ⇒ name + "=" + value }.toArray

      //FIXES java.io.IOException: error=26
      val process = runtime.synchronized {
        runtime.exec(
          command,
          inheritedEnvironment ++ openmoleEnvironment,
          workDir
        )
      }

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

    case class Raw(command: String) extends ExecutionCommand
    case class Parsed(command: String*) extends ExecutionCommand

    def parse(executionCommand: ExecutionCommand): Vector[String] =
      executionCommand match {
        case c: Raw    ⇒ systemexec.parse(c.command)
        case p: Parsed ⇒ p.command.toVector
      }

    implicit def stringToRaw(c: String) = Raw(c)
  }

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
