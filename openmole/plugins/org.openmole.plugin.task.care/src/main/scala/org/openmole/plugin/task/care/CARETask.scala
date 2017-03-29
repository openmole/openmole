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

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.external.{ External, ExternalBuilder }
import org.openmole.plugin.task.systemexec._
import org.openmole.core.expansion._
import org.openmole.tool.logger.Logger
import org.openmole.tool.random._
import org.openmole.plugin.task.container

import cats.implicits._

object CARETask extends Logger {
  implicit def isTask: InputOutputBuilder[CARETask] = InputOutputBuilder(CARETask._config)
  implicit def isExternal: ExternalBuilder[CARETask] = ExternalBuilder(CARETask.external)

  implicit def isBuilder = new ReturnValue[CARETask] with ErrorOnReturnValue[CARETask] with StdOutErr[CARETask] with EnvironmentVariables[CARETask] with WorkDirectory[CARETask] with HostFiles[CARETask] {
    override def hostFiles = CARETask.hostFiles
    override def environmentVariables = CARETask.environmentVariables
    override def workDirectory = CARETask.workDirectory
    override def returnValue = CARETask.returnValue
    override def errorOnReturnValue = CARETask.errorOnReturnValue
    override def stdOut = CARETask.stdOut
    override def stdErr = CARETask.stdErr
  }

  def apply(archive: File, command: String)(implicit sourceCodeName: sourcecode.Name): CARETask =
    new CARETask(
      archive = archive,
      command = command,
      hostFiles = Vector.empty,
      workDirectory = None,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      environmentVariables = Vector.empty,
      _config = InputOutputConfig(),
      external = External()
    )

}

@Lenses case class CARETask(
    archive:              File,
    hostFiles:            Vector[(String, Option[String])],
    command:              FromContext[String],
    workDirectory:        Option[String],
    errorOnReturnValue:   Boolean,
    returnValue:          Option[Val[Int]],
    stdOut:               Option[Val[String]],
    stdErr:               Option[Val[String]],
    environmentVariables: Vector[(String, FromContext[String])],
    _config:              InputOutputConfig,
    external:             External
) extends Task with ValidateTask {

  def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)

  def validateArchive(archive: File) =
    if (!archive.exists) container.ArchiveNotFound(archive)
    else if (!archive.canExecute) Seq(new UserBadDataError(s"Archive $archive must be executable. Make sure you upload it with x permissions"))
    else container.ArchiveOK

  override def validate: Seq[Throwable] = container.validateContainer(validateArchive)(archive, command, environmentVariables, external, this.inputs)

  override protected def process(ctx: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = External.withWorkDir(executionContext) { taskWorkDirectory ⇒

    taskWorkDirectory.mkdirs()

    val context = ctx + (External.PWD → taskWorkDirectory.getAbsolutePath)

    def rootfs = "rootfs"

    // unarchiving in task's work directory
    // no need to retrieve error => will throw exception if failing
    execute(Array(archive.getAbsolutePath), taskWorkDirectory, Vector.empty, returnOutput = true, returnError = true)

    val extractedArchive = taskWorkDirectory.listFilesSafe.headOption.getOrElse(
      throw new InternalProcessingError("Work directory should contain extracted archive, but is empty")
    )

    val reExecute = extractedArchive / "re-execute.sh"

    val packagingDirectory: String = workDirectoryLine(reExecute.lines).getOrElse(
      throw new InternalProcessingError(s"Could not find packaging path in $archive")
    )

    def userWorkDirectory = workDirectory.getOrElse(packagingDirectory)

    val inputDirectory = taskWorkDirectory / "inputs"

    def inputPathResolver = container.inputPathResolver(inputDirectory, userWorkDirectory) _

    val (preparedContext, preparedFilesInfo) = external.prepareAndListInputFiles(context, inputPathResolver)

    // Replace new proot with a version with user bindings
    val proot = extractedArchive / "proot"
    proot move (extractedArchive / "proot.origin")

    def preparedFileBindings = preparedFilesInfo.map { case (f, d) ⇒ d.getAbsolutePath → f.name }
    def hostFileBindings = hostFiles.map { case (f, b) ⇒ f → b.getOrElse(f) }
    def bindings = preparedFileBindings ++ hostFileBindings

    def createDestination(binding: (String, String)) = {
      import org.openmole.tool.file.{ File ⇒ OMFile }
      val (f, b) = binding

      if (OMFile(f).isDirectory) (taskWorkDirectory / rootfs / b).mkdirs()
      else {
        val dest = taskWorkDirectory / rootfs / b
        dest.getParentFileSafe.mkdirs()
        dest.createNewFile()
      }
    }

    for (binding ← bindings) createDestination(binding)

    // replace original proot executable with a script that will first bind all the inputs in the guest rootfs before
    // calling the original proot
    proot.content =
      s"""
        |#!/bin/bash
        |TRUEPROOT="$${PROOT-$$(dirname $$0)/proot.origin}"
        |$${TRUEPROOT} \\
        | ${bindings.map { case (f, d) ⇒ s"""-b "$f:$d"""" }.mkString(" \\\n")} \\
        | "$${@}"
      """.stripMargin

    proot.setExecutable(true)

    reExecute.content = reExecute.lines.map {
      case line if line.trim.startsWith("-w") ⇒ s"-w '$userWorkDirectory' \\"
      case line                               ⇒ line
    }.mkString("\n")

    reExecute.setExecutable(true)

    val commandline = commandLine(command.map(s"./${reExecute.getName} " + _), userWorkDirectory, preparedContext)

    def prootNoSeccomp = ("PROOT_NO_SECCOMP", "1")

    // FIXME duplicated from SystemExecTask
    val allEnvironmentVariables = environmentVariables.map { case (varName, variable) ⇒ (varName, variable.from(context)) } ++ Vector(prootNoSeccomp)
    val executionResult = execute(commandline, extractedArchive, allEnvironmentVariables, stdOut.isDefined, stdErr.isDefined)

    if (errorOnReturnValue && returnValue.isEmpty && executionResult.returnCode != 0) throw error(commandline.toVector, executionResult)

    def rootDirectory = extractedArchive / rootfs

    def outputPathResolver = container.outputPathResolver(preparedFileBindings, hostFileBindings, inputDirectory, userWorkDirectory, rootDirectory) _

    val retContext = external.fetchOutputFiles(preparedContext, outputPathResolver)
    external.checkAndClean(this, retContext, taskWorkDirectory)

    retContext ++
      List(
        stdOut.map { o ⇒ Variable(o, executionResult.output.get) },
        stdErr.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
        returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
      ).flatten
  }
}
