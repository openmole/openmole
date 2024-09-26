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
import org.openmole.core.setter.{DefinitionScope, InfoBuilder, InfoConfig, InputOutputBuilder, InputOutputConfig}
import org.openmole.core.workflow.task.{Task, TaskExecutionContext}
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.container.ContainerTask.{Commands, downloadImage, extractImage, repositoryDirectory}
import org.openmole.plugin.task.external.*
import org.openmole.tool.cache.{CacheKey, WithInstance}
import org.openmole.tool.hash.hashString
import org.openmole.tool.lock.*
import org.openmole.tool.outputredirection.*
import org.openmole.tool.stream.*

object ContainerTask:

  given InputOutputBuilder[ContainerTask] = InputOutputBuilder(Focus[ContainerTask](_.config))
  given ExternalBuilder[ContainerTask] = ExternalBuilder(Focus[ContainerTask](_.external))
  given InfoBuilder[ContainerTask] = InfoBuilder(Focus[ContainerTask](_.info))

  val RegistryTimeout = PreferenceLocation("ContainerTask", "RegistryTimeout", Some(1 minutes))
  val RegistryRetryOnError = PreferenceLocation("ContainerTask", "RegistryRetryOnError", Some(5))

  lazy val installLockKey = LockKey()

  case class Commands(value: Vector[FromContext[String]])

  object Commands:
    implicit def fromContext(f: FromContext[String]): Commands = Commands(Vector(f))
    implicit def fromString(f: String): Commands = Commands(Vector(f))
    implicit def seqOfString(f: Seq[String]): Commands = Commands(f.map(x ⇒ x: FromContext[String]).toVector)
    implicit def seqOfFromContext(f: Seq[FromContext[String]]): Commands = Commands(f.toVector)

  def extractImage(image: SavedDockerImage, directory: File): _root_.container.SavedImage =
    import _root_.container._
    ImageBuilder.extractImage(image.file, directory, compressed = image.compressed)

  def downloadImage(image: DockerImage, repository: File)(implicit preference: Preference, threadProvider: ThreadProvider, networkService: NetworkService): _root_.container.SavedImage =
    import _root_.container._

    ImageDownloader.downloadContainerImage(
      DockerImage.toRegistryImage(image),
      repository,
      timeout = preference(RegistryTimeout),
      retry = Some(preference(RegistryRetryOnError)),
      executor = ImageDownloader.Executor.parallel(threadProvider.pool),
      proxy = networkService.httpProxy.map(p ⇒ ImageDownloader.HttpProxy(p.hostURI))
    )

  def repositoryDirectory(workspace: Workspace) = workspace.persistentDir /> "container" /> "repos"

  def runCommandInContainer(
    containerSystem:      ContainerSystem,
    image:                _root_.container.FlatImage,
    commands:             Seq[String],
    volumes:              Seq[(String, String)]      = Seq.empty,
    environmentVariables: Seq[(String, String)]      = Seq.empty,
    workDirectory:        Option[String]             = None,
    output:               PrintStream,
    error:                PrintStream)(implicit tmpDirectory: TmpDirectory, networkService: NetworkService) =

    val retCode =
      containerSystem match
        case Proot(proot, noSeccomp, kernel) ⇒
          tmpDirectory.withTmpDir: directory ⇒
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
        case Singularity(command) ⇒
          tmpDirectory.withTmpDir: directory ⇒
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


    retCode

  def apply(
             image:                  ContainerImage,
             command:                Commands,
             containerSystem:        ContainerSystem                                    = ContainerSystem.default,
             installContainerSystem: ContainerSystem                                    = ContainerSystem.default,
             install:                Seq[String]                                        = Vector.empty,
             installFiles:           Seq[File]                                          = Vector.empty,
             workDirectory:          OptionalArgument[String]                           = None,
             relativePathRoot:       OptionalArgument[String]                           = None,
             hostFiles:              Seq[HostFile]                                      = Vector.empty,
             duplicatedDirectories:  Seq[String]                                        = Vector.empty,
             environmentVariables:   Seq[EnvironmentVariable]                           = Vector.empty,
             errorOnReturnValue:     Boolean                                            = true,
             returnValue:            OptionalArgument[Val[Int]]                         = None,
             stdOut:                 OptionalArgument[Val[String]]                      = None,
             stdErr:                 OptionalArgument[Val[String]]                      = None,
             duplicateImage:         Boolean                                            = true,
             reuseContainer:         Boolean                                            = true,
             clearCache:             Boolean                                            = false,
             containerPoolKey:       CacheKey[WithInstance[Pooled]] = CacheKey())(using sourcecode.Name, DefinitionScope, TmpDirectory, NetworkService, Workspace, ThreadProvider, Preference, OutputRedirection, SerializerService, FileService) =
    new ContainerTask(
      containerSystem,
      ContainerTask.install(installContainerSystem, image, install, volumes = installFiles.map(f => f -> f.getName), clearCache = clearCache),
      command,
      workDirectory = workDirectory.option,
      relativePathRoot = relativePathRoot,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue.option,
      hostFiles = hostFiles,
      duplicatedDirectories = duplicatedDirectories,
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

  def install(containerSystem: ContainerSystem, image: ContainerImage, install: Seq[String], volumes: Seq[(File, String)] = Seq.empty, errorDetail: Int ⇒ Option[String] = _ ⇒ None, clearCache: Boolean = false)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, workspace: Workspace, fileService: FileService) =
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

  def executeInstall(containerSystem: ContainerSystem, image: _root_.container.FlatImage, install: Seq[String], volumes: Seq[(File, String)], errorDetail: Int ⇒ Option[String])(implicit tmpDirectory: TmpDirectory, outputRedirection: OutputRedirection, networkService: NetworkService) =
    if install.isEmpty
    then image
    else
      val retCode = runCommandInContainer(containerSystem, image, install, output = outputRedirection.output, error = outputRedirection.error, volumes = volumes.map((f, n) => f.getAbsolutePath -> n))
      if (retCode != 0) throw new UserBadDataError(s"Process exited a non 0 return code ($retCode)" + errorDetail(retCode).map(m ⇒ s": $m").getOrElse(""))
      image

  def localImage(image: ContainerImage, containerDirectory: File, clearCache: Boolean)(implicit networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, tmpDirectory: TmpDirectory) =
    image match
      case image: DockerImage ⇒
        if (clearCache) _root_.container.ImageDownloader.imageDirectory(repositoryDirectory(workspace), DockerImage.toRegistryImage(image)).recursiveDelete
        val savedImage = downloadImage(image, repositoryDirectory(workspace))
        _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)
      case image: SavedDockerImage ⇒
        tmpDirectory.withTmpDir: imageDirectory ⇒
          val savedImage = extractImage(image, imageDirectory)
          _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)


  def newCacheKey = CacheKey[WithInstance[Pooled]]()

  type FileInfo = (External.DeployedFile, File)
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String


  def isolatedWorkdirectory(executionContext: TaskExecutionContext)(
    containerSystem: ContainerSystem,
    image: InstalledImage,
    command: Commands,
    workDirectory: String,
    environmentVariables: Seq[EnvironmentVariable],
    errorOnReturnValue: Boolean,
    returnValue: Option[Val[Int]],
    stdOut: Option[Val[String]],
    stdErr: Option[Val[String]],
    external: External,
    info: InfoConfig,
    config: InputOutputConfig,
    containerPoolKey: CacheKey[WithInstance[Pooled]],
    hostFiles: Seq[HostFile] = Seq(),
    relativePathRoot: Option[String] = None) =
    val workDirectoryFile = executionContext.taskExecutionDirectory.newDirectory("workdirectory", create = true)

    ContainerTask.internal(
      containerSystem = containerSystem,
      image = image,
      command = command,
      workDirectory = Some(workDirectory),
      relativePathRoot = relativePathRoot,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      duplicateImage = false,
      stdOut = stdOut,
      stdErr = stdErr,
      config = config,
      external = external,
      info = info,
      containerPoolKey = containerPoolKey
    ) set (
      resources += (workDirectoryFile, workDirectory, head = true)
    )

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
    containerPoolKey: CacheKey[WithInstance[Pooled]],
    hostFiles: Seq[HostFile] = Seq(),
    duplicatedDirectories:  Seq[String] = Seq(),
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
      duplicatedDirectories = duplicatedDirectories,
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

  case class Pooled(image: InstalledImage, isolatedDirectories: Seq[VolumeInfo])

import ContainerTask._

case class ContainerTask(
    containerSystem:        ContainerSystem,
    image:                  InstalledImage,
    command:                Commands,
    workDirectory:          Option[String],
    relativePathRoot:       Option[String],
    hostFiles:              Seq[HostFile],
    duplicatedDirectories:  Seq[String],
    environmentVariables:   Seq[EnvironmentVariable],
    errorOnReturnValue:     Boolean,
    returnValue:            Option[Val[Int]],
    stdOut:                 Option[Val[String]],
    stdErr:                 Option[Val[String]],
    duplicateImage:         Boolean,
    reuseContainer:         Boolean,
    config:                 InputOutputConfig,
    external:               External,
    info:                   InfoConfig,
    containerPoolKey:       CacheKey[WithInstance[Pooled]]) extends Task with ValidateTask { self ⇒

  def validate = validateContainer(command.value, environmentVariables, external)

  override def process(executionContext: TaskExecutionContext) = FromContext[Context]: parameters ⇒
    import parameters._
    import executionContext.networkService

    def workDirectoryValue(image: InstalledImage) = workDirectory.orElse(image.workDirectory.filter(_.trim.nonEmpty)).getOrElse("/")
    def relativeWorkDirectoryValue(image: InstalledImage) = relativePathRoot.getOrElse(workDirectoryValue(image))
    def rootDirectory(image: InstalledImage) = image.file / _root_.container.FlatImage.rootfsName


    def containerPathResolver(image: InstalledImage, path: String): File =
      val rootDirectory = File("/")
      if File(path).isAbsolute
      then rootDirectory / path
      else rootDirectory / relativeWorkDirectoryValue(image) / path

    def createPool =
      executionContext.remote match
        case Some(r) if r.threads == 1 ⇒  WithInstance[ContainerTask.Pooled](pooled = false) { () ⇒ ContainerTask.Pooled(image, Seq()) }
        case _ ⇒
            WithInstance[ContainerTask.Pooled](pooled = reuseContainer): () ⇒
              val pooledImage =
                if duplicateImage && duplicatedDirectories.isEmpty
                then
                  val containerDirectory = executionContext.moleExecutionDirectory.newDirectory("container")
                  _root_.container.ImageBuilder.duplicateFlatImage(image, containerDirectory)
                else image

              val isolatedDirectoryMapping =
                duplicatedDirectories.map: d =>
                  val isolatedDirectory = executionContext.moleExecutionDirectory.newDirectory("isolated")
                  val containerPathValue = containerPathResolver(image, d).getPath
                  val containerPath = rootDirectory(image) / containerPathValue

                  if containerPath.exists()
                  then containerPath.copy(isolatedDirectory)
                  else isolatedDirectory.mkdirs()

                  isolatedDirectory -> containerPathValue

              ContainerTask.Pooled(image = pooledImage, isolatedDirectories = isolatedDirectoryMapping)

    val pool = executionContext.cache.getOrElseUpdate(containerPoolKey)(createPool)

    val outBuilder = new StringOutputStream
    val errBuilder = new StringOutputStream

    val tailSize = 10000
    val tailBuilder = new StringOutputStream(maxCharacters = Some(tailSize))

    val out: PrintStream =
      if stdOut.isDefined
      then new PrintStream(MultiplexedOutputStream(outBuilder, executionContext.outputRedirection.output, tailBuilder))
      else new PrintStream(MultiplexedOutputStream(executionContext.outputRedirection.output, tailBuilder))

    val err: PrintStream =
      if stdErr.isDefined
      then new PrintStream(MultiplexedOutputStream(errBuilder, executionContext.outputRedirection.error, tailBuilder))
      else new PrintStream(MultiplexedOutputStream(executionContext.outputRedirection.error, tailBuilder))

    def prepareVolumes(
      preparedFilesInfo:        Iterable[FileInfo],
      containerPathResolver:    String ⇒ File,
      hostFiles:                Seq[HostFile],
      volumesInfo:              Seq[VolumeInfo]): Iterable[MountPoint] =
      val volumes =
        volumesInfo.map((f, d) ⇒ f.toString -> d) ++
          hostFiles.map(h ⇒ h.path -> h.destination) ++
          preparedFilesInfo.map((f, d) ⇒ d.getAbsolutePath -> containerPathResolver(f.expandedUserPath).toString)

      volumes.sortBy((_, bind) => bind.split("/").length)

    pool: container ⇒
      def uniquePathResolver(path: String): File =
        import org.openmole.tool.hash.*
        executionContext.taskExecutionDirectory /> path.hash().toString / path

      val (preparedContext, preparedFilesInfo) = External.deployAndListInputFiles(external, context, uniquePathResolver)

      val volumes =
        prepareVolumes(
          preparedFilesInfo,
          containerPathResolver(container.image, _),
          hostFiles,
          container.isolatedDirectories
        ).toVector

      val containerEnvironmentVariables =
        environmentVariables.map(v ⇒ v.name.from(preparedContext) -> v.value.from(preparedContext))

      val commandValue = command.value.map(_.from(context))

      val retCode =
        runCommandInContainer(
          containerSystem,
          image = container.image,
          commands = commandValue,
          workDirectory = Some(workDirectoryValue(container.image)),
          output = out,
          error = err,
          volumes = volumes,
          environmentVariables = containerEnvironmentVariables
        )

      if errorOnReturnValue && !returnValue.isDefined && retCode != 0
      then
        def log =
          // last line might have been truncated
          val lst = tailBuilder.toString
          if lst.size >= tailSize
          then lst.split('\n').drop(1).map(l ⇒ s"|$l").mkString("\n")
          else lst.split('\n').map(l ⇒ s"|$l").mkString("\n")

        def command = commandValue.mkString(" ; ")

        val error =
          s"""Process \"$command\" exited with an error code $retCode (it should equal 0).
             |The last lines of the standard output were:
             $log
             |You may want to check the log of the standard outputs for more information on this error.""".stripMargin

        throw new InternalProcessingError(error)


      def outputPathResolverValue =
        val mappings =
          container.isolatedDirectories.map((f, d) => f.toString -> d) ++
          hostFiles.map(h ⇒ h.path → h.destination) ++
            preparedFilesInfo.map((f, d) ⇒ d.toString -> f.expandedUserPath)

        outputPathResolver(mappings, rootDirectory(container.image), containerPathResolver(container.image, _))

      val retContext =
        External.fetchOutputFiles(
          external,
          self.outputs,
          preparedContext,
          outputPathResolverValue,
          Seq(rootDirectory(container.image), executionContext.taskExecutionDirectory) ++ container.isolatedDirectories.map(_._1)
        )

      retContext ++
        returnValue.map(v ⇒ Variable(v, retCode)) ++
        stdOut.map(v ⇒ Variable(v, outBuilder.toString)) ++
        stdErr.map(v ⇒ Variable(v, errBuilder.toString))

}
