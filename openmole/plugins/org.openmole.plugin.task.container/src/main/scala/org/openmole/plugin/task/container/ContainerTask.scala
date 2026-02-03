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
import monocle.syntax.all.*
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.task.external.*
import org.openmole.tool.cache.*
import org.openmole.tool.hash.*
import org.openmole.tool.lock.*
import org.openmole.tool.outputredirection.*
import org.openmole.tool.stream.*

import java.io.PrintStream
import java.util.UUID

object ContainerTask:
  val RegistryTimeout = PreferenceLocation("ContainerTask", "RegistryTimeout", Some(1 minutes))
  val RegistryRetryOnError = PreferenceLocation("ContainerTask", "RegistryRetryOnError", Some(5))

  lazy val installLockKey = LockKey()

  case class Commands(value: Vector[FromContext[String]])

  object Commands:
    implicit def fromContext(f: FromContext[String]): Commands = Commands(Vector(f))
    implicit def fromString(f: String): Commands = Commands(Vector(f))
    implicit def seqOfString(f: Seq[String]): Commands = Commands(f.map(x => x: FromContext[String]).toVector)
    implicit def seqOfFromContext(f: Seq[FromContext[String]]): Commands = Commands(f.toVector)

  type SingularitySIF = SingularityOverlay | SingularityMemory

  def apply(
    image:                  ContainerImage,
    command:                Commands,
    containerSystem:        OptionalArgument[ContainerSystem]                  = None,
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
    clearCache:             Boolean                                            = false)(using sourcecode.Name, DefinitionScope) =
    ExternalTask.build("ContainerTask"): buildParameters =>
      import buildParameters.*

      val installedImage =
        import taskExecutionBuildContext.given
        ContainerTask.install(
          containerSystem,
          image,
          install,
          buildParameters = buildParameters,
          volumes = installFiles.map(f => f -> f.getName),
          clearCache = clearCache
        )

      val containerExecution =
        ContainerTask.execution(
          image = installedImage,
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
          info = info
        )

      ExternalTask.execution: p =>
        import p.*
        containerExecution(p.executionContext).from(context)

    .set (outputs ++= Seq(returnValue.option, stdOut.option, stdErr.option).flatten)
    .withValidate: info =>
      validateContainer(command.value, environmentVariables, info.external)

  def embeddedResources(resources: Seq[External.Resource], embed: Boolean)(using FileService, TmpDirectory): Seq[(File, InstalledSingularityImage.EmbeddedResource)] =
    if embed
    then
      def resourcesPath = "/_openmole_data_"
      resources.collect:
        case r: External.FileResource =>
          val hash = FileService.hashNoCache(r.file).toString
          (r.file, InstalledSingularityImage.EmbeddedResource(s"$resourcesPath/$hash", r.destination, hash))
    else Seq()

  def install(
    containerSystem: Option[ContainerSystem],
    image: ContainerImage,
    install: Seq[String],
    buildParameters: ExternalTask.BuildParameters,
    volumes: Seq[(File, String)] = Seq.empty,
    errorDetail: Int => Option[String] = _ => None,
    clearCache: Boolean = false)(using TmpDirectory, SerializerService, OutputRedirection, NetworkService, ThreadProvider, Preference, Workspace, FileService, EventDispatcher): InstalledSingularityImage =
    containerSystem.getOrElse(SingularityOverlay()) match
      case containerSystem: SingularitySIF => installSIF(containerSystem, image, install, volumes, buildParameters, errorDetail, clearCache)
      case containerSystem: SingularityFlatImage => FlatContainerTask.install(containerSystem, image, install, volumes, buildParameters, errorDetail, clearCache)

  def installSIF(
    containerSystem: SingularitySIF,
    image: ContainerImage,
    install: Seq[String],
    volumes: Seq[(File, String)] = Seq.empty,
    buildParameters: ExternalTask.BuildParameters,
    errorDetail: Int => Option[String] = _ => None,
    clearCache: Boolean = false)(using TmpDirectory, SerializerService, OutputRedirection, NetworkService, ThreadProvider, Preference, Workspace, FileService, EventDispatcher) =

    import org.openmole.tool.hash.*

    def cacheId(image: ContainerImage): Seq[String] =
      image match
        case image: DockerImage      => Seq(image.image, image.tag, image.registry)
        case image: SavedDockerImage => Seq(Hash.file(image.file).toString)

    val volumeCacheKey = volumes.map((f, _) => FileService.hashNoCache(f).toString) ++ volumes.map((_, d) => d)


    val embeddedResourcesValue =
      val embed =
        containerSystem match
          case s: SingularityOverlay => s.embedResources
          case s: SingularityMemory => s.embedResources

      embeddedResources(buildParameters.external.resources, embed)

    val embedCacheKey = embeddedResourcesValue.map((_, e) => e.hash)

    val cacheKey: String =
      Hash.string:
        (cacheId(image) ++ install ++ volumeCacheKey ++ embedCacheKey ++ Seq("sif")).mkString("\n")
      .toString

    val cacheDirectory = summon[Workspace].tmpDirectory /> "container" /> "cached" /> cacheKey
    val serializedSingularityImage = cacheDirectory / "singularityImage.bin"

    val installedImage =
      cacheDirectory.withLockInDirectory:
        TmpDirectory.withTmpDir: tmpDirectory =>
          val containerDirectory = tmpDirectory / "fs"
          val singularityImageFile = cacheDirectory / "image.sif"

          if clearCache
          then
            serializedSingularityImage.delete
            containerDirectory.recursiveDelete

          if serializedSingularityImage.exists
          then summon[SerializerService].deserialize[_root_.container.Singularity.SingularityImageFile](serializedSingularityImage)
          else
            val img = localImage(image, containerDirectory, clearCache = clearCache, buildParameters = buildParameters)
            val installedImage =
              buildParameters.taskExecutionBuildContext.buildEventHandler.stage("Installing", s"Executing install commands"):
                executeInstall(img, install, volumes = volumes, errorDetail = errorDetail)

            embeddedResourcesValue.foreach: (f, d) =>
              _root_.container.ImageBuilder.copyIntoFlatImage(f, installedImage, d.path)

            val singularityImage =
              buildParameters.taskExecutionBuildContext.buildEventHandler.stage("Building", s"Building singularity image"):
                _root_.container.Singularity.buildSIF(installedImage, singularityImageFile, logger = summon[OutputRedirection].output)

            summon[SerializerService].serialize(singularityImage, serializedSingularityImage)
            singularityImage

    containerSystem match
      case overlay : SingularityOverlay if overlay.copy =>
        val overlayImageFile = TmpDirectory.newFile("overlay", ".img")
        val initializedOverlay =
          buildParameters.taskExecutionBuildContext.buildEventHandler.stage("Building", s"Building overlay"):
            _root_.container.Singularity.createOverlay(overlayImageFile, overlay.size, output = summon[OutputRedirection].output, error = summon[OutputRedirection].error)
        InstalledSingularityImage.InstalledSIFOverlayImage(installedImage, overlay, embeddedResourcesValue.map(_._2), Some(initializedOverlay))
      case overlay: SingularityOverlay =>
        InstalledSingularityImage.InstalledSIFOverlayImage(installedImage, overlay, embeddedResourcesValue.map(_._2), None)
      case memory: SingularityMemory =>
        InstalledSingularityImage.InstalledSIFMemoryImage(installedImage, memory, embeddedResourcesValue.map(_._2))


  def executeInstall(image: _root_.container.FlatImage, install: Seq[String], volumes: Seq[(File, String)], errorDetail: Int => Option[String])(using tmpDirectory: TmpDirectory, outputRedirection: OutputRedirection, networkService: NetworkService) =
    if install.isEmpty
    then image
    else
      val retCode = runCommandInFlatImageContainer(image, install, output = outputRedirection.output, error = outputRedirection.error, volumes = volumes.map((f, n) => f.getAbsolutePath -> n))
      if (retCode != 0) throw new UserBadDataError(s"Process exited a non 0 return code ($retCode)" + errorDetail(retCode).map(m => s": $m").getOrElse(""))
      image

  def localImage(image: ContainerImage, containerDirectory: File, clearCache: Boolean, buildParameters: ExternalTask.BuildParameters)(using NetworkService, Workspace, ThreadProvider, Preference, TmpDirectory, EventDispatcher) =
    image match
      case image: DockerImage =>
        if clearCache then _root_.container.ImageDownloader.imageDirectory(repositoryDirectory(summon[Workspace]), DockerImage.toRegistryImage(image)).recursiveDelete
        val savedImage =
          buildParameters.taskExecutionBuildContext.buildEventHandler.stage("Downloading", s"Downloading docker image ${image.image}:${image.tag}"):
            downloadImage(image, repositoryDirectory(summon[Workspace]))
        buildParameters.taskExecutionBuildContext.buildEventHandler.stage("Extracting", s"Extracting docker image ${image.image}:${image.tag}"):
          _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)
      case image: SavedDockerImage =>
        TmpDirectory.withTmpDir: imageDirectory =>
          buildParameters.taskExecutionBuildContext.buildEventHandler.stage("Extracting", s"Extracting docker image"):
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
      executor = ImageDownloader.Executor.parallel(using threadProvider.virtualThreadPool),
      proxy = networkService.httpProxy.map(p => ImageDownloader.HttpProxy(p.hostURI))
    )

  def repositoryDirectory(workspace: Workspace): File = workspace.persistentDir /> "container" /> "repos"

  def workDirectoryValue(imageWorkDirectory: Option[String], workDirectory: Option[String]) =
    workDirectory.orElse(imageWorkDirectory.filter(_.trim.nonEmpty)).getOrElse("/")

  def pathResolver(imageWorkDirectory: Option[String], relativePathRoot: Option[String], workDirectory: Option[String], path: String): File =
    def relativeWorkDirectoryValue = relativePathRoot.getOrElse(workDirectoryValue(imageWorkDirectory, workDirectory))

    val rootDirectory = File("/")
    if File(path).isAbsolute
    then rootDirectory / path
    else rootDirectory / relativeWorkDirectoryValue / path

  def runCommandInContainer(
    image: InstalledSingularityImage,
    commands: Seq[String],
    volumes: Seq[(String, String)] = Seq.empty,
    environmentVariables: Seq[(String, String)] = Seq.empty,
    workDirectory: Option[String] = None,
    verbose: Boolean = false,
    output: PrintStream,
    error: PrintStream)(using TmpDirectory, NetworkService) =
    image match
      case image: InstalledSingularityImage.InstalledSIFMemoryImage =>
        runCommandInSIFContainer(
          image.image,
          commands = commands,
          volumes = volumes,
          environmentVariables = environmentVariables,
          workDirectory = workDirectory,
          verbose = verbose,
          output = output,
          error = error,
          overlay = None,
          tmpFS = true
        )

      case image: InstalledSingularityImage.InstalledSIFOverlayImage =>
        TmpDirectory.withTmpFile: overlayFile =>
          runCommandInSIFContainer(
            image.image,
            commands = commands,
            volumes = volumes,
            environmentVariables = environmentVariables,
            workDirectory = workDirectory,
            verbose = verbose,
            output = output,
            error = error,
            overlay = Some(_root_.container.Singularity.createOverlay(overlayFile, image.containerSystem.size, output = output, error = error)),
            tmpFS = false
          )

      case image: InstalledSingularityImage.InstalledFlatImage =>
        runCommandInFlatImageContainer(
          image.image,
          commands = commands,
          volumes = volumes,
          environmentVariables = environmentVariables,
          workDirectory = workDirectory,
          verbose = verbose,
          output = output,
          error = error
        )


  def runCommandInFlatImageContainer(
    image: _root_.container.FlatImage,
    commands: Seq[String],
    volumes: Seq[(String, String)] = Seq.empty,
    environmentVariables: Seq[(String, String)] = Seq.empty,
    workDirectory: Option[String] = None,
    verbose: Boolean = false,
    output: PrintStream,
    error: PrintStream)(using tmpDirectory: TmpDirectory, networkService: NetworkService): Int =
    tmpDirectory.withTmpDir: directory =>
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

  def runCommandInSIFContainer(
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
    tmpDirectory.withTmpDir: directory =>
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


  type FileInfo = (External.DeployedFile, File)
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String

  def execution(
    image: InstalledSingularityImage,
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
    config: InputOutputConfig = InputOutputConfig()): ContainerTaskExecution =

    val embedExternal: External =
      if image.embedResources
      then
        external.focus(_.resources).modify: resources =>
          resources.collect:
            case r: External.EmptyDirectoryResource => r
      else external

    ContainerTaskExecution(
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
      external = embedExternal,
      info = info
    ).set(
      outputs ++= Seq(returnValue, stdOut, stdErr).flatten
    )


  def process(
    image:                  InstalledSingularityImage.InstalledSIFImage,
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
        containerPathResolver: String => File,
        hostFiles: Seq[HostFile],
        volumesInfo: Seq[VolumeInfo] = Seq()): Iterable[MountPoint] =
        val volumes =
          volumesInfo.map((f, d) => f.toString -> d) ++
            hostFiles.map(h => h.path -> h.destination) ++
            preparedFilesInfo.map((f, d) => d.getAbsolutePath -> containerPathResolver(f.expandedUserPath).toString)

        volumes.sortBy((_, bind) => bind.split("/").length)

      def uniquePathResolver(path: String): File =
        import org.openmole.tool.hash.*
        executionContext.taskExecutionDirectory /> Hash.string(path).toString / path

      val (preparedContext, preparedFilesInfo) = External.deployAndListInputFiles(external, context, uniquePathResolver)

      val volumes =
        prepareVolumes(
          preparedFilesInfo,
          pathResolver(image.workDirectory, relativePathRoot, workDirectory, _),
          hostFiles
        ).toVector

      val containerEnvironmentVariables =
        environmentVariables.map(v => v.name.from(preparedContext) -> v.value.from(preparedContext))

      def ignoreRetCode = !errorOnReturnValue || returnValue.isDefined
      def retCodeVariable = "_PROCESS_RET_CODE_"

      val linkCommand: Seq[String] =
        image.embeddedResources.flatMap: e =>
          val destination = ContainerTask.pathResolver(image.workDirectory, relativePathRoot, workDirectory, e.destination.from(context))
          Seq(s"mkdir -p \"$$(dirname \"$destination\")\"", s"ln -s \"${e.path}\" \"$destination\"")

      val commandValue =
        def dropEmptyLinesAtTheEnd(s: String) = s.split("\n").reverse.dropWhile(_.trim.isEmpty).reverse.mkString("\n")
        val value = command.value.map(_.from(context)).map(dropEmptyLinesAtTheEnd)
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
          val resolved = pathResolver(image.workDirectory, relativePathRoot, workDirectory, origin)
          OutputMapping(origin, resolved, directory, resultDirectory / directory / resolved.getName)

      val copyCommand: Seq[String] =
        outputFileMapping.flatMap: m =>
          val destinationDirectory = s"$resultDirectoryBind/${m.directory}/"
          Seq(s"""mkdir -p \"$destinationDirectory\"""", s"""cp -ra \"${m.resolved}\" \"$destinationDirectory\"""")

      val exitCommand = if ignoreRetCode then Seq(s"exit $$$retCodeVariable") else Seq()

      val copyVolume = resultDirectory.getAbsolutePath -> resultDirectoryBind

      val retCode =
        image match
          case image: InstalledSingularityImage.InstalledSIFOverlayImage if executionContext.localEnvironment.runtimeSetting.memoryOverlay =>
            runCommandInSIFContainer(
              image = image.image,
              tmpFS = true,
              commands = linkCommand ++ commandValue ++ copyCommand ++ exitCommand,
              workDirectory = Some(workDirectoryValue(image.workDirectory, workDirectory)),
              output = out,
              error = err,
              volumes = volumes ++ Seq(copyVolume),
              environmentVariables = containerEnvironmentVariables,
              verbose = image.containerSystem.verbose
            )
          case image: InstalledSingularityImage.InstalledSIFOverlayImage =>
            def createOverlayPool =
              WithInstance[_root_.container.Singularity.OverlayImage](pooled = image.containerSystem.reuse): () =>
                val overlay =
                  if image.containerSystem.reuse
                  then executionContext.moleExecutionDirectory.newFile("overlay", ".img")
                  else executionContext.taskExecutionDirectory.newFile("overlay", ".img")

                image.overlay match
                  case None => _root_.container.Singularity.createOverlay(overlay, image.containerSystem.size, output = out, error = err)
                  case Some(existingOverlay) =>
                    if image.containerSystem.reuse && executionContext.localEnvironment.threads == 1
                    then existingOverlay
                    else _root_.container.Singularity.copyOverlay(existingOverlay, overlay)

            val overlayCache = executionContext.cache.getOrElseUpdate(image.cacheKey)(createOverlayPool)

            overlayCache: overlay =>
              runCommandInSIFContainer(
                image = image.image,
                overlay = Some(overlay),
                commands = linkCommand ++ commandValue ++ copyCommand ++ exitCommand,
                workDirectory = Some(workDirectoryValue(image.workDirectory, workDirectory)),
                output = out,
                error = err,
                volumes = volumes ++ Seq(copyVolume),
                environmentVariables = containerEnvironmentVariables,
                verbose = image.containerSystem.verbose
              )

          case image: InstalledSingularityImage.InstalledSIFMemoryImage =>
            runCommandInSIFContainer(
              image = image.image,
              tmpFS = true,
              commands = linkCommand ++ commandValue ++ copyCommand ++ exitCommand,
              workDirectory = Some(workDirectoryValue(image.workDirectory, workDirectory)),
              output = out,
              error = err,
              volumes = volumes ++ Seq(copyVolume),
              environmentVariables = containerEnvironmentVariables,
              verbose = image.containerSystem.verbose
            )

      if errorOnReturnValue && !returnValue.isDefined && retCode != 0
      then
        def log =
          // last line might have been truncated
          val lst = tailBuilder.toString
          if lst.size >= tailSize
          then lst.split('\n').drop(1).map(l => s"|$l").mkString("\n")
          else lst.split('\n').map(l => s"|$l").mkString("\n")

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
        returnValue.map(v => Variable(v, retCode)) ++
        stdOut.map(v => Variable(v, outBuilder.toString)) ++
        stdErr.map(v => Variable(v, errBuilder.toString))

  def validateContainer(
    commands: Seq[FromContext[String]],
    environmentVariables: Seq[EnvironmentVariable],
    external: External): Validate = 
    Validate: p =>
      import p.*
  
      val allInputs = External.PWD :: p.inputs.toList
      val validateVariables = environmentVariables.flatMap(v => Seq(v.name, v.value)).flatMap(_.validate(allInputs))
  
      commands.flatMap(_.validate(allInputs)) ++
        validateVariables ++
        External.validate(external)(allInputs)

import org.openmole.plugin.task.container.ContainerTask.*

object ContainerTaskExecution:
  given InputOutputBuilder[ContainerTaskExecution] = InputOutputBuilder(Focus[ContainerTaskExecution](_.config))
  given ExternalBuilder[ContainerTaskExecution] = ExternalBuilder(Focus[ContainerTaskExecution](_.external))

case class ContainerTaskExecution(
  image: InstalledSingularityImage,
  command: ContainerTask.Commands,
  workDirectory: Option[String],
  relativePathRoot: Option[String],
  hostFiles: Seq[HostFile],
  environmentVariables: Seq[EnvironmentVariable],
  errorOnReturnValue: Boolean,
  returnValue: Option[Val[Int]],
  stdOut: Option[Val[String]],
  stdErr: Option[Val[String]],
  config: InputOutputConfig,
  external: External,
  info: InfoConfig) extends TaskExecution:

    override def apply(executionContext: TaskExecutionContext) = FromContext: p =>
      import p.*
      image match
        case image: InstalledSingularityImage.InstalledSIFImage =>
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
            info = info)(executionContext).from(context)
        case image: InstalledSingularityImage.InstalledFlatImage =>
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
            info = info)(executionContext).from(context)