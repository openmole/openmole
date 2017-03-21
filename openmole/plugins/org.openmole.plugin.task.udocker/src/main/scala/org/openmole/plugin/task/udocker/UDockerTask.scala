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

  implicit def isBuilder = new ReturnValue[UDockerTask] with ErrorOnReturnValue[UDockerTask] with StdOutErr[UDockerTask] with EnvironmentVariables[UDockerTask] with HostFiles[UDockerTask] with ReuseContainer[UDockerTask] { builder ⇒
    override def environmentVariables = UDockerTask.environmentVariables
    override def returnValue = UDockerTask.returnValue
    override def errorOnReturnValue = UDockerTask.errorOnReturnValue
    override def stdOut = UDockerTask.stdOut
    override def stdErr = UDockerTask.stdErr
    override def hostFiles = UDockerTask.hostFiles
    override def reuseContainer = UDockerTask.reuseContainer
  }

  def apply(
    image:   ContainerImage,
    command: FromContext[String]
  )(implicit name: sourcecode.Name): UDockerTask =
    new UDockerTask(
      image = image,
      command = command,
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

  override protected def process(ctx: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = External.withWorkDir(executionContext) { taskWorkDirectory ⇒

    taskWorkDirectory.mkdirs()

    val inputDirectory = taskWorkDirectory /> "inputs"

    def subDirectory(name: String) = taskWorkDirectory /> name

    // FIXME containersDirectory might actually be imagesDirectory
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

    // FIXME get rid of it
    def basePathResolver(baseDirectory: String)(path: String) =
      if (File(path).isAbsolute) path
      else (File(baseDirectory) / path).getPath

    def executeWithContainerReuse = {

      val containersDirectory = executionContext.tmpDirectory /> "containers"
      val udockerVariables = setContainerPaths(containersDirectory)

      def containerExists(name: String) = Workspace.withTmpDir { tmpDirectory ⇒
        val commandline = commandLine(s"${udocker.getAbsolutePath} ps", tmpDirectory.getAbsolutePath, Context.empty)(RandomProvider.empty)
        val result = execute(commandline, tmpDirectory, udockerVariables, returnOutput = true, returnError = true)
        result.output.get.lines.exists(_.contains(name))
      }

      def createContainer(imageId: String, name: String) =
        containersDirectory.withLockInDirectory {
          if (!containerExists(name))
            Workspace.withTmpDir { tmpDirectory ⇒
              val commandline = commandLine(s"${udocker.getAbsolutePath} create --name=$name $imageId", tmpDirectory.getAbsolutePath, Context.empty)(RandomProvider.empty)
              execute(commandline, tmpDirectory, udockerVariables, returnOutput = false, returnError = false)
              name
            }
        }

      createContainer(pulledImageId, containerName)

      val tmpMount = "/tmp/openmole/"
      val containerTmpVolume = taskWorkDirectory /> "tmp"
      val context = ctx + (External.PWD → tmpMount)

      // FIXME get rid of it
      def containerPathResolver(path: String) = basePathResolver(tmpMount)(path)

      def inputPathResolver(path: String) = inputDirectory / basePathResolver(tmpMount)(path)

      val (preparedContext, preparedFilesInfo) = external.prepareAndListInputFiles(context, inputPathResolver)

      def volumes =
        preparedFilesInfo.map { case (f, d) ⇒ d.getAbsolutePath → containerPathResolver(f.name) } ++
          hostFiles.map { case (f, b) ⇒ f → b.getOrElse(f) } ++
          List(containerTmpVolume → tmpMount)

      def runCommand: FromContext[String] = {
        val variablesArgument =
          (environmentVariables ++ List("HOME" → FromContext.value(tmpMount))).map { case (name, variable) ⇒ s"""-e $name="${variable.from(context)}"""" }.mkString(" ")

        def volumesArgument = volumes.map { case (host, container) ⇒ s"""-v "$host":"$container"""" }.mkString(" ")

        command.map(cmd ⇒ s"""${udocker.getAbsolutePath} run --workdir="$tmpMount" $variablesArgument $volumesArgument $containerName $cmd""")
      }

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

      val rootDirectory = containersDirectory / containerName / "ROOT"

      def outputPathResolver(filePath: String): File =
        if (File(filePath).isAbsolute) rootDirectory / filePath
        else containerTmpVolume / filePath

      val retContext = external.fetchOutputFiles(preparedContext, outputPathResolver)
      external.checkAndClean(this, retContext, taskWorkDirectory)
      (retContext, executionResult)
    }

    def executeWithNewContainer = {
      val containersDirectory = taskWorkDirectory /> "containers"
      val udockerVariables = setContainerPaths(containersDirectory)

      val dockerWorkDirectory = {

        import org.json4s._
        import org.json4s.jackson.JsonMethods._

        val cmd = commandLine(s"${udocker.getAbsolutePath} inspect $pulledImageId", taskWorkDirectory.getAbsolutePath, ctx)
        val result = execute(cmd, taskWorkDirectory, udockerVariables, returnOutput = true, returnError = true)
        implicit def format = DefaultFormats

        result.output.flatMap {
          o ⇒ (parse(o) \ "config" \ "WorkingDir").extractOpt[String]
        }
      }

      def workDirectory =
        dockerWorkDirectory map {
          case "" ⇒ "/"
          case d  ⇒ d
        } getOrElse ("/")

      val context = ctx + (External.PWD → workDirectory)

      def containerPathResolver(path: String) = basePathResolver(workDirectory)(path)

      def inputPathResolver(path: String) = inputDirectory / basePathResolver(workDirectory)(path)

      val (preparedContext, preparedFilesInfo) = external.prepareAndListInputFiles(context, inputPathResolver)

      def volumes =
        preparedFilesInfo.map { case (f, d) ⇒ d.getAbsolutePath → containerPathResolver(f.name) } ++
          hostFiles.map { case (f, b) ⇒ f → b.getOrElse(f) }

      def runCommand: FromContext[String] = {
        val variablesArgument =
          environmentVariables.map { case (name, variable) ⇒ s"""-e $name="${variable.from(context)}"""" }.mkString(" ")
        def volumesArgument = volumes.map { case (host, container) ⇒ s"""-v "$host":"$container"""" }.mkString(" ")
        command.map(cmd ⇒ s"""${udocker.getAbsolutePath} run $variablesArgument $volumesArgument $pulledImageId $cmd""")
      }

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

      val rootDirectory = containersDirectory.listFiles().head / "ROOT"

      // FIXME check the case when output file is in volume (input or host file)
      def outputPathResolver(filePath: String): File =
        if (File(filePath).isAbsolute) rootDirectory / filePath
        else rootDirectory / workDirectory / filePath

      val retContext = external.fetchOutputFiles(preparedContext, outputPathResolver)
      external.checkAndClean(this, retContext, taskWorkDirectory)
      (retContext, executionResult)
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

  def udockerRepoVariables(tmpDirectory: File, loglevel: Int = 1) =
    Vector(
      "UDOCKER_DIR" → udockerInstallTmpDirectory.getAbsolutePath,
      "UDOCKER_TMPDIR" → tmpDirectory.getAbsolutePath,
      "UDOCKER_REPOS" → repoDirectory.getAbsolutePath,
      "UDOCKER_LAYERS" → layersDirectory.getAbsolutePath,
      "UDOCKER_TARBALL" → udockerTarBall.getAbsolutePath,
      "UDOCKER_LOGLEVEL" → loglevel.toString
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
          val result = execute(commandline, tmpDirectory, udockerRepoVariables(tmpDirectory), returnOutput = false, returnError = false)
          val output = result.output.get
          output.lines.toSeq.last
        }
      case image: DockerImage ⇒
        Workspace.withTmpDir { tmpDirectory ⇒
          repoDirectory.withLockInDirectory {
            val commandline = commandLine(s"${udocker.getAbsolutePath} pull --registry ${image.registry} ${image.image}", tmpDirectory.getAbsolutePath, Context.empty)(RandomProvider.empty)
            execute(commandline, tmpDirectory, udockerRepoVariables(tmpDirectory, loglevel = 0), returnOutput = false, returnError = false)
            image.image
          }
        }
    }
    imageId
  }

}
