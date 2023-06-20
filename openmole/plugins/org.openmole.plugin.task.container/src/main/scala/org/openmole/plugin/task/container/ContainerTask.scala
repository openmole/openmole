package org.openmole.plugin.task.container

/*
 * Copyright (C) 2019 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.PrintStream
import monocle.Focus
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.preference.{Preference, PreferenceLocation}
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder.{DefinitionScope, InfoBuilder, InfoConfig, InputOutputBuilder, InputOutputConfig}
import org.openmole.core.workflow.task.{Task, TaskExecutionContext}
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.container.ContainerTask.{Commands, downloadImage, extractImage, repositoryDirectory}
import org.openmole.plugin.task.external.{EnvironmentVariable, External, ExternalBuilder}
import org.openmole.tool.cache.{CacheKey, WithInstance}
import org.openmole.tool.hash.hashString
import org.openmole.tool.lock.*
import org.openmole.tool.outputredirection.*
import org.openmole.tool.stream.*

object ContainerTask:

  implicit def isTask: InputOutputBuilder[ContainerTask] = InputOutputBuilder(Focus[ContainerTask](_.config))
  implicit def isExternal: ExternalBuilder[ContainerTask] = ExternalBuilder(Focus[ContainerTask](_.external))
  implicit def isInfo: InfoBuilder[ContainerTask] = InfoBuilder(Focus[ContainerTask](_.info))

  val RegistryTimeout = PreferenceLocation("ContainerTask", "RegistryTimeout", Some(1 minutes))
  val RegistryRetryOnError = PreferenceLocation("ContainerTask", "RegistryRetryOnError", Some(5))

  lazy val installLockKey = LockKey()

  case class Commands(value: Vector[FromContext[String]])

  object Commands {
    implicit def fromContext(f: FromContext[String]): Commands = Commands(Vector(f))
    implicit def fromString(f: String): Commands = Commands(Vector(f))
    implicit def seqOfString(f: Seq[String]): Commands = Commands(f.map(x ⇒ x: FromContext[String]).toVector)
    implicit def seqOfFromContext(f: Seq[FromContext[String]]): Commands = Commands(f.toVector)
  }

  def extractImage(image: SavedDockerImage, directory: File): _root_.container.SavedImage = {
    import _root_.container._
    ImageBuilder.extractImage(image.file, directory, compressed = image.compressed)
  }

  def downloadImage(image: DockerImage, repository: File)(implicit preference: Preference, threadProvider: ThreadProvider, networkService: NetworkService): _root_.container.SavedImage = {
    import _root_.container._

    ImageDownloader.downloadContainerImage(
      DockerImage.toRegistryImage(image),
      repository,
      timeout = preference(RegistryTimeout),
      retry = Some(preference(RegistryRetryOnError)),
      executor = ImageDownloader.Executor.parallel(threadProvider.pool),
      proxy = networkService.httpProxy.map(p ⇒ ImageDownloader.HttpProxy(p.hostURI))
    )
  }

  def repositoryDirectory(workspace: Workspace) = workspace.persistentDir /> "container" /> "repos"

  def runCommandInContainer(
    containerSystem:      ContainerSystem,
    image:                _root_.container.FlatImage,
    commands:             Seq[String],
    volumes:              Seq[(String, String)]      = Seq.empty,
    environmentVariables: Seq[(String, String)]      = Seq.empty,
    workDirectory:        Option[String]             = None,
    output:               PrintStream,
    error:                PrintStream)(implicit tmpDirectory: TmpDirectory, networkService: NetworkService) = {

    val retCode =
      containerSystem match {
        case Proot(proot, noSeccomp, kernel) ⇒
          tmpDirectory.withTmpDir { directory ⇒
            _root_.container.Proot.execute(
              image,
              directory / "tmp",
              commands = commands,
              proot = proot.getAbsolutePath,
              output = output,
              error = error,
              kernel = Some(kernel),
              noSeccomp = noSeccomp,
              bind = volumes,
              environmentVariables = NetworkService.proxyVariables ++ environmentVariables,
              workDirectory = workDirectory
            )
          }
        case Singularity(command) ⇒
          tmpDirectory.withTmpDir { directory ⇒
            _root_.container.Singularity.executeFlatImage(
              image,
              directory / "tmp",
              commands = commands,
              output = output,
              error = error,
              bind = volumes,
              environmentVariables = NetworkService.proxyVariables ++ environmentVariables,
              workDirectory = workDirectory,
              singularityCommand = command,
              singularityWorkdir = Some(directory /> "singularitytmp")
            )
          }
      }

    retCode
  }

  def apply(
    image:                  ContainerImage,
    command:                Commands,
    containerSystem:        ContainerSystem                                    = ContainerSystem.default,
    installContainerSystem: ContainerSystem                                    = ContainerSystem.default,
    install:                Seq[String]                                        = Vector.empty,
    workDirectory:          OptionalArgument[String]                           = None,
    relativePathRoot:       OptionalArgument[String]                           = None,
    hostFiles:              Seq[HostFile]                                      = Vector.empty,
    environmentVariables:   Seq[EnvironmentVariable]                           = Vector.empty,
    errorOnReturnValue:     Boolean                                            = true,
    returnValue:            OptionalArgument[Val[Int]]                         = None,
    stdOut:                 OptionalArgument[Val[String]]                      = None,
    stdErr:                 OptionalArgument[Val[String]]                      = None,
    duplicateImage:         Boolean                                            = true,
    reuseContainer:         Boolean                                            = true,
    clearCache:             Boolean                                            = false,
    containerPoolKey:       CacheKey[WithInstance[InstalledImage]] = CacheKey())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, tmpDirectory: TmpDirectory, networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, outputRedirection: OutputRedirection, serializerService: SerializerService, fileService: FileService) =
    new ContainerTask(
      containerSystem,
      ContainerTask.install(installContainerSystem, image, install, clearCache = clearCache),
      command,
      workDirectory = workDirectory.option,
      relativePathRoot = relativePathRoot,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue.option,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      stdOut = stdOut.option,
      stdErr = stdErr.option,
      duplicateImage = duplicateImage,
      reuseContainer = reuseContainer,
      config = InputOutputConfig(),
      info = InfoConfig(),
      external = External(),
      containerPoolKey = containerPoolKey) set (
      outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten
    )

  def install(containerSystem: ContainerSystem, image: ContainerImage, install: Seq[String], volumes: Seq[(String, String)] = Seq.empty, errorDetail: Int ⇒ Option[String] = _ ⇒ None, clearCache: Boolean = false)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, workspace: Workspace, fileService: FileService) =
    import org.openmole.tool.hash._

    def cacheId(image: ContainerImage): Seq[String] =
      image match
        case image: DockerImage      ⇒ Seq(image.image, image.tag, image.registry)
        case image: SavedDockerImage ⇒ Seq(image.file.hash().toString)

    val volumeCacheKey = volumes.map { (f, _) ⇒ fileService.hashNoCache(f).toString } ++ volumes.map { (_, d) ⇒ d }
    val cacheKey: String = hashString((cacheId(image) ++ install ++ volumeCacheKey).mkString("\n")).toString
    val cacheDirectory = workspace.tmpDirectory /> "container" /> "cached" /> cacheKey
    val serializedFlatImage = cacheDirectory / "flatimage.bin"

    cacheDirectory.withLockInDirectory:
      val containerDirectory = cacheDirectory / "fs"

      if clearCache
      then
        serializedFlatImage.delete
        containerDirectory.recursiveDelete

      if serializedFlatImage.exists
      then serializerService.deserialize[_root_.container.FlatImage](serializedFlatImage)
      else
        val img = localImage(image, containerDirectory, clearCache = clearCache)
        val installedImage = executeInstall(containerSystem, img, install, volumes = volumes, errorDetail = errorDetail)
        serializerService.serialize(installedImage, serializedFlatImage)
        installedImage

  def executeInstall(containerSystem: ContainerSystem, image: _root_.container.FlatImage, install: Seq[String], volumes: Seq[(String, String)], errorDetail: Int ⇒ Option[String])(implicit tmpDirectory: TmpDirectory, outputRedirection: OutputRedirection, networkService: NetworkService) =
    if (install.isEmpty) image
    else {
      val retCode = runCommandInContainer(containerSystem, image, install, output = outputRedirection.output, error = outputRedirection.error, volumes = volumes)
      if (retCode != 0) throw new UserBadDataError(s"Process exited a non 0 return code ($retCode)" + errorDetail(retCode).map(m ⇒ s": $m").getOrElse(""))
      image
    }

  def localImage(image: ContainerImage, containerDirectory: File, clearCache: Boolean)(implicit networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, tmpDirectory: TmpDirectory) =
    image match {
      case image: DockerImage ⇒
        if (clearCache) _root_.container.ImageDownloader.imageDirectory(repositoryDirectory(workspace), DockerImage.toRegistryImage(image)).recursiveDelete
        val savedImage = downloadImage(image, repositoryDirectory(workspace))
        _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)
      case image: SavedDockerImage ⇒
        tmpDirectory.withTmpDir { imageDirectory ⇒
          val savedImage = extractImage(image, imageDirectory)
          _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)
        }
    }

  def newCacheKey = CacheKey[WithInstance[InstalledImage]]()

  type FileInfo = (External.DeployedFile, File)
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String

  def internal(
    containerSystem: ContainerSystem,
    image: InstalledImage,
    command: Commands,
    environmentVariables: Seq[EnvironmentVariable],
    errorOnReturnValue: Boolean,
    returnValue: Option[Val[Int]],
    stdOut: Option[Val[String]],
    stdErr: Option[Val[String]],
    external: External,
    info: InfoConfig,
    containerPoolKey: CacheKey[WithInstance[InstalledImage]],
    hostFiles: Seq[HostFile] = Seq(),
    workDirectory: Option[String] = None,
    relativePathRoot: Option[String] = None,
    duplicateImage: Boolean = true,
    reuseContainer: Boolean = true,
    config: InputOutputConfig = InputOutputConfig()) =
    ContainerTask(
      containerSystem = containerSystem,
      image = image,
      command = command,
      workDirectory = workDirectory,
      relativePathRoot = relativePathRoot,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      duplicateImage = duplicateImage,
      reuseContainer = reuseContainer,
      stdOut = stdOut,
      stdErr = stdErr,
      config = config,
      external = external,
      info = info,
      containerPoolKey = containerPoolKey
    )


import ContainerTask._

case class ContainerTask(
  containerSystem:      ContainerSystem,
  image:                InstalledImage,
  command:              Commands,
  workDirectory:        Option[String],
  relativePathRoot:     Option[String],
  hostFiles:            Seq[HostFile],
  environmentVariables: Seq[EnvironmentVariable],
  errorOnReturnValue:   Boolean,
  returnValue:          Option[Val[Int]],
  stdOut:               Option[Val[String]],
  stdErr:               Option[Val[String]],
  duplicateImage:       Boolean,
  reuseContainer:       Boolean,
  config:               InputOutputConfig,
  external:             External,
  info:                 InfoConfig,
  containerPoolKey:     CacheKey[WithInstance[InstalledImage]]) extends Task with ValidateTask { self ⇒

  def validate = validateContainer(command.value, environmentVariables, external)

  override def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    import parameters._
    import executionContext.networkService

    def noImageDuplication = WithInstance[_root_.container.FlatImage](pooled = false) { () ⇒ image }

    def createPool =
      executionContext.remote match
        case Some(r) if r.threads == 1 ⇒ noImageDuplication
        case _ ⇒
          if duplicateImage
          then
            WithInstance[_root_.container.FlatImage](close = _.file.recursiveDelete, pooled = reuseContainer): () ⇒
              val containersDirectory = executionContext.moleExecutionDirectory.newDirectory("container")
              _root_.container.ImageBuilder.duplicateFlatImage(image, containersDirectory)
          else noImageDuplication

    val pool = executionContext.cache.getOrElseUpdate(containerPoolKey, createPool)

    val outBuilder = new StringOutputStream
    val errBuilder = new StringOutputStream

    val tailSize = 10000
    val tailBuilder = new StringOutputStream(maxCharacters = Some(tailSize))

    val out: PrintStream =
      if (stdOut.isDefined) new PrintStream(MultiplexedOutputStream(outBuilder, executionContext.outputRedirection.output, tailBuilder))
      else new PrintStream(MultiplexedOutputStream(executionContext.outputRedirection.output, tailBuilder))

    val err: PrintStream =
      if (stdErr.isDefined) new PrintStream(MultiplexedOutputStream(errBuilder, executionContext.outputRedirection.error, tailBuilder))
      else new PrintStream(MultiplexedOutputStream(executionContext.outputRedirection.error, tailBuilder))

    def prepareVolumes(
      preparedFilesInfo:     Iterable[FileInfo],
      containerPathResolver: String ⇒ File,
      hostFiles:             Seq[HostFile],
      volumesInfo:           List[VolumeInfo]   = List.empty[VolumeInfo]): Iterable[MountPoint] =
      hostFiles.map { h ⇒ h.path → h.destination } ++
      preparedFilesInfo.map { (f, d) ⇒ d.getAbsolutePath → containerPathResolver(f.expandedUserPath).toString } ++
        volumesInfo.map { (f, d) ⇒ f.toString → d }

    pool { container ⇒
      val workDirectoryValue = workDirectory.orElse(container.workDirectory.filter(_.trim.nonEmpty)).getOrElse("/")
      val relativeWorkDirectoryValue = relativePathRoot.getOrElse(workDirectoryValue)
      val inputDirectory = executionContext.taskExecutionDirectory /> "inputs"

      def containerPathResolver(path: String): File =
        val rootDirectory = File("/")
        if File(path).isAbsolute
        then rootDirectory / path
        else rootDirectory / relativeWorkDirectoryValue / path

      def uniquePathResolver(path: String): File =
        import org.openmole.tool.hash.*
        executionContext.taskExecutionDirectory /> path.hash().toString / path

      val (preparedContext, preparedFilesInfo) = External.deployAndListInputFiles(external, context, uniquePathResolver)

      val volumes = prepareVolumes(preparedFilesInfo, containerPathResolver, hostFiles).toVector

      val containerEnvironmentVariables =
        environmentVariables.map { v ⇒ v.name.from(preparedContext) -> v.value.from(preparedContext) }

      val commandValue = command.value.map(_.from(context))

      val retCode =
        runCommandInContainer(
          containerSystem,
          image = container,
          commands = commandValue,
          workDirectory = Some(workDirectoryValue),
          output = out,
          error = err,
          volumes = volumes,
          environmentVariables = containerEnvironmentVariables
        )

      if (errorOnReturnValue && !returnValue.isDefined && retCode != 0) {
        def log = {
          // last line might have been truncated
          val lst = tailBuilder.toString
          if (lst.size >= tailSize) lst.split('\n').drop(1).map(l ⇒ s"|$l").mkString("\n")
          else lst.split('\n').map(l ⇒ s"|$l").mkString("\n")
        }

        def command = commandValue.mkString(" ; ")

        val error =
          s"""Process \"$command\" exited with an error code $retCode (it should equal 0).
             |The last lines of the standard output were:
             $log
             |You may want to check the log of the standard outputs for more information on this error.""".stripMargin

        throw new InternalProcessingError(error)
      }

      val rootDirectory = container.file / _root_.container.FlatImage.rootfsName

      def outputPathResolverValue =
        outputPathResolver(
          hostFiles.map { h ⇒ h.path → h.destination } ++ preparedFilesInfo.map { (f, d) ⇒ d.toString -> f.expandedUserPath },
          inputDirectory,
          relativeWorkDirectoryValue,
          rootDirectory
        ) _

      val retContext =
        External.fetchOutputFiles(external, self.outputs, preparedContext, outputPathResolverValue, Seq(rootDirectory, executionContext.taskExecutionDirectory))

      retContext ++
        returnValue.map(v ⇒ Variable(v, retCode)) ++
        stdOut.map(v ⇒ Variable(v, outBuilder.toString)) ++
        stdErr.map(v ⇒ Variable(v, errBuilder.toString))

    }
  }

}
