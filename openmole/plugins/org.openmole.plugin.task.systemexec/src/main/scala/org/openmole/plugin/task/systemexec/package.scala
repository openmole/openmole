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

import org.openmole.core.macros.Keyword._
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.data.Prototype
import org.openmole.core.workflow.tools.VariableExpansion

package systemexec {

  import java.io.{ IOException, PrintStream, File }

  import org.apache.commons.exec.CommandLine
  import org.openmole.core.exception.{ UserBadDataError, InternalProcessingError }
  import org.openmole.core.tools.service.ProcessUtil._
  import org.openmole.core.workflow.data.{ RandomProvider, Context, Variable }
  import org.openmole.core.workflow.tools.VariableExpansion.Expansion
  import org.openmole.plugin.task.external.ExternalTask
  import org.openmole.tool.stream.StringOutputStream
  import org.openmole.tool.file._

  trait SystemExecPackage {

    /**
     * Command line representation
     *
     * @param command the actual command line to be executed
     */
    case class Command(command: String)

    /**
     * Sequence of commands for a particular OS
     *
     * @param os target Operating System
     * @param parts Sequence of commands to be executed
     * @see Command
     */
    case class OSCommands(os: OS, parts: Command*) {
      @transient lazy val expanded = parts.map(c ⇒ (VariableExpansion(c.command)))
    }

    /** Make commands non-remote by default */
    implicit def stringToCommand(s: String) = Command(s)
    /** A single command can be a sequence  */
    implicit def stringToCommands(s: String) = OSCommands(OS(), s)
    /** A sequence of command lines is considered local (non-remote) by default */
    implicit def seqOfStringToCommands(s: Seq[String]): OSCommands = OSCommands(OS(), s.map(s ⇒ Command(s)): _*)

    lazy val errorOnReturnCode = set[{ def setErrorOnReturnValue(b: Boolean) }]

    lazy val returnValue = set[{ def setReturnValue(v: Option[Prototype[Int]]) }]

    lazy val stdOut = set[{ def setStdOut(v: Option[Prototype[String]]) }]

    lazy val stdErr = set[{ def setStdErr(v: Option[Prototype[String]]) }]

    lazy val commands = add[{ def addCommand(os: OS, cmd: OSCommands*) }]

    lazy val environmentVariable =
      new {
        def +=(prototype: Prototype[_], variable: Option[String] = None) =
          (_: SystemExecTaskBuilder).addEnvironmentVariable(prototype, variable)
      }

    lazy val workDirectory = set[{ def setWorkDirectory(s: Option[String]) }]
  }

  // FIXME keep on factorising and insert in CARETask/SystemExecTask
  trait SystemExecutor[T] {

    def directory: Option[String]
    def output: Option[Prototype[String]]
    def error: Option[Prototype[String]]
    def variables: Seq[(Prototype[_], String)]

    val outBuilder = new StringOutputStream
    val errBuilder = new StringOutputStream

    val out = output match {
      case Some(_) ⇒ new PrintStream(outBuilder)
      case None    ⇒ System.out
    }
    val err = error match {
      case Some(_) ⇒ new PrintStream(errBuilder)
      case None    ⇒ System.err
    }

    protected[systemexec] def workDirPath: String = directory.getOrElse("")

    protected[systemexec] def commandLine(cmd: Expansion,
                                          workDir: File,
                                          preparedContext: Context)(implicit rng: RandomProvider): Array[String] =
      CommandLine.parse(cmd.expand(preparedContext + Variable(ExternalTask.PWD, workDir.getAbsolutePath))).toStrings

    protected[systemexec] def execute(command: Array[String],
                                      out: PrintStream,
                                      err: PrintStream,
                                      workDir: File,
                                      preparedContext: Context): Int = {
      try {
        val runtime = Runtime.getRuntime

        //FIXES java.io.IOException: error=26
        val process = runtime.synchronized {
          runtime.exec(
            command,
            variables.map { case (p, v) ⇒ v + "=" + preparedContext(p).toString }.toArray,
            workDir)
        }

        executeProcess(process, out, err)
      }
      catch {
        case e: IOException ⇒ throw new InternalProcessingError(e,
          s"""Error executing: ${command}
            |The content of the working directory was:
            |${workDir.listRecursive(_ ⇒ true).map(_.getPath).mkString("\n")}
          """.stripMargin
        )
      }
    }
  }
}

package object systemexec extends external.ExternalPackage with SystemExecPackage
