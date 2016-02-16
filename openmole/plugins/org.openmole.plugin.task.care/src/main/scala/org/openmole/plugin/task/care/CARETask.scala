/*
 *  Copyright (C) 2015 Jonathan Passerat-Palmbach
 *  Copyright (C) 2016 Romain Reuillon
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
  def apply(archive: File, command: String, workDirectory: Option[String]) = {

    // FIXME does it actually need archiveLocation => reuse archive / archive name?
    new CARETaskBuilder(archive, command, workDirectory) with CanBuildTask[CARETask] {
      def toTask = canBuildTask2.toTask
    }
  }
}

abstract class CARETask(
    val archive: File,
    val command: systemexec.Command,
    val workDirectory: Option[String],
    val errorOnReturnCode: Boolean,
    val returnValue: Option[Prototype[Int]],
    val output: Option[Prototype[String]],
    val error: Option[Prototype[String]],
    val environmentVariables: Seq[(Prototype[_], String)]) extends ExternalTask {

  archive.setExecutable(true)

  lazy val packagingDirectory: String = getCareBinInfos(archive).workDirectory.getOrElse(
    throw new InternalProcessingError(s"Could not find packaging path in ${archive}"))

  override protected def process(context: Context)(implicit rng: RandomProvider) = withWorkDir { taskWorkDirectory ⇒
    def userWorkDirectory = workDirectory.getOrElse(packagingDirectory)

    // unarchiving in task's work directory
    // no need to retrieve error => will throw exception if failing
    execute(Array(archive.getAbsolutePath), taskWorkDirectory, Seq.empty, Context.empty, true, true)

    val extractedArchive = taskWorkDirectory.listFilesSafe.headOption.getOrElse(
      throw new InternalProcessingError("Work directory should contain extracted archive, but is empty"))

    val preparedContext = prepareInputFiles(context, taskWorkDirectory / "inputs", Some(userWorkDirectory))

    val proot = extractedArchive / "proot"
    proot move (extractedArchive / "proot.origin")

    /** Traverse directory hierarchy to retrieve terminal elements (files and empty directories) */
    def leafs(file: File, bindingDestination: String): Seq[(File, String)] =
      if (file.isDirectory)
        if (file.isDirectoryEmpty) Seq(file -> bindingDestination)
        else file.listFilesSafe.flatMap(f ⇒ leafs(f, s"$bindingDestination/${f.getName}"))
      else Seq(file -> bindingDestination)

    val bindings = leafs(taskWorkDirectory / "inputs", "").map { case (f, d) ⇒ s"-b ${f.getAbsolutePath}:$d" }.mkString(" \\ \n")

    // replace original proot executable with a script that will first bind all the inputs in the guest rootfs before
    // calling the original proot
    proot.content =
      s"""
        |#!/bin/bash
        |TRUEPROOT="$${PROOT-$$(dirname $$0)/proot.origin}"
        |$${TRUEPROOT} \
        | ${bindings} \
        | $$@
      """.stripMargin

    val reExecute = extractedArchive / "re-execute.sh"
    reExecute.content = reExecute.lines.map {
      case line if line.trim.startsWith("-w") ⇒ s"-w $userWorkDirectory"
      case line                               ⇒ line
    }.mkString("\n")

    val expandedCommand = VariableExpansion(s"./${reExecute.getName} ${command.command}")
    val commandline = commandLine(expandedCommand, userWorkDirectory, preparedContext)

    // FIXME duplicated from SystemExecTask
    val executionResult = execute(commandline, extractedArchive, environmentVariables, preparedContext, output.isDefined, error.isDefined)
    if (errorOnReturnCode && executionResult.returnCode != 0)
      throw new InternalProcessingError(
        s"""Error executing command":
                 |[${commandline.mkString(" ")}] return code was not 0 but ${executionResult.returnCode}""".stripMargin)

    val retContext: Context = fetchOutputFiles(preparedContext, extractedArchive / "rootfs", Some(userWorkDirectory))

    retContext ++
      List(
        // .GET => because!
        output.map { o ⇒ Variable(o, executionResult.output.get) },
        error.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
        returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
      ).flatten
  }
}
