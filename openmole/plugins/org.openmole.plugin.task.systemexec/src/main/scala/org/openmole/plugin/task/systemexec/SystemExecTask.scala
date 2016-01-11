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
import org.openmole.core.workflow.builder.CanBuildTask
import org.openmole.core.workflow.tools.ExpandedString
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
    new SystemExecTaskBuilder(commands: _*) with CanBuildTask[SystemExecTask] {
      def toTask = canBuildTask.toTask
    }
}

case class ExpandedSystemExecCommand(expandedCommand: Expansion)

abstract class SystemExecTask(
    val command: Seq[OSCommands],
    val directory: Option[String],
    val errorOnReturnCode: Boolean,
    val returnValue: Option[Prototype[Int]],
    val output: Option[Prototype[String]],
    val error: Option[Prototype[String]],
    val variables: Seq[(Prototype[_], String)],
    val isRemote: Boolean = false) extends ExternalTask with SystemExecutor[RandomProvider] {

  @tailrec
  protected[systemexec] final def execAll(cmds: List[ExpandedSystemExecCommand], workDir: File, preparedContext: Context)(implicit rng: RandomProvider): Int =
    cmds match {
      case Nil ⇒ 0
      case cmd :: t ⇒
        val commandline = commandLine(cmd.expandedCommand, workDir, preparedContext)

        val retCode = execute(commandline, out, err, workDir, preparedContext)
        if (errorOnReturnCode && retCode != 0)
          throw new InternalProcessingError(
            s"""Error executing command"}:
                 |[${commandline.mkString(" ")}] return code was not 0 but ${retCode}""".stripMargin)

        if (t.isEmpty || retCode != 0) retCode
        else execAll(t, workDir, preparedContext)
    }

  override protected def process(context: Context)(implicit rng: RandomProvider) = withWorkDir { tmpDir ⇒
    val workDir =
      directory match {
        case None    ⇒ tmpDir
        case Some(d) ⇒ new File(tmpDir, d)
      }

    val preparedContext = prepareInputFiles(context, tmpDir, workDirPath)

    val osCommandLines: Seq[ExpandedSystemExecCommand] = command.find { _.os.compatible }.map {
      cmd ⇒ cmd.expanded map { expansion ⇒ ExpandedSystemExecCommand(expansion) }
    }.getOrElse(throw new UserBadDataError("Not command line found for " + OS.actualOS))

    val retCode = execAll(osCommandLines.toList, workDir, preparedContext)
    val retContext: Context = fetchOutputFiles(preparedContext, workDir, workDirPath)

    retContext ++
      List(
        output.map { o ⇒ Variable(o, outBuilder.toString) },
        error.map { e ⇒ Variable(e, errBuilder.toString) },
        returnValue.map { r ⇒ Variable(r, retCode) }
      ).flatten
  }

}
