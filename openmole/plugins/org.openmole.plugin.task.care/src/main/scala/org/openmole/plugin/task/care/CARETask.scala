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
import java.nio.file.Files
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.builder.CanBuildTask
import org.openmole.core.workflow.data.{ Context, Variable }
import org.openmole.core.workflow.tools.VariableExpansion
import org.openmole.plugin.task.external.ExternalTask
import org.openmole.tool.file._

import org.openmole.tool.logger.Logger
import org.openmole.plugin.task.systemexec._
import org.openmole.plugin.task.systemexec
import org.openmole.core.workflow.data._

object CARETask extends Logger {

  // TODO command and archiveWorkDirectory should be optional now that we have the utilities to dig into the archive
  def apply(archiveLocation: String, command: String, archiveWorkDirectory: String) = {

    val archive = archiveLocation.split('/').last.split('.').head

    def reExecuteCommand(commandline: String) = s"./${archive}/re-execute.sh ${commandline}"

    // FIXME does it actually need archiveLocation => reuse archive / archive name?
    new CARETaskBuilder(archiveLocation, reExecuteCommand(command), archiveWorkDirectory) with CanBuildTask[CARETask] {
      def toTask = canBuildTask2.toTask
    }.addResource(new File(archiveLocation))

  }
}

abstract class CARETask(
    val archiveLocation: String,
    val command: systemexec.Command,
    val archiveWorkDirectory: String,
    val directory: Option[String],
    val errorOnReturnCode: Boolean,
    val returnValue: Option[Prototype[Int]],
    val output: Option[Prototype[String]],
    val error: Option[Prototype[String]],
    val variables: Seq[(Prototype[_], String)]) extends ExternalTask with SystemExecutor[RandomProvider] {

  override protected def process(context: Context)(implicit rng: RandomProvider) = withWorkDir { tmpDir ⇒

    // FIXME this can be factorised between tasks
    val workDir =
      directory match {
        case None    ⇒ tmpDir
        case Some(d) ⇒ new File(tmpDir, d)
      }

    val preparedContext = prepareInputFiles(context, tmpDir, workDirPath)

    val expandedCommand = VariableExpansion(command.command)
    val commandline = commandLine(expandedCommand, workDir, preparedContext)

    // prepare CARE archive and working environment
    extract(workDir)
    linkInputsOutputs(workDir)

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

  /**
   * Extract the CARE archive passed to the constructor to the task's working directory.
   *
   * @param workDir Task's working directory as defined in the context.
   */
  def extract(workDir: File) = {
    val careArchive = workDir / archiveName
    extractArchive(careArchive, workDir)
  }

  /** Create exchange directories between the task's work directory and the archive chrooted environment */
  def linkInputsOutputs(workDir: File) = {

    // cd ${archive}/rootfs/\\$$taskdir && ln \\-s \\-t . \\`readlink \\-f \\$$OLDPWD/${archive}/rootfs/inputs\\`/\\*;
    // cd \\$$OLDPWD/${archive}/rootfs && ln \\-s \\$$taskdir ./outputs

    // TODO factorise
    val archiveFolder = archiveName.split('.').head

    val inputSource = workDir / s"./${archiveFolder}/rootfs/${archiveWorkDirectory}/inputs"
    val inputTarget = workDir / "./inputs"

    val outputSource = workDir / s"./${archiveFolder}/rootfs/outputs"
    val outputTarget = workDir / s"./${archiveFolder}/rootfs/${archiveWorkDirectory}/outputs"

    Files.createDirectory(inputSource)
    inputTarget.createLink(inputSource)
    Files.createDirectory(outputSource)
    outputTarget.createLink(outputSource)

    (inputSource, outputSource)
  }

}
