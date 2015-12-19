/*
 *  Copyright (C) 2015 Jonathan Passerat-Palmbach
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

package org.openmole.plugin.task.care

import java.io.File
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.builder.CanBuildTask
import org.openmole.core.workflow.data.{ Context, Variable }
import org.openmole.core.workflow.tools.VariableExpansion
import org.openmole.plugin.task.external.ExternalTask
import org.openmole.tool.file._

import org.openmole.tool.logger.Logger
import org.openmole.plugin.task.systemexec._
import org.openmole.core.workflow.data._

import org.openmole.plugin.task.care._

object CARETask extends Logger {

  def apply(archiveLocation: String, command: String, archiveWorkDirectory: String) = {

    val archive = archiveLocation.split('/').last

    def reExecuteCommand(commandline: String) = s"./re-execute.sh/${commandline}"

    // TODO does it actually need archiveLocation?
    new CARETaskBuilder(archiveLocation, Command(reExecuteCommand(command)), archiveWorkDirectory) with CanBuildTask[CARETask] {
      def toTask: CARETask = new CARETask(
        archiveLocation, command, archiveWorkDirectory,
        workDirectory, errorOnReturnValue, returnValue, stdOut, stdErr, variables.toList) with this.Built {
        override val outputs: PrototypeSet = this.outputs + List(stdOut, stdErr, returnValue).flatten
      }
    }.addResource(new File(archiveLocation))

  }
}

abstract class CARETask(
    val archiveLocation: String,
    val command: Command,
    val archiveWorkDirectory: String,
    val directory: Option[String],
    val errorOnReturnCode: Boolean,
    val returnValue: Option[Prototype[Int]],
    val output: Option[Prototype[String]],
    val error: Option[Prototype[String]],
    val variables: Seq[(Prototype[_], String)]) extends ExternalTask with SystemExecutor[RandomProvider] {

  override protected def process(context: Context)(implicit rng: RandomProvider) = withWorkDir { tmpDir ⇒

    extract
    linkInputsOutputs

    val workDir =
      directory match {
        case None    ⇒ tmpDir
        case Some(d) ⇒ new File(tmpDir, d)
      }

    val preparedContext = prepareInputFiles(context, tmpDir, workDirPath)

    //    val osCommandLines: Seq[ExpandedSystemExecCommand] = command.find { _.os.compatible }.map {
    //      cmd ⇒ cmd.expanded map { expansion ⇒ ExpandedSystemExecCommand(expansion) }
    //    }.getOrElse(throw new UserBadDataError("Not command line found for " + OS.actualOS))

    val expandedCommand = VariableExpansion(command.toString)
    val commandline = commandLine(expandedCommand, workDir, preparedContext)

    // FIXME duplicated from SystemExecTask
    val retCode = execute(commandline, out, err, workDir, preparedContext)
    if (errorOnReturnCode && retCode != 0)
      throw new InternalProcessingError(
        s"""Error executing command"}:
                 |[${commandline.mkString(" ")}] return code was not 0 but ${retCode}""".stripMargin)

    //    val retCode = execAll(osCommandLines.toList, workDir, preparedContext)
    val retContext: Context = fetchOutputFiles(preparedContext, workDir, workDirPath)

    retContext ++
      List(
        output.map { o ⇒ Variable(o, outBuilder.toString) },
        error.map { e ⇒ Variable(e, errBuilder.toString) },
        returnValue.map { r ⇒ Variable(r, retCode) }
      ).flatten
  }

  val archiveName = archiveLocation.split("/").last

  def extract = {
    extractArchive(managedArchive(new File(".") / archiveName))
  }

  def linkInputsOutputs = {

    // cd ${archive}/rootfs/\\$$taskdir && ln \\-s \\-t . \\`readlink \\-f \\$$OLDPWD/${archive}/rootfs/inputs\\`/\\*;
    // cd \\$$OLDPWD/${archive}/rootfs && ln \\-s \\$$taskdir ./outputs

    val inputSource = new File(s"./${archiveName}/rootfs/${archiveWorkDirectory}/inputs")
    val inputTarget = new File("./inputs")

    val outputSource = new File(s"./${archiveName}/rootfs/outputs")
    val outputTarget = new File(s"./${archiveName}/rootfs/${archiveWorkDirectory}/outputs")

    inputSource.createLink(inputTarget)
    outputSource.createLink(outputTarget)

    (inputSource, outputSource)
  }

}
