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

import monocle.macros._
import cats.implicits._
import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace._
import org.openmole.core.networkservice._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.builder._
import org.openmole.core.expansion._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.systemexec._
import org.openmole.core.preference._
import org.openmole.plugin.task.container
import org.openmole.plugin.task.udocker.DockerMetadata._
import org.openmole.tool.cache._
import org.openmole.core.dsl._
import org.openmole.core.fileservice.FileService
import org.openmole.core.outputredirection.OutputRedirection
import org.openmole.core.threadprovider._
import org.openmole.plugin.task.container.HostFiles
import org.openmole.tool.lock.LockKey
import org.openmole.plugin.task.container._

import scala.language.postfixOps

object UDockerTask {

  @Lenses case class Commands(value: Vector[FromContext[String]])

  object Commands {
    implicit def fromContext(f: FromContext[String]) = Commands(Vector(f))
    implicit def fromString(f: String) = Commands(Vector(f))
    implicit def seqOfString(f: Seq[String]) = Commands(f.map(x ⇒ x: FromContext[String]).toVector)
    implicit def seqOfFromContext(f: Seq[FromContext[String]]) = Commands(f.toVector)
  }

  import UDocker._

  val RegistryTimeout = ConfigurationLocation("UDockerTask", "RegistryTimeout", Some(1 minutes))

  implicit def isTask: InputOutputBuilder[UDockerTask] = InputOutputBuilder(UDockerTask._config)
  implicit def isExternal: ExternalBuilder[UDockerTask] = ExternalBuilder(UDockerTask.external)
  implicit def isInfo = InfoBuilder(info)

  implicit def isBuilder = new ReturnValue[UDockerTask] with ErrorOnReturnValue[UDockerTask] with StdOutErr[UDockerTask] with EnvironmentVariables[UDockerTask] with HostFiles[UDockerTask] with WorkDirectory[UDockerTask] { builder ⇒
    override def returnValue = UDockerTask.returnValue
    override def errorOnReturnValue = UDockerTask.errorOnReturnValue
    override def stdOut = UDockerTask.stdOut
    override def stdErr = UDockerTask.stdErr
    override def environmentVariables = UDockerTask.uDocker composeLens UDockerArguments.environmentVariables
    override def hostFiles = UDockerTask.uDocker composeLens UDockerArguments.hostFiles
    override def workDirectory = UDockerTask.uDocker composeLens UDockerArguments.workDirectory
  }

  def apply(
    image:              ContainerImage,
    run:                Commands,
    install:            Seq[String]                   = Vector.empty,
    user:               OptionalArgument[String]      = None,
    mode:               OptionalArgument[String]      = None,
    reuseContainer:     Boolean                       = true,
    cacheInstall:       Boolean                       = true,
    forceUpdate:        Boolean                       = false,
    returnValue:        OptionalArgument[Val[Int]]    = None,
    stdOut:             OptionalArgument[Val[String]] = None,
    stdErr:             OptionalArgument[Val[String]] = None,
    errorOnReturnValue: Boolean                       = true
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: NewFile, workspace: Workspace, preference: Preference, threadProvider: ThreadProvider, fileService: FileService, outputRedirection: OutputRedirection, networkService: NetworkService): UDockerTask = {

    def blockChars(s: String): String = {
      val blocked = Set(''', '"', '\\')
      s.flatMap { c ⇒ if (blocked.contains(c)) s"\\$c" else s"$c" }
    }

    UDockerTask(
      uDocker = createUDocker(image, install, user, mode, cacheInstall, forceUpdate, reuseContainer),
      commands = Commands.value.modify(_.map(_.map(blockChars)))(run),
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      stdOut = stdOut,
      stdErr = stdErr,
      _config = InputOutputConfig(),
      external = External(),
      info = InfoConfig()
    )
  }

  def createUDocker(
    image:          ContainerImage,
    install:        Seq[String]              = Vector.empty,
    user:           OptionalArgument[String] = None,
    mode:           OptionalArgument[String] = None,
    cacheInstall:   Boolean                  = true,
    forceUpdate:    Boolean                  = false,
    reuseContainer: Boolean                  = true)(implicit newFile: NewFile, preference: Preference, threadProvider: ThreadProvider, workspace: Workspace, fileService: FileService, outputRedirection: OutputRedirection, networkService: NetworkService) = {
    val uDocker =
      UDockerArguments(
        localDockerImage = toLocalImage(image) match {
          case Right(x) ⇒ x
          case Left(x)  ⇒ throw new UserBadDataError(x.msg)
        },
        mode = mode orElse Some("P1"),
        reuseContainer = reuseContainer,
        user = user)

    installLibraries(uDocker, install, cacheInstall, forceUpdate)

  }

  def installLibraries(uDocker: UDockerArguments, installCommands: Seq[String], cacheInstall: Boolean, forceUpdate: Boolean)(implicit newFile: NewFile, workspace: Workspace, fileService: FileService, outputRedirection: OutputRedirection, networkService: NetworkService) = {
    def installLibrariesInContainer(destination: File) =
      newFile.withTmpFile { tmpDirectory ⇒
        val layersDirectory = UDockerTask.layersDirectory(workspace)
        val repositoryDirectory = UDockerTask.repositoryDirectory(workspace)
        UDocker.buildRepoV2(repositoryDirectory, layersDirectory, uDocker.localDockerImage)

        val (uDockerExecutable, uDockerInstallDirectory, uDockerTarball) = UDocker.install(tmpDirectory)

        val containersDirectory = tmpDirectory / "containers"

        def uDockerVariables = UDocker.environmentVariables(
          tmpDirectory = tmpDirectory,
          homeDirectory = tmpDirectory,
          containersDirectory = containersDirectory,
          repositoryDirectory = repositoryDirectory,
          layersDirectory = layersDirectory,
          installDirectory = uDockerInstallDirectory,
          tarball = uDockerTarball
        )

        val container = UDocker.createContainer(uDocker, uDockerExecutable, containersDirectory, uDockerVariables, Vector.empty, imageId(uDocker))

        def httpProxyVars: Seq[(String, String)] =
          networkService.httpProxy match {
            case Some(proxy) ⇒
              Seq(
                ("http_proxy", NetworkService.HttpHost.toString(proxy)),
                ("https_proxy", NetworkService.HttpHost.toString(proxy)))
            case _ ⇒
              Seq()
          }

        runCommands(
          uDocker,
          uDockerExecutable,
          uDockerVariables,
          uDockerVolumes = Vector.empty,
          container = container,
          commands = installCommands,
          environmentVariables = httpProxyVars,
          stdOut = outputRedirection.output,
          stdErr = outputRedirection.output
        )

        if (forceUpdate) destination.recursiveDelete
        (containersDirectory / container) move destination
      }

    def installedUDockerContainer() =
      if (installCommands.isEmpty) uDocker
      else {
        if (cacheInstall) {
          import org.openmole.tool.hash._
          val cacheKey: String = hashString((uDocker.localDockerImage.content.layers.map(_._1.digest) ++ installCommands).mkString("\n")).toString
          val cacheDirectory = installCacheDirectory(workspace)
          cacheDirectory.withLockInDirectory {
            val cachedContainer = cacheDirectory / cacheKey
            if (forceUpdate || !cachedContainer.exists()) installLibrariesInContainer(cachedContainer)
            (UDockerArguments.localDockerImage composeLens LocalDockerImage.container) set Some(cachedContainer) apply uDocker
          }
        }
        else {
          val createdContainer = newFile.newDir("container")
          installLibrariesInContainer(createdContainer)
          fileService.deleteWhenGarbageCollected(createdContainer)
          (UDockerArguments.localDockerImage composeLens LocalDockerImage.container) set Some(createdContainer) apply uDocker
        }
      }

    installedUDockerContainer()
  }

  def toLocalImage(containerImage: ContainerImage)(implicit preference: Preference, newFile: NewFile, workspace: Workspace, threadProvider: ThreadProvider, outputRedirection: OutputRedirection, networkservice: NetworkService): Either[Err, LocalDockerImage] =
    containerImage match {
      case i: DockerImage      ⇒ downloadImage(i, manifestDirectory(workspace), layersDirectory(workspace), preference(RegistryTimeout))
      case i: SavedDockerImage ⇒ loadImage(i)
    }

  def installCacheDirectory(workspace: Workspace) = workspace.persistentDir /> "udocker" /> "cached"
  def layersDirectory(workspace: Workspace) = workspace.persistentDir /> "udocker" /> "layers"
  def repositoryDirectory(workspace: Workspace) = workspace.persistentDir /> "udocker" /> "repos"
  def manifestDirectory(workspace: Workspace) = workspace.persistentDir /> "udocker" /> "manifest"

  lazy val containerPoolKey = CacheKey[WithInstance[ContainerID]]()
  lazy val installLockKey = LockKey()

  def config(
    config:      InputOutputConfig,
    returnValue: Option[Val[Int]],
    stdOut:      Option[Val[String]],
    stdErr:      Option[Val[String]]) = config.addOutput(Seq(stdOut, stdErr, returnValue).flatten: _*)

}

@Lenses case class UDockerTask(
  uDocker:            UDockerArguments,
  commands:           UDockerTask.Commands,
  errorOnReturnValue: Boolean,
  returnValue:        Option[Val[Int]],
  stdOut:             Option[Val[String]],
  stdErr:             Option[Val[String]],
  _config:            InputOutputConfig,
  external:           External,
  info:               InfoConfig
) extends Task with ValidateTask { self ⇒

  override def config = UDockerTask.config(_config, returnValue, stdOut, stdErr)
  override def validate = container.validateContainer(commands.value, uDocker.environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    import parameters._

    val (uDockerExecutable, uDockerInstallDirectory, uDockerTarball) =
      executionContext.lockRepository.withLock(UDockerTask.installLockKey) {
        UDocker.install(executionContext.tmpDirectory)
      }

    val layersDirectory = UDockerTask.layersDirectory(executionContext.workspace)
    val repositoryDirectory = UDockerTask.repositoryDirectory(executionContext.workspace)
    UDocker.buildRepoV2(repositoryDirectory, layersDirectory, uDocker.localDockerImage)

    External.withWorkDir(executionContext) { taskWorkDirectory ⇒
      taskWorkDirectory.mkdirs()

      val containersDirectory =
        if (uDocker.reuseContainer) executionContext.tmpDirectory /> "containers" /> uDocker.localDockerImage.id
        else taskWorkDirectory /> "containers" /> uDocker.localDockerImage.id

      def uDockerVariables = UDocker.environmentVariables(
        tmpDirectory = executionContext.tmpDirectory,
        homeDirectory = taskWorkDirectory,
        containersDirectory = containersDirectory,
        repositoryDirectory = repositoryDirectory,
        layersDirectory = layersDirectory,
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
          hostFiles.map { h ⇒ h.path → h.destination } ++
          volumesInfo.map { case (f, d) ⇒ f.toString → d }

      val userWorkDirectoryValue = userWorkDirectory(uDocker)
      val inputDirectory = taskWorkDirectory /> "inputs"

      val context = parameters.context + (External.PWD → userWorkDirectoryValue)
      def containerPathResolver = container.inputPathResolver(File(""), userWorkDirectoryValue) _
      def inputPathResolver = container.inputPathResolver(inputDirectory, userWorkDirectoryValue) _

      val (preparedContext, preparedFilesInfo) = External.deployAndListInputFiles(external, context, inputPathResolver)

      def outputPathResolver(rootDirectory: File) = container.outputPathResolver(
        preparedFilesInfo.map { case (f, d) ⇒ f.toString → d.toString },
        uDocker.hostFiles.map { h ⇒ h.path → h.destination },
        inputDirectory,
        userWorkDirectoryValue.toString,
        rootDirectory
      ) _

      val volumes = prepareVolumes(preparedFilesInfo, containerPathResolver, uDocker.hostFiles).toVector

      val imageId = s"${uDocker.localDockerImage.image}:${uDocker.localDockerImage.tag}"

      val pool =
        if (uDocker.reuseContainer) executionContext.cache.getOrElseUpdate(UDockerTask.containerPoolKey, Pool[ContainerId](() ⇒ UDocker.createContainer(uDocker, uDockerExecutable, containersDirectory, uDockerVariables, volumes, imageId)))
        else WithNewInstance[ContainerId](() ⇒ UDocker.createContainer(uDocker, uDockerExecutable, containersDirectory, uDockerVariables, volumes, imageId))

      pool { runId ⇒

        def runContainer = {
          val rootDirectory = containersDirectory / runId / "ROOT"

          val containerEnvironmentVariables =
            uDocker.environmentVariables.map { case (name, variable) ⇒ name -> variable.from(preparedContext) }

          def expandCommand(command: FromContext[String]) =
            ExecutionCommand.Raw(UDocker.uDockerRunCommand(
              uDocker.user,
              containerEnvironmentVariables,
              volumes,
              userWorkDirectory(uDocker),
              uDockerExecutable,
              runId,
              command.from(preparedContext)))

          val executionResult = executeAll(
            taskWorkDirectory,
            uDockerVariables,
            commands.value.map(expandCommand).toList,
            errorOnReturnValue && !returnValue.isDefined,
            stdOut.isDefined,
            stdErr.isDefined,
            stdOut = executionContext.outputRedirection.output,
            stdErr = executionContext.outputRedirection.output
          )

          val retContext = External.fetchOutputFiles(external, outputs, preparedContext, outputPathResolver(rootDirectory), rootDirectory)
          External.cleanWorkDirectory(outputs, retContext, taskWorkDirectory)
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
