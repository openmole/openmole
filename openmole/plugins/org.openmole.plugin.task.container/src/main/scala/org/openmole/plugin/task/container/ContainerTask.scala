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

import monocle.Focus
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.preference.{Preference, PreferenceLocation}
import org.openmole.core.serializer.SerializerService
import org.openmole.core.setter.{DefinitionScope, InfoBuilder, InfoConfig, InputOutputBuilder, InputOutputConfig}
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.task.{Task, TaskExecutionContext}
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.external.*
import org.openmole.tool.cache.*
import org.openmole.tool.hash.hashString
import org.openmole.tool.lock.*
import org.openmole.tool.outputredirection.*
import org.openmole.tool.stream.*

import java.io.PrintStream
import java.util.UUID

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



  def apply(
    image:                  ContainerImage,
    command:                Commands,
    containerSystem:        ContainerSystem                                    = ContainerSystem.default,
    install:                Seq[String]                                        = Vector.empty,
    installFiles:           Seq[File]                                          = Vector.empty,
    workDirectory:          OptionalArgument[String]                           = None,
    relativePathRoot:       OptionalArgument[String]                           = None,
    hostFiles:              Seq[HostFile]                                      = Vector.empty,
    environmentVariables:   Seq[EnvironmentVariable]                           = Vector.empty,
    errorOnReturnValue:     Boolean                                            = true,
    returnValue:            OptionalArgument[Val[Int]]                         = None,
    stdOut:                 OptionalArgument[Val[String]]                      = None,
    stdErr:                 OptionalArgument[Val[String]]                      = None,
    clearCache:             Boolean                                            = false)(using sourcecode.Name, DefinitionScope, TmpDirectory, NetworkService, Workspace, ThreadProvider, Preference, OutputRedirection, SerializerService, FileService) =
    new ContainerTask(
      ContainerTask.install(containerSystem, image, install, volumes = installFiles.map(f => f -> f.getName), clearCache = clearCache),
      command,
      workDirectory = workDirectory.option,
      relativePathRoot = relativePathRoot,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue.option,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      stdOut = stdOut.option,
      stdErr = stdErr.option,
      config = InputOutputConfig(),
      info = InfoConfig(),
      external = External()) set (
      outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten
    )

  def install(
    containerSystem: ContainerSystem,
    image: ContainerImage,
    install: Seq[String],
    volumes: Seq[(File, String)] = Seq.empty,
    errorDetail: Int ⇒ Option[String] = _ ⇒ None,
    clearCache: Boolean = false)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, workspace: Workspace, fileService: FileService): ContainerSystem.InstalledImage =
    containerSystem match
      case containerSystem: ContainerSystem.SingularitySIF => installSIF(containerSystem, image, install, volumes, errorDetail, clearCache)
      case containerSystem: SingularityFlatImage => FlatContainerTask.install(containerSystem, image, install, volumes, errorDetail, clearCache)

  def installSIF(
    containerSystem: ContainerSystem.SingularitySIF,
    image: ContainerImage,
    install: Seq[String],
    volumes: Seq[(File, String)] = Seq.empty,
    errorDetail: Int ⇒ Option[String] = _ ⇒ None,
    clearCache: Boolean = false)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, workspace: Workspace, fileService: FileService) =

    import org.openmole.tool.hash.*

    def cacheId(image: ContainerImage): Seq[String] =
      image match
        case image: DockerImage      ⇒ Seq(image.image, image.tag, image.registry)
        case image: SavedDockerImage ⇒ Seq(image.file.hash().toString)

    val volumeCacheKey = volumes.map((f, _) ⇒ fileService.hashNoCache(f).toString) ++ volumes.map((_, d) ⇒ d)

    val cacheKey: String = hashString((cacheId(image) ++ install ++ volumeCacheKey++ Seq("sif")).mkString("\n")).toString

    val cacheDirectory = workspace.tmpDirectory /> "container" /> "cached" /> cacheKey
    val serializedSingularityImage = cacheDirectory / "singularityImage.bin"

    val installedImage =
      cacheDirectory.withLockInDirectory:
        tmpDirectory.withTmpDir: tmpDirectory =>
          val containerDirectory = tmpDirectory / "fs"
          val singularityImageFile = cacheDirectory / "image.sif"

          if clearCache
          then
            serializedSingularityImage.delete
            containerDirectory.recursiveDelete

          if serializedSingularityImage.exists
          then serializerService.deserialize[_root_.container.Singularity.SingularityImageFile](serializedSingularityImage)
          else
            val img = localImage(image, containerDirectory, clearCache = clearCache)
            val installedImage = executeInstall(img, install, volumes = volumes, errorDetail = errorDetail)
            val singularityImage = _root_.container.Singularity.buildSIF(installedImage, singularityImageFile, logger = outputRedirection.output)
            serializerService.serialize(singularityImage, serializedSingularityImage)
            singularityImage

    val initializedOverlay = initializeOverlay(containerSystem)

    ContainerSystem.InstalledSIFImage(installedImage, initializedOverlay)

  def executeInstall(image: _root_.container.FlatImage, install: Seq[String], volumes: Seq[(File, String)], errorDetail: Int ⇒ Option[String])(implicit tmpDirectory: TmpDirectory, outputRedirection: OutputRedirection, networkService: NetworkService) =
    if install.isEmpty
    then image
    else
      val retCode = runCommandInFlatImageContainer(image, install, output = outputRedirection.output, error = outputRedirection.error, volumes = volumes.map((f, n) => f.getAbsolutePath -> n))
      if (retCode != 0) throw new UserBadDataError(s"Process exited a non 0 return code ($retCode)" + errorDetail(retCode).map(m ⇒ s": $m").getOrElse(""))
      image

  def localImage(image: ContainerImage, containerDirectory: File, clearCache: Boolean)(using networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, tmpDirectory: TmpDirectory) =
    image match
      case image: DockerImage ⇒
        if clearCache then _root_.container.ImageDownloader.imageDirectory(repositoryDirectory(workspace), DockerImage.toRegistryImage(image)).recursiveDelete
        val savedImage = downloadImage(image, repositoryDirectory(workspace))
        _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)
      case image: SavedDockerImage ⇒
        tmpDirectory.withTmpDir: imageDirectory ⇒
          val savedImage = extractImage(image, imageDirectory)
          _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)

  def extractImage(image: SavedDockerImage, directory: File): _root_.container.SavedImage =
    import _root_.container.*
    ImageBuilder.extractImage(image.file, directory, compressed = image.compressed)

  def downloadImage(image: DockerImage, repository: File)(implicit preference: Preference, threadProvider: ThreadProvider, networkService: NetworkService): _root_.container.SavedImage =
    import _root_.container.*

    ImageDownloader.downloadContainerImage(
      DockerImage.toRegistryImage(image),
      repository,
      timeout = preference(RegistryTimeout),
      retry = Some(preference(RegistryRetryOnError)),
      executor = ImageDownloader.Executor.parallel(threadProvider.pool),
      proxy = networkService.httpProxy.map(p ⇒ ImageDownloader.HttpProxy(p.hostURI))
    )

  def repositoryDirectory(workspace: Workspace): File = workspace.persistentDir /> "container" /> "repos"

  def runCommandInFlatImageContainer(
    image: _root_.container.FlatImage,
    commands: Seq[String],
    volumes: Seq[(String, String)] = Seq.empty,
    environmentVariables: Seq[(String, String)] = Seq.empty,
    workDirectory: Option[String] = None,
    verbose: Boolean = false,
    output: PrintStream,
    error: PrintStream)(using tmpDirectory: TmpDirectory, networkService: NetworkService): Int =
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
        singularityWorkdir = Some(directory /> "singularitytmp"),
        verbose = verbose
      )

  def runCommandInContainer(
    image: _root_.container.Singularity.SingularityImageFile,
    overlay: Option[_root_.container.Singularity.OverlayImage] = None,
    tmpFS: Boolean = false,
    commands: Seq[String],
    volumes: Seq[(String, String)] = Seq.empty,
    environmentVariables: Seq[(String, String)] = Seq.empty,
    workDirectory: Option[String] = None,
    verbose: Boolean = false,
    output: PrintStream,
    error: PrintStream)(using tmpDirectory: TmpDirectory, networkService: NetworkService) =
    tmpDirectory.withTmpDir: directory ⇒
      _root_.container.Singularity.executeImage(
        image,
        directory / "tmp",
        overlay = overlay,
        tmpFS = tmpFS,
        commands = commands,
        output = output,
        error = error,
        bind = volumes,
        environmentVariables = NetworkService.proxyVariables ++ environmentVariables,
        workDirectory = workDirectory,
        singularityWorkdir = Some(directory /> "singularitytmp"),
        verbose = verbose
      )

  def initializeOverlay(containerSystem: ContainerSystem.SingularitySIF)(using TmpDirectory, OutputRedirection) =
    containerSystem match
      case overlay: SingularityOverlay if overlay.copy =>
        val overlayImageFile = TmpDirectory.newFile("overlay", ".img")
        val initializedOverlay = _root_.container.Singularity.createOverlay(overlayImageFile, overlay.size, output = summon[OutputRedirection].output, error = summon[OutputRedirection].error)
        overlay.copy(overlay = Some(initializedOverlay))
      case _ => containerSystem

  type FileInfo = (External.DeployedFile, File)
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String

  def internal(
    image: ContainerSystem.InstalledImage,
    command: Commands,
    environmentVariables: Seq[EnvironmentVariable],
    errorOnReturnValue: Boolean,
    returnValue: Option[Val[Int]],
    stdOut: Option[Val[String]],
    stdErr: Option[Val[String]],
    external: External,
    info: InfoConfig,
    hostFiles: Seq[HostFile] = Seq(),
    workDirectory: Option[String] = None,
    relativePathRoot: Option[String] = None,
    config: InputOutputConfig = InputOutputConfig()) =
    new ContainerTask(
      image = image,
      command = command,
      workDirectory = workDirectory,
      relativePathRoot = relativePathRoot,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      stdOut = stdOut,
      stdErr = stdErr,
      config = config,
      external = external,
      info = info
    ) set (
      outputs ++= Seq(returnValue, stdOut, stdErr).flatten
    )


  def process(
    image:                  ContainerSystem.InstalledSIFImage,
    command:                Commands,
    workDirectory:          Option[String],
    relativePathRoot:       Option[String],
    hostFiles:              Seq[HostFile],
    environmentVariables:   Seq[EnvironmentVariable],
    errorOnReturnValue:     Boolean,
    returnValue:            Option[Val[Int]],
    stdOut:                 Option[Val[String]],
    stdErr:                 Option[Val[String]],
    config:                 InputOutputConfig,
    external:               External,
    info:                   InfoConfig)(executionContext: TaskExecutionContext): FromContext[Context] =
    FromContext[Context]: parameters =>
      import executionContext.networkService
      import parameters.*

      case class OutputMapping(origin: String, resolved: File, directory: String, file: File)

      def workDirectoryValue(image: ContainerSystem.InstalledSIFImage) = workDirectory.orElse(image.image.workDirectory.filter(_.trim.nonEmpty)).getOrElse("/")

      def relativeWorkDirectoryValue(image: ContainerSystem.InstalledSIFImage) = relativePathRoot.getOrElse(workDirectoryValue(image))

      def containerPathResolver(image: ContainerSystem.InstalledSIFImage, path: String): File =
        val rootDirectory = File("/")
        if File(path).isAbsolute
        then rootDirectory / path
        else rootDirectory / relativeWorkDirectoryValue(image) / path

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
        preparedFilesInfo: Iterable[FileInfo],
        containerPathResolver: String ⇒ File,
        hostFiles: Seq[HostFile],
        volumesInfo: Seq[VolumeInfo] = Seq()): Iterable[MountPoint] =
        val volumes =
          volumesInfo.map((f, d) ⇒ f.toString -> d) ++
            hostFiles.map(h ⇒ h.path -> h.destination) ++
            preparedFilesInfo.map((f, d) ⇒ d.getAbsolutePath -> containerPathResolver(f.expandedUserPath).toString)

        volumes.sortBy((_, bind) => bind.split("/").length)

      def uniquePathResolver(path: String): File =
        import org.openmole.tool.hash.*
        executionContext.taskExecutionDirectory /> path.hash().toString / path

      val (preparedContext, preparedFilesInfo) = External.deployAndListInputFiles(external, context, uniquePathResolver)

      val volumes =
        prepareVolumes(
          preparedFilesInfo,
          containerPathResolver(image, _),
          hostFiles
        ).toVector

      val containerEnvironmentVariables =
        environmentVariables.map(v ⇒ v.name.from(preparedContext) -> v.value.from(preparedContext))

      def ignoreRetCode = !errorOnReturnValue || returnValue.isDefined
      def retCodeVariable = "_PROCESS_RET_CODE_"

      val commandValue =
        val value = command.value.map(_.from(context))
        if ignoreRetCode
        then Seq(s"${value.mkString(" && ")} ; $retCodeVariable=$$? ; true")
        else value
        
      // Prepare the copy of output files
      val resultDirectory = executionContext.moleExecutionDirectory.newDirectory("result", create = true)
      val resultDirectoryBind = "/_result_"

      val outputFileMapping =
        external.outputFiles.map: f =>
          val origin = f.origin.from(context)
          val directory = UUID.randomUUID.toString
          val resolved = containerPathResolver(image, origin)
          OutputMapping(origin, resolved, directory, resultDirectory / directory / resolved.getName)

      val copyCommand: Seq[String] =
        outputFileMapping.flatMap: m =>
          val destinationDirectory = s"$resultDirectoryBind/${m.directory}/"
          Seq(s"""mkdir -p \"$destinationDirectory\"""", s"""cp -ra \"${m.resolved}\" \"$destinationDirectory\"""")

      val exitCommand = if ignoreRetCode then Seq(s"exit $$$retCodeVariable") else Seq()

      val copyVolume = resultDirectory.getAbsolutePath -> resultDirectoryBind

      val retCode =
        image.containerSystem match
          case containerSystem: SingularityOverlay =>
            def createPool =
              WithInstance[_root_.container.Singularity.OverlayImage](pooled = containerSystem.reuse): () ⇒
                val overlay =
                  if containerSystem.reuse
                  then executionContext.moleExecutionDirectory.newFile("overlay", ".img")
                  else executionContext.taskExecutionDirectory.newFile("overlay", ".img")

                containerSystem.overlay match
                  case None => _root_.container.Singularity.createOverlay(overlay, containerSystem.size, output = out, error = err)
                  case Some(image) =>
                    if containerSystem.reuse && executionContext.localEnvironment.threads == 1
                    then image
                    else _root_.container.Singularity.copyOverlay(image, overlay)

            val overlayCache = executionContext.cache.getOrElseUpdate(containerSystem.cacheKey)(createPool)

            overlayCache: overlay =>
              runCommandInContainer(
                image = image.image,
                overlay = Some(overlay),
                commands = commandValue ++ copyCommand ++ exitCommand,
                workDirectory = Some(workDirectoryValue(image)),
                output = out,
                error = err,
                volumes = volumes ++ Seq(copyVolume),
                environmentVariables = containerEnvironmentVariables,
                verbose = containerSystem.verbose
              )

          case containerSystem: SingularityMemory =>
            runCommandInContainer(
              image = image.image,
              tmpFS = true,
              commands = commandValue ++ copyCommand ++ exitCommand,
              workDirectory = Some(workDirectoryValue(image)),
              output = out,
              error = err,
              volumes = volumes ++ Seq(copyVolume),
              environmentVariables = containerEnvironmentVariables,
              verbose = containerSystem.verbose
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

      // Set garbage collection of directories
      for
        m <- outputFileMapping
      do fileService.deleteWhenEmpty(resultDirectory / m.directory)
      fileService.deleteWhenEmpty(resultDirectory)

      val outputPathResolverValue =
        outputFileMapping.map: r =>
          r.origin -> r.file
        .toMap

      val retContext = //context
        External.fetchOutputFiles(
          external,
          config.outputs,
          preparedContext,
          outputPathResolverValue.apply,
          Seq()
        )

      retContext ++
        returnValue.map(v ⇒ Variable(v, retCode)) ++
        stdOut.map(v ⇒ Variable(v, outBuilder.toString)) ++
        stdErr.map(v ⇒ Variable(v, errBuilder.toString))


import org.openmole.plugin.task.container.ContainerTask.*

case class ContainerTask(
  image:                  InstalledContainerImage,
  command:                ContainerTask.Commands,
  workDirectory:          Option[String],
  relativePathRoot:       Option[String],
  hostFiles:              Seq[HostFile],
  environmentVariables:   Seq[EnvironmentVariable],
  errorOnReturnValue:     Boolean,
  returnValue:            Option[Val[Int]],
  stdOut:                 Option[Val[String]],
  stdErr:                 Option[Val[String]],
  config:                 InputOutputConfig,
  external:               External,
  info:                   InfoConfig) extends Task with ValidateTask:

  def validate = validateContainer(command.value, environmentVariables, external)

  override def process(executionContext: TaskExecutionContext) =
    image match
      case image: ContainerSystem.InstalledSIFImage =>
        ContainerTask.process(
          image = image,
          command = command,
          workDirectory = workDirectory,
          relativePathRoot = relativePathRoot,
          hostFiles = hostFiles,
          environmentVariables = environmentVariables,
          errorOnReturnValue = errorOnReturnValue,
          returnValue = returnValue,
          stdOut = stdOut,
          stdErr = stdErr,
          config = config,
          external = external,
          info = info)(executionContext)
      case image: ContainerSystem.InstalledFlatImage =>
        FlatContainerTask.process(
          image = image,
          command = command,
          workDirectory = workDirectory,
          relativePathRoot = relativePathRoot,
          hostFiles = hostFiles,
          environmentVariables = environmentVariables,
          errorOnReturnValue = errorOnReturnValue,
          returnValue = returnValue,
          stdOut = stdOut,
          stdErr = stdErr,
          config = config,
          external = external,
          info = info)(executionContext)
