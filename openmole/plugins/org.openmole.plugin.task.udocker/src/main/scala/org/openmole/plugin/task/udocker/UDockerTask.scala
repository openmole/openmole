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
) extends Task with ValidateTask {

  override def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)

  // TODO see whether it can factored with CARETask's
  override def validate: Seq[Throwable] = {
    val allInputs = External.PWD :: inputs.toList

    def validateArchive =
      image match {
        case SavedDockerImage(archive) if !archive.exists ⇒ Seq(new UserBadDataError(s"Cannot find specified Archive $archive in your work directory. Did you prefix the path with `workDirectory / `?"))
        case _ ⇒ Seq.empty
      }

    def validateVariables = environmentVariables.map(_._2).flatMap(_.validate(allInputs))
    command.validate(allInputs) ++ validateArchive ++ validateVariables ++ External.validate(external, allInputs)
  }

  type FileInfo = (External.ToPut, File)
  type HostFile = (String, Option[String])
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)

  override protected def process(ctx: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = External.withWorkDir(executionContext) { taskWorkDirectory ⇒

    taskWorkDirectory.mkdirs()

    val inputDirectory = taskWorkDirectory /> "inputs"

    def subDirectory(name: String) = taskWorkDirectory /> name

    /**
     * Sets environment variable according to the location of the container
     *
     * @param containersDirectory Directory storing the container on disk
     * @return Expanded set of environment variables containing location dependent entries
     */
    def setContainerPaths(containersDirectory: File) =
      Vector(
        "HOME" → taskWorkDirectory.getAbsolutePath,
        "UDOCKER_CONTAINERS" → containersDirectory.getAbsolutePath
      ) ++ udockerRepoVariables(subDirectory("tmpdir"))

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

    val containersDirectory = executionContext.tmpDirectory /> "containers"
    val udockerVariables = setContainerPaths(containersDirectory)

    lazy val dockerWorkDirectory = {

      import org.json4s._
      import org.json4s.jackson.JsonMethods._

      val cmd = commandLine(s"${udocker.getAbsolutePath} inspect $pulledImageId", taskWorkDirectory.getAbsolutePath, ctx)
      val result = execute(cmd, taskWorkDirectory, udockerVariables, returnOutput = true, returnError = false)
      implicit def format = DefaultFormats

      result.output.flatMap {
        o ⇒ (parse(o) \ "config" \ "WorkingDir").extractOpt[String]
      }
    }

    val userWorkDirectory = workDirectory.getOrElse(
      dockerWorkDirectory map {
        case "" ⇒ "/"
        case d  ⇒ d
      } getOrElse ("/")
    )

    val context = ctx + (External.PWD → userWorkDirectory)

    def containerPathResolver = container.inputPathResolver(File(""), userWorkDirectory) _

    def inputPathResolver = container.inputPathResolver(inputDirectory, userWorkDirectory) _

    val (preparedContext, preparedFilesInfo) = external.prepareAndListInputFiles(context, inputPathResolver)

    def outputPathResolver(rootDirectory: File) = container.outputPathResolver(
      preparedFilesInfo.map { case (f, d) ⇒ f.toString → d.toString },
      hostFiles.map { case (f, b) ⇒ f.toString → b.getOrElse(f) },
      inputDirectory,
      userWorkDirectory.toString,
      rootDirectory
    ) _

    val volumes = prepareVolumes(preparedFilesInfo, containerPathResolver, hostFiles)

    // location of the root directory HAS TO BE Call By Name to delay evaluation until directory is actually populated
    def runContainer(runCommand: FromContext[String], rootDirectory: ⇒ File) = {
      val executionResult = executeAll(
        taskWorkDirectory,
        udockerVariables,
        errorOnReturnValue,
        returnValue,
        stdOut,
        stdErr,
        context,
        List(runCommand)
      )

      val retContext = external.fetchOutputFiles(preparedContext, outputPathResolver(rootDirectory))

      external.checkAndClean(this, retContext, taskWorkDirectory)
      (retContext, executionResult)
    }

    def executeWithContainerReuse = {

      def containerExists(name: String) = Workspace.withTmpDir { tmpDirectory ⇒
        val commandline = commandLine(s"${udocker.getAbsolutePath} ps", tmpDirectory.getAbsolutePath, Context.empty)(RandomProvider.empty)
        val result = execute(commandline, tmpDirectory, udockerVariables, returnOutput = true, returnError = false)

        result.output.get.lines.exists(_.contains(s"['$name']"))
      }

      def createContainer(imageId: String, name: String): String =
        containersDirectory.withLockInDirectory {
          if (!containerExists(name))
            Workspace.withTmpDir { tmpDirectory ⇒
              val commandline = commandLine(s"${udocker.getAbsolutePath} create --name=$name $imageId", tmpDirectory.getAbsolutePath, Context.empty)(RandomProvider.empty)
              execute(commandline, tmpDirectory, udockerVariables, returnOutput = false, returnError = false)
            }
          name
        }

      createContainer(pulledImageId, containerName)

      def runCommand: FromContext[String] = {
        val variablesArgument =
          (environmentVariables ++ List("HOME" → FromContext.value(userWorkDirectory))).map { case (name, variable) ⇒ s"""-e $name="${variable.from(context)}"""" }.mkString(" ")

        command.map(cmd ⇒ s"""${udocker.getAbsolutePath} run --workdir="$userWorkDirectory" $variablesArgument ${volumesArgument(volumes)} $containerName $cmd""")
      }

      val rootDirectory = containersDirectory / containerName / "ROOT"

      runContainer(runCommand, rootDirectory)
    }

    def executeWithNewContainer = {

      def runCommand: FromContext[String] = {
        val variablesArgument =
          environmentVariables.map { case (name, variable) ⇒ s"""-e $name="${variable.from(context)}"""" }.mkString(" ")

        // one container per run so run based on imageId
        command.map(cmd ⇒ s"""${udocker.getAbsolutePath} run $variablesArgument ${volumesArgument(volumes)} $pulledImageId $cmd""")
      }

      // lazy combined with Call By Name to delay evaluation until directory is actually populated
      lazy val rootDirectory = containersDirectory.listFiles().head / "ROOT"

      runContainer(runCommand, rootDirectory)
    }

    val (retContext, executionResult) =
      if (reuseContainer) executeWithContainerReuse else executeWithNewContainer

    retContext ++
      List(
        stdOut.map { o ⇒ Variable(o, executionResult.output.get) },
        stdErr.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
        returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
      ).flatten
  }

  lazy val containerName: String = {
    val uuid = UUID.randomUUID().toString
    uuid.filter(_ != '-').map {
      case c if c < 'a' ⇒ (c - '0' + 'g').toChar
      case c            ⇒ c
    }.takeRight(10)
  }

  def udockerDirectory = Workspace.persistentDir /> "udocker"
  def layersDirectory = udockerDirectory /> "layers"
  def repoDirectory = udockerDirectory /> "repo"
  def udockerInstallTmpDirectory = Workspace.tmpDir /> "udocker"

  def udockerRepoVariables(tmpDirectory: File, logLevel: Int = 1) =
    Vector(
      "UDOCKER_DIR" → udockerInstallTmpDirectory.getAbsolutePath,
      "UDOCKER_TMPDIR" → tmpDirectory.getAbsolutePath,
      "UDOCKER_REPOS" → repoDirectory.getAbsolutePath,
      "UDOCKER_LAYERS" → layersDirectory.getAbsolutePath,
      "UDOCKER_TARBALL" → udockerTarBall.getAbsolutePath,
      "UDOCKER_LOGLEVEL" → logLevel.toString
    )

  @volatile @transient lazy val udockerTarBall = {
    val tarball = udockerInstallTmpDirectory / "udocketarball.tar.gz"
    tarball.createNewFile()
    tarball.withLock { os ⇒ if (tarball.isEmpty) getClass.getClassLoader.getResourceAsStream("udocker.tar.gz") copy os }
    tarball
  }

  @volatile @transient lazy val udocker = {
    val destination = udockerInstallTmpDirectory / "udocker"
    destination.createNewFile()
    destination.withLock { os ⇒
      if (destination.isEmpty) {
        getClass.getClassLoader.getResourceAsStream("udocker") copy os
        destination.setExecutable(true)
      }
    }
    destination
  }

  @volatile @transient lazy val pulledImageId: String = {
    val imageId = image match {
      case SavedDockerImage(archive) ⇒
        Workspace.withTmpDir { tmpDirectory ⇒
          val commandline = commandLine(s"${udocker.getAbsolutePath} load -i ${archive.getAbsolutePath}", tmpDirectory.getAbsolutePath, Context.empty)(RandomProvider.empty)

          val result = execute(commandline, tmpDirectory, udockerRepoVariables(tmpDirectory), returnOutput = true, returnError = true, errorOnReturnValue = false)

          // retrieve and parse error as workaround to https://github.com/indigo-dc/udocker/issues/45
          val imageId = if (result.returnCode == 1) {
            // error in the form of "Error: repository and tag already exist ubuntu 16.04"
            val Pattern = ".* (\\S+) (\\S+)".r
            result.errorOutput.flatMap(_.split('\n').headOption) match {
              case Some(Pattern(repo, tag)) ⇒ Some(s"$repo:$tag")
              case _                        ⇒ None
            }
          }
          else for {
            output ← result.output
          } yield output.lines.toSeq.last

          imageId.getOrElse(throw new UserBadDataError(s"Could not retrieve image from archive ${archive.getAbsolutePath}"))
        }
      case image: DockerImage ⇒
        Workspace.withTmpDir { tmpDirectory ⇒
          repoDirectory.withLockInDirectory {
            val commandline = commandLine(s"${udocker.getAbsolutePath} pull --registry ${image.registry} ${image.image}", tmpDirectory.getAbsolutePath, Context.empty)(RandomProvider.empty)
            execute(commandline, tmpDirectory, udockerRepoVariables(tmpDirectory, logLevel = 0), returnOutput = false, returnError = false)
            image.image
          }
        }
    }
    imageId
  }

}
