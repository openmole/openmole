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

import org.apache.commons.exec.CommandLine
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.macros.Keyword._
import org.openmole.core.tools.service.OS
import org.openmole.core.tools.service.ProcessUtil._
import org.openmole.core.workflow.data.{ Context, Prototype, RandomProvider, Variable }
import org.openmole.core.workflow.tools.{ FromContext, VariableExpansion }
import org.openmole.core.workflow.tools.VariableExpansion.Expansion
import org.openmole.plugin.task.external.ExternalTask
import org.openmole.tool.stream.StringOutputStream
import org.openmole.tool.file._

import collection.mutable.ListBuffer

package systemexec {

  import org.openmole.core.workflow.builder.InputOutputBuilder

  trait ReturnValue {
    protected var returnValue: Option[Prototype[Int]] = None

    def setReturnValue(p: Option[Prototype[Int]]): this.type = {
      returnValue = p
      this
    }
  }

  trait ErrorOnReturnValue {
    protected var errorOnReturnValue = true

    def setErrorOnReturnValue(b: Boolean): this.type = {
      errorOnReturnValue = b
      this
    }
  }

  trait StdOutErr {
    protected var stdOut: Option[Prototype[String]] = None
    protected var stdErr: Option[Prototype[String]] = None

    def setStdOut(p: Option[Prototype[String]]): this.type = {
      stdOut = p
      this
    }

    def setStdErr(p: Option[Prototype[String]]): this.type = {
      stdErr = p
      this
    }
  }

  trait EnvironmentVariables <: InputOutputBuilder {
    protected val environmentVariables = new ListBuffer[(Prototype[_], String)]

    /**
     * Add variable from openmole to the environment of the system exec task. The
     * environment variable is set using a toString of the openmole variable content.
     *
     * @param prototype the prototype of the openmole variable to inject in the environment
     * @param variable the name of the environment variable. By default the name of the environment
     *                 variable is the same as the one of the openmole protoype.
     */
    def addEnvironmentVariable(prototype: Prototype[_], variable: Option[String] = None): this.type = {
      environmentVariables += prototype → variable.getOrElse(prototype.name)
      addInput(prototype)
      this
    }
  }

  trait WorkDirectory {
    protected var workDirectory: Option[String] = None

    def setWorkDirectory(s: Option[String]): this.type = {
      workDirectory = s
      this
    }
  }

  trait SystemExecPackage {

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
      @transient lazy val expanded = parts.map(c ⇒ (VariableExpansion(c.command)))
    }

    object OSCommands {
      /** A single command can be a sequence  */
      implicit def stringToCommands(s: String) = OSCommands(OS(), s)

      /** A sequence of command lines is considered local (non-remote) by default */
      implicit def seqOfStringToCommands(s: Seq[String]): OSCommands = OSCommands(OS(), s.map(s ⇒ Command(s)): _*)
    }

    lazy val errorOnReturnValue =
      new {
        def :=(b: Boolean) = (_: ErrorOnReturnValue).setErrorOnReturnValue(b)
      }

    lazy val returnValue =
      new {
        def :=(v: Option[Prototype[Int]]) = (_: ReturnValue).setReturnValue(v)
      }

    lazy val stdOut =
      new {
        def :=(v: Option[Prototype[String]]) = (_: StdOutErr).setStdOut(v)
      }

    lazy val stdErr =
      new {
        def :=(v: Option[Prototype[String]]) = (_: StdOutErr).setStdErr(v)
      }

    lazy val commands = add[{ def addCommand(os: OS, cmd: OSCommands*) }]

    lazy val environmentVariable =
      new {
        def +=(prototype: Prototype[_], variable: Option[String] = None) =
          (_: EnvironmentVariables).addEnvironmentVariable(prototype, variable)
      }

    lazy val customWorkDirectory =
      new {
        def :=(s: Option[String]) = (_: WorkDirectory).setWorkDirectory(s)
      }

  }
}

package object systemexec extends external.ExternalPackage with SystemExecPackage {

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

  def commandLine(
    cmd:     FromContext[String],
    workDir: String,
    context: Context
  )(implicit rng: RandomProvider): Array[String] =
    CommandLine.parse(cmd.from(context + Variable(ExternalTask.PWD, workDir))).toStrings

  def execute(
    command:              Array[String],
    workDir:              File,
    environmentVariables: Seq[(Prototype[_], String)],
    context:              Context,
    returnOutput:         Boolean,
    returnError:          Boolean
  ) = {
    try {

      val outBuilder = new StringOutputStream
      val errBuilder = new StringOutputStream

      val out = if (returnOutput) new PrintStream(outBuilder) else System.out
      val err = if (returnError) new PrintStream(errBuilder) else System.err

      val runtime = Runtime.getRuntime

      //FIXES java.io.IOException: error=26
      val process = runtime.synchronized {
        runtime.exec(
          command,
          environmentVariables.map { case (p, v) ⇒ v + "=" + context(p).toString }.toArray,
          workDir
        )
      }

      ExecutionResult(
        executeProcess(process, out, err),
        if (returnOutput) Some(outBuilder.toString) else None,
        if (returnError) Some(errBuilder.toString) else None
      )
    }
    catch {
      case e: IOException ⇒ throw new InternalProcessingError(
        e,
        s"""Error executing: ${command.mkString(" ")}

            |The content of the working directory was:
            |${workDir.listRecursive(_ ⇒ true).map(_.getPath).mkString("\n")}
          """.stripMargin
      )
    }
  }
}
