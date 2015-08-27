/*
 *  Copyright (C) 2010 Romain Reuillon <romain.Romain Reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.systemexec

import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.tools.VariableExpansion.Expansion
import org.openmole.tool.file._
import org.openmole.core.tools.service.{ OS, ProcessUtil }
import ProcessUtil._
import java.io.File
import java.io.IOException
import java.io.PrintStream
import org.apache.commons.exec.CommandLine
import org.openmole.core.workflow.data._
import org.openmole.plugin.task.external._
import org.openmole.tool.logger.Logger
import org.openmole.tool.stream.StringOutputStream

import scala.annotation.tailrec

object SystemExecTask extends Logger {

  /**
   * System exec task execute an external process.
   * To communicate with the dataflow the result should be either a file / category or the return
   * value of the process.
   */
  def apply(commands: Command*) =
    new SystemExecTaskBuilder(commands: _*)

}

case class ExpandedSystemExecCommand(expandedCommand: Expansion, isRemote: Boolean = false)

abstract class SystemExecTask(
    val commands: Seq[OSCommands],
    val directory: Option[String],
    val errorOnReturnCode: Boolean,
    val returnValue: Option[Prototype[Int]],
    val output: Option[Prototype[String]],
    val error: Option[Prototype[String]],
    val variables: Seq[(Prototype[_], String)]) extends ExternalTask {

  override protected def process(context: Context)(implicit rng: RandomProvider) = withWorkDir { tmpDir ⇒
    val workDir =
      directory match {
        case None    ⇒ tmpDir
        case Some(d) ⇒ new File(tmpDir, d)
      }

    def workDirPath = directory.getOrElse("")
    val preparedContext = prepareInputFiles(context, tmpDir, workDirPath)

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

    def commandLine(cmd: Expansion, basePath: String = workDir.getAbsolutePath, separator: String = File.separator): Array[String] =
      CommandLine.parse(basePath + separator + cmd.expand(preparedContext + Variable(ExternalTask.PWD, workDir.getAbsolutePath))).toStrings

    def execute(command: Array[String], out: PrintStream, err: PrintStream): Int = {
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

    // find the sequence of command lines corresponding to the host system
    // unused
    val osCommandLines: Seq[ExpandedSystemExecCommand] = commands.find { _.os.compatible }.map {
      cmd ⇒ cmd.expanded map { case (expansion, remote) ⇒ ExpandedSystemExecCommand(expansion, remote) }
    }.getOrElse(
      throw new UserBadDataError("No command line found for " + OS.actualOS))

    @tailrec
    def execAll(cmds: Seq[ExpandedSystemExecCommand]): Int =
      cmds match {
        case Nil ⇒ 0
        case cmd :: t ⇒

          // commands in a task marked as remote are not executed from the working directory
          val commandline = {
            if (cmd.isRemote) {
              commandLine(cmd.expandedCommand, basePath = "", separator = "")
            }
            else commandLine(cmd.expandedCommand)
          }

          val retCode = execute(commandline, out, err)
          if (errorOnReturnCode && retCode != 0) {
            throw new InternalProcessingError(s"""
                                              Error executing ${if (cmd.isRemote) "remote command"}:
                                              [${commandline.mkString(" ")}] return code was not 0 but ${retCode}
                                              """)
          }

          if (t.isEmpty || (!errorOnReturnCode && retCode != 0)) retCode
          else execAll(t)
      }

    val retCode = execAll(osCommandLines.toList)
    val retContext: Context = fetchOutputFiles(preparedContext, workDir, workDirPath)

    retContext ++
      List(
        output.map { o ⇒ Variable(o, outBuilder.toString) },
        error.map { e ⇒ Variable(e, errBuilder.toString) },
        returnValue.map { r ⇒ Variable(r, retCode) }
      ).flatten

  }

}
