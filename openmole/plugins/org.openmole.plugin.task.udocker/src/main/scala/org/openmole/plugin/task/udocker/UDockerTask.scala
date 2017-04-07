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
import org.openmole.core.workflow.builder._
import org.openmole.tool.random._
import org.openmole.tool.stream._
import org.openmole.tool.file._
import org.openmole.core.expansion._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.systemexec._
import org.openmole.core.exception._
import org.openmole.plugin.task.container
import org.openmole.tool.cache._

object UDockerTask {

  implicit def isTask: InputOutputBuilder[UDockerTask] = InputOutputBuilder(UDockerTask._config)
  implicit def isExternal: ExternalBuilder[UDockerTask] = ExternalBuilder(UDockerTask.external)

  implicit def isBuilder = new ReturnValue[UDockerTask] with ErrorOnReturnValue[UDockerTask] with StdOutErr[UDockerTask] with EnvironmentVariables[UDockerTask] with HostFiles[UDockerTask] with ReuseContainer[UDockerTask] with WorkDirectory[UDockerTask] { builder ⇒
    override def environmentVariables = UDockerTask.environmentVariables
    override def returnValue = UDockerTask.returnValue
    override def errorOnReturnValue = UDockerTask.errorOnReturnValue
    override def stdOut = UDockerTask.stdOut
    override def stdErr = UDockerTask.stdErr
    override def hostFiles = UDockerTask.hostFiles
    override def reuseContainer = UDockerTask.reuseContainer
    override def workDirectory = UDockerTask.workDirectory
  }

  def apply(
    image:   ContainerImage,
    command: FromContext[String]
  )(implicit name: sourcecode.Name): UDockerTask =
    new UDockerTask(
      image = image,
      command = command,
      workDirectory = None,
      reuseContainer = true,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      environmentVariables = Vector.empty,
      hostFiles = Vector.empty,
      _config = InputOutputConfig(),
      external = External()
    )

}

@Lenses case class UDockerTask(
    image:                ContainerImage,
    command:              FromContext[String],
    workDirectory:        Option[String],
    reuseContainer:       Boolean,
    errorOnReturnValue:   Boolean,
    returnValue:          Option[Val[Int]],
    stdOut:               Option[Val[String]],
    stdErr:               Option[Val[String]],
    environmentVariables: Vector[(String, FromContext[String])],
    hostFiles:            Vector[(String, Option[String])],
    _config:              InputOutputConfig,
    external:             External
) extends Task with ValidateTask { self ⇒
  override def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)

  def validateArchive(image: ContainerImage) = image match {
    case SavedDockerImage(archive) if !archive.exists ⇒ container.ArchiveNotFound(archive)
    case _ ⇒ container.ArchiveOK
  }

  override def validate: Seq[Throwable] = container.validateContainer(validateArchive)(image, command, environmentVariables, external, this.inputs)

  type FileInfo = (External.ToPut, File)
  type HostFile = (String, Option[String])
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)

  case class PulledImage(userWorkDirectory: String, pulledImageId: String)
  lazy val pulledImageIdKey = CacheKey[PulledImage]()

  type ContainerId = String
  lazy val containerPoolKey = CacheKey[WithInstance[ContainerId]]()

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    import parameters._

    val layersDirectory = executionContext.workspace.persistentDir /> "udocker" /> "layers"
    val repoDirectory = executionContext.tmpDirectory /> image.id /> "repo"
    val udockerInstallDirectory = executionContext.tmpDirectory /> "udocker"

    val udockerTarBall = udockerInstallDirectory / "udocketarball.tar.gz"

    if (!udockerTarBall.exists)
      udockerInstallDirectory.withLockInDirectory { if (!udockerTarBall.exists()) this.getClass.getClassLoader.getResourceAsStream("udocker.tar.gz") copy udockerTarBall }

    def udockerRepoVariables(logLevel: Int = 1) =
      Vector(
        "UDOCKER_DIR" → udockerInstallDirectory.getAbsolutePath,
        "UDOCKER_TMPDIR" → executionContext.tmpDirectory.getAbsolutePath,
        "UDOCKER_REPOS" → repoDirectory.getAbsolutePath,
        "UDOCKER_LAYERS" → layersDirectory.getAbsolutePath,
        "UDOCKER_TARBALL" → udockerTarBall.getAbsolutePath,
        "UDOCKER_LOGLEVEL" → logLevel.toString
      )

    val udocker = udockerInstallDirectory / "udocker"

    if (!udocker.exists)
      udockerInstallDirectory.withLockInDirectory {
        if (!udocker.exists()) {
          this.getClass.getClassLoader.getResourceAsStream("udocker") copy udocker
          udocker.setExecutable(true)
        }
      }

    External.withWorkDir(executionContext) { taskWorkDirectory ⇒

      taskWorkDirectory.mkdirs()

      val inputDirectory = taskWorkDirectory /> "inputs"

      def subDirectory(name: String) = taskWorkDirectory /> name

      def prepareVolumes(
        preparedFilesInfo:     Iterable[FileInfo],
        containerPathResolver: String ⇒ File,
        hostFiles:             Vector[HostFile],
        volumesInfo:           List[VolumeInfo]   = List.empty[VolumeInfo]
      ): Iterable[MountPoint] =
        preparedFilesInfo.map { case (f, d) ⇒ d.getAbsolutePath → containerPathResolver(f.name).toString } ++
          hostFiles.map { case (f, b) ⇒ f → b.getOrElse(f) } ++
          volumesInfo.map { case (f, d) ⇒ f.toString → d }

      def volumesArgument(volumes: Iterable[MountPoint]) = volumes.map { case (host, container) ⇒ s"""-v "$host":"$container"""" }.mkString(" ")

      val containersDirectory = executionContext.tmpDirectory /> image.id /> "containers"
      val udockerVariables =
        Vector(
          "HOME" → taskWorkDirectory.getAbsolutePath,
          "UDOCKER_CONTAINERS" → containersDirectory.getAbsolutePath
        ) ++ udockerRepoVariables()

      def pullImage: PulledImage = {
        val pulledImageId = repoDirectory.withLockInDirectory {
          val images = {
            val commandline = commandLine(s"${udocker.getAbsolutePath} images")
            val result = execute(commandline, executionContext.tmpDirectory, udockerRepoVariables(), returnOutput = true, returnError = true)
            result.output.get.split("\n").flatMap { l ⇒
              val trimed = l.dropRight(1).trim
              if (trimed.isEmpty) None else Some(trimed)
            }
          }

          images.headOption match {
            case Some(id) ⇒ id
            case None ⇒
              image match {
                case image: SavedDockerImage ⇒
                  val commandline = commandLine(s"${udocker.getAbsolutePath} load -i ${image.file.getAbsolutePath}")
                  val result = execute(commandline, executionContext.tmpDirectory, udockerRepoVariables(), returnOutput = true, returnError = true)
                  val imageId = result.output.map(_.lines.toSeq.last)
                  imageId.getOrElse(throw new UserBadDataError(s"Could not retrieve image from archive ${image.file}"))
                case image: DockerImage ⇒
                  val commandline = commandLine(s"${udocker.getAbsolutePath} pull --registry ${image.registry} ${image.image}")
                  execute(commandline, executionContext.tmpDirectory, udockerRepoVariables(logLevel = 0), returnOutput = true, returnError = true)
                  image.image
              }
          }
        }

        def dockerWorkDirectory = {
          import org.json4s._
          import org.json4s.jackson.JsonMethods._

          val cmd = commandLine(s"${udocker.getAbsolutePath} inspect $pulledImageId", taskWorkDirectory.getAbsolutePath)
          val result = execute(cmd.from(parameters.context), taskWorkDirectory, udockerVariables, returnOutput = true, returnError = true)

          implicit def format = DefaultFormats

          result.output.flatMap {
            o ⇒ (parse(o) \ "config" \ "WorkingDir").extractOpt[String]
          }
        }

        val userWorkDirectory =
          workDirectory.getOrElse(
            dockerWorkDirectory.map {
              case "" ⇒ "/"
              case d  ⇒ d
            } getOrElse ("/")
          )

        PulledImage(userWorkDirectory, pulledImageId)
      }

      val pulledImage = executionContext.cache.getOrElseUpdate(pulledImageIdKey, pullImage)

      def newContainer() =
        executionContext.newFile.withTmpDir { tmpDirectory ⇒
          val name = containerName(UUID.randomUUID().toString)
          val commandline = commandLine(s"${udocker.getAbsolutePath} create --name=${name} ${pulledImage.pulledImageId}")
          execute(commandline, tmpDirectory, udockerVariables, returnOutput = true, returnError = true)
          name
        }

      def deleteContainer(name: String): Unit = {
        val commandline = commandLine(s"${udocker.getAbsolutePath} delete --name $name")
        execute(commandline, executionContext.tmpDirectory, udockerRepoVariables(), returnOutput = true, returnError = true)
      }

      val pool =
        executionContext.cache.getOrElseUpdate(
          containerPoolKey,
          if (reuseContainer) Pool[ContainerId](newContainer) else WithNewInstance[ContainerId](newContainer, name ⇒ deleteContainer(name))
        )

      pool { runId ⇒

        val context = parameters.context + (External.PWD → pulledImage.userWorkDirectory)
        def containerPathResolver = container.inputPathResolver(File(""), pulledImage.userWorkDirectory) _
        def inputPathResolver = container.inputPathResolver(inputDirectory, pulledImage.userWorkDirectory) _

        val (preparedContext, preparedFilesInfo) = external.prepareAndListInputFiles(context, inputPathResolver)

        def outputPathResolver(rootDirectory: File) = container.outputPathResolver(
          preparedFilesInfo.map { case (f, d) ⇒ f.toString → d.toString },
          hostFiles.map { case (f, b) ⇒ f.toString → b.getOrElse(f) },
          inputDirectory,
          pulledImage.userWorkDirectory.toString,
          rootDirectory
        ) _

        val volumes = prepareVolumes(preparedFilesInfo, containerPathResolver, hostFiles)

        val name: String = if (!reuseContainer) containerName(UUID.randomUUID().toString) else runId

        def runContainer = {
          val rootDirectory = containersDirectory / name / "ROOT"
          def runCommand: FromContext[String] = {
            val variablesArgument = environmentVariables.map { case (name, variable) ⇒ s"""-e $name="${variable.from(context)}"""" }.mkString(" ")
            command.map(cmd ⇒ s"""${udocker.getAbsolutePath} run --workdir="${pulledImage.userWorkDirectory}" $variablesArgument ${volumesArgument(volumes)} $runId $cmd""")
          }

          val executionResult = executeAll(
            taskWorkDirectory,
            udockerVariables,
            errorOnReturnValue,
            returnValue,
            stdOut,
            stdErr,
            List(runCommand)
          )(parameters.copy(context = preparedContext))

          val retContext = external.fetchOutputFiles(preparedContext, outputPathResolver(rootDirectory))
          external.checkAndClean(this, retContext, taskWorkDirectory)
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

  def containerName(uuid: String): String = {
    uuid.filter(_ != '-').map {
      case c if c < 'a' ⇒ (c - '0' + 'g').toChar
      case c            ⇒ c
    }
  }

}
