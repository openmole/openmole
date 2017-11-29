/**
 * Copyright (C) 2017 Romain Reuillon
 * Copyright (C) 2017 Jonathan Passerat-Palmbach
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.task.udocker

import java.util.UUID

import monocle.macros._
import cats.implicits._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace._
import org.openmole.core.context._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.builder._
import org.openmole.core.expansion._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.systemexec._
import org.openmole.core.preference._
import org.openmole.plugin.task.container
import org.openmole.plugin.task.udocker.DockerMetadata._
import org.openmole.plugin.task.udocker.Registry._
import org.openmole.tool.cache._
import squants._
import squants.time.TimeConversions._
import org.openmole.tool.file._
import io.circe.generic.extras.auto._
import io.circe.jawn.{ decode, decodeFile }
import io.circe.syntax._
import org.openmole.core.threadprovider._
import org.openmole.plugin.task.container.HostFiles

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.language.postfixOps

object UDockerTask {

  import UDocker._

  val RegistryTimeout = ConfigurationLocation("UDockerTask", "RegistryTimeout", Some(1 minutes))

  implicit def isTask: InputOutputBuilder[UDockerTask] = InputOutputBuilder(UDockerTask._config)
  implicit def isExternal: ExternalBuilder[UDockerTask] = ExternalBuilder(UDockerTask.external)

  implicit def isBuilder = new ReturnValue[UDockerTask] with ErrorOnReturnValue[UDockerTask] with StdOutErr[UDockerTask] with EnvironmentVariables[UDockerTask] with HostFiles[UDockerTask] with ReuseContainer[UDockerTask] with WorkDirectory[UDockerTask] with UDockerUser[UDockerTask] { builder ⇒
    override def returnValue = UDockerTask.returnValue
    override def errorOnReturnValue = UDockerTask.errorOnReturnValue
    override def stdOut = UDockerTask.stdOut
    override def stdErr = UDockerTask.stdErr
    override def environmentVariables = UDockerTask.uDocker composeLens UDockerArguments.environmentVariables
    override def hostFiles = UDockerTask.uDocker composeLens UDockerArguments.hostFiles
    override def reuseContainer = UDockerTask.uDocker composeLens UDockerArguments.reuseContainer
    override def workDirectory = UDockerTask.uDocker composeLens UDockerArguments.workDirectory
    override def uDockerUser = UDockerTask.uDocker composeLens UDockerArguments.uDockerUser
  }

  def apply(
    image:           ContainerImage,
    command:         FromContext[String],
    installCommands: Vector[String]      = Vector.empty
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, threadProvider: ThreadProvider): UDockerTask = {
    val uDocker =
      UDockerArguments(
        localDockerImage = toLocalImage(image) match {
          case Right(x) ⇒ x
          case Left(x)  ⇒ throw new UserBadDataError(x.msg)
        },
        command = command,
        installCommands = installCommands
      )

    fromUDocker(uDocker)
  }

  def fromUDocker(uDocker: UDockerArguments)(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference): UDockerTask =
    new UDockerTask(
      uDocker = uDocker,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      _config = InputOutputConfig(),
      external = External()
    )

  def layersDirectory(workspace: Workspace) = workspace.persistentDir /> "udocker" /> "layers"

  def repositoriesDirectory(workspace: Workspace) = workspace.persistentDir /> "udocker" /> "repos"

}

@Lenses case class UDockerTask(
  uDocker:            UDockerArguments,
  errorOnReturnValue: Boolean,
  returnValue:        Option[Val[Int]],
  stdOut:             Option[Val[String]],
  stdErr:             Option[Val[String]],
  _config:            InputOutputConfig,
  external:           External
) extends Task with ValidateTask { self ⇒
  override def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)
  override def validate =
    container.validateContainer(Vector(uDocker.command), uDocker.environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒

    import parameters._
    val (uDockerExecutable, uDockerInstallDirectory, uDockerTarball) = UDocker.install(executionContext, executionContext.tmpDirectory)
    UDocker.buildRepoV2(uDocker.localDockerImage)(executionContext.workspace)

    External.withWorkDir(executionContext) { taskWorkDirectory ⇒
      taskWorkDirectory.mkdirs()

      val containersDirectory =
        if (uDocker.reuseContainer) executionContext.tmpDirectory /> "containers" /> uDocker.localDockerImage.id
        else taskWorkDirectory /> "containers" /> uDocker.localDockerImage.id

      def uDockerVariables = UDocker.environmentVariables(
        tmpDirectory = executionContext.tmpDirectory,
        homeDirectory = taskWorkDirectory,
        containersDirectory = containersDirectory,
        repositoryDirectory = UDockerTask.repositoriesDirectory(executionContext.workspace),
        layersDirectory = UDockerTask.layersDirectory(executionContext.workspace),
        installDirectory = uDockerInstallDirectory,
        tarball = uDockerTarball
      )

      def prepareVolumes(
        preparedFilesInfo:     Iterable[FileInfo],
        containerPathResolver: String ⇒ File,
        hostFiles:             Vector[HostFile],
        volumesInfo:           List[VolumeInfo]   = List.empty[VolumeInfo]
      ): Iterable[MountPoint] =
        preparedFilesInfo.map { case (f, d) ⇒ d.getAbsolutePath → containerPathResolver(f.expandedUserPath).toString } ++
          hostFiles.map { case (f, b) ⇒ f → b.getOrElse(f) } ++
          volumesInfo.map { case (f, d) ⇒ f.toString → d }

      val userWorkDirectoryValue = userWorkDirectory(uDocker)
      val inputDirectory = taskWorkDirectory /> "inputs"

      val context = parameters.context + (External.PWD → userWorkDirectoryValue)
      def containerPathResolver = container.inputPathResolver(File(""), userWorkDirectoryValue) _
      def inputPathResolver = container.inputPathResolver(inputDirectory, userWorkDirectoryValue) _

      val (preparedContext, preparedFilesInfo) = external.prepareAndListInputFiles(context, inputPathResolver)

      def outputPathResolver(rootDirectory: File) = container.outputPathResolver(
        preparedFilesInfo.map { case (f, d) ⇒ f.toString → d.toString },
        uDocker.hostFiles.map { case (f, b) ⇒ f.toString → b.getOrElse(f) },
        inputDirectory,
        userWorkDirectoryValue.toString,
        rootDirectory
      ) _

      val volumes = prepareVolumes(preparedFilesInfo, containerPathResolver, uDocker.hostFiles).toVector

      val imageId = s"${uDocker.localDockerImage.image}:${uDocker.localDockerImage.tag}"

      val pool =
        if (uDocker.reuseContainer) executionContext.cache.getOrElseUpdate(containerPoolKey, Pool[ContainerId](() ⇒ UDocker.newContainer(uDocker, uDockerExecutable, uDockerVariables, volumes, imageId)))
        else WithNewInstance[ContainerId](() ⇒ UDocker.newContainer(uDocker, uDockerExecutable, uDockerVariables, volumes, imageId))

      pool { runId ⇒

        def runContainer = {
          val rootDirectory = containersDirectory / runId / "ROOT"

          val containerEnvironmentVariables =
            uDocker.environmentVariables.map { case (name, variable) ⇒ name -> variable.from(preparedContext) }

          val preparedParameters = parameters.copy(context = preparedContext)

          val executionResult = executeAll(
            taskWorkDirectory,
            uDockerVariables,
            errorOnReturnValue,
            returnValue,
            stdOut,
            stdErr,
            List(
              uDockerRunCommand(
                uDocker.uDockerUser,
                containerEnvironmentVariables,
                volumes,
                userWorkDirectory(uDocker),
                uDockerExecutable,
                runId,
                uDocker.command)
            )
          )(preparedParameters)

          val retContext = external.fetchOutputFiles(outputs, preparedContext, outputPathResolver(rootDirectory), rootDirectory)
          external.cleanWorkDirectory(outputs, retContext, taskWorkDirectory)
          (retContext, executionResult)
        }

        val (retContext, executionResult) = runContainer

        retContext ++
          List(
            stdOut.map { o ⇒ Variable(o, executionResult.output.get) },
            stdErr.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
            returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
          ).flatten
      }
    }
  }

}
