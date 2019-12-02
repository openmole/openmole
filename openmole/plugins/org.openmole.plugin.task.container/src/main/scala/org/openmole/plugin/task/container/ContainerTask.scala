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

import monocle.macros.Lenses
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder.{ DefinitionScope, InfoBuilder, InfoConfig, InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.task.{ Task, TaskExecutionContext }
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.plugin.task.container.ContainerTask.{ Commands, downloadImage, extractImage, repositoryDirectory }
import org.openmole.plugin.task.external.{ EnvironmentVariable, External, ExternalBuilder }
import org.openmole.tool.cache.{ CacheKey, WithInstance }
import org.openmole.tool.hash.hashString
import org.openmole.tool.lock._
import org.openmole.tool.outputredirection._
import org.openmole.tool.stream._

object ContainerTask {

  implicit def isTask: InputOutputBuilder[ContainerTask] = InputOutputBuilder(ContainerTask.config)
  implicit def isExternal: ExternalBuilder[ContainerTask] = ExternalBuilder(ContainerTask.external)
  implicit def isInfo = InfoBuilder(ContainerTask.info)

  val RegistryTimeout = ConfigurationLocation("ContainerTask", "RegistryTimeout", Some(1 minutes))
  val RegistryRetryOnError = ConfigurationLocation("ContainerTask", "RegistryRetryOnError", Some(5))

  lazy val installLockKey = LockKey()

  case class Commands(value: Vector[FromContext[String]])

  object Commands {
    implicit def fromContext(f: FromContext[String]) = Commands(Vector(f))
    implicit def fromString(f: String) = Commands(Vector(f))
    implicit def seqOfString(f: Seq[String]) = Commands(f.map(x ⇒ x: FromContext[String]).toVector)
    implicit def seqOfFromContext(f: Seq[FromContext[String]]) = Commands(f.toVector)
  }

  def extractImage(image: SavedDockerImage, directory: File): _root_.container.SavedImage = {
    import _root_.container._
    ImageBuilder.extractImage(image.file, directory, compressed = image.compressed)
  }

  def downloadImage(image: DockerImage, repository: File)(implicit preference: Preference, threadProvider: ThreadProvider, networkService: NetworkService): _root_.container.SavedImage = {
    import _root_.container._

    ImageDownloader.downloadContainerImage(
      _root_.container.RegistryImage(
        imageName = image.image,
        tag = image.tag,
        registry = image.registry
      ),
      repository,
      timeout = preference(RegistryTimeout),
      retry = Some(preference(RegistryRetryOnError)),
      executor = ImageDownloader.Executor.parallel(threadProvider.pool),
      proxy = networkService.httpProxy.map(p ⇒ ImageDownloader.HttpProxy(p.hostURI))
    )
  }

  def repositoryDirectory(workspace: Workspace) = workspace.persistentDir /> "container" /> "repos"

  def installProot(installDirectory: File) = {
    val proot = installDirectory / "proot"

    def retrieveResource(candidateFile: File, resourceName: String, executable: Boolean = false) =
      if (!candidateFile.exists()) {
        withClosable(this.getClass.getClassLoader.getResourceAsStream(resourceName))(_.copy(candidateFile))
        if (executable) candidateFile.setExecutable(true)
        candidateFile
      }

    retrieveResource(proot, "proot", true)

    proot
  }

  def runCommandInContainer(
    containerSystem:      ContainerSystem,
    image:                _root_.container.FlatImage,
    commands:             Seq[String],
    volumes:              Seq[(String, String)]           = Seq.empty,
    environmentVariables: Seq[(String, String)]           = Seq.empty,
    workDirectory:        Option[String]                  = None,
    logger:               scala.sys.process.ProcessLogger)(implicit tmpDirectory: TmpDirectory) = {
    val retCode =
      containerSystem match {
        case Proot(noSeccomp, kernel) ⇒
          tmpDirectory.withTmpDir { directory ⇒
            val proot = installProot(directory)

            _root_.container.Proot.execute(
              image,
              directory / "tmp",
              commands = commands,
              proot = proot.getAbsolutePath,
              logger = logger,
              kernel = Some(kernel),
              noSeccomp = noSeccomp,
              bind = volumes,
              environmentVariables = environmentVariables,
              workDirectory = workDirectory
            )
          }
        case Singularity(command) ⇒
          tmpDirectory.withTmpDir { directory ⇒
            _root_.container.Singularity.executeFlatImage(
              image,
              directory / "tmp",
              commands = commands,
              logger = logger,
              bind = volumes,
              environmentVariables = environmentVariables,
              workDirectory = workDirectory,
              singularityCommand = command
            )
          }
      }

    retCode
  }

  def apply(
    image:                  ContainerImage,
    command:                Commands,
    containerSystem:        ContainerSystem                                    = Proot(),
    installContainerSystem: ContainerSystem                                    = Proot(),
    install:                Seq[String]                                        = Vector.empty,
    workDirectory:          OptionalArgument[String]                           = None,
    hostFiles:              Seq[HostFile]                                      = Vector.empty,
    environmentVariables:   Seq[EnvironmentVariable]                           = Vector.empty,
    errorOnReturnValue:     Boolean                                            = true,
    returnValue:            OptionalArgument[Val[Int]]                         = None,
    stdOut:                 OptionalArgument[Val[String]]                      = None,
    stdErr:                 OptionalArgument[Val[String]]                      = None,
    reuseContainer:         Boolean                                            = true,
    containerPoolKey:       CacheKey[WithInstance[_root_.container.FlatImage]] = CacheKey())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, tmpDirectory: TmpDirectory, networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, outputRedirection: OutputRedirection, serializerService: SerializerService) = {
    new ContainerTask(
      containerSystem,
      prepare(installContainerSystem, image, install),
      command,
      workDirectory = workDirectory.option,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue.option,
      hostFiles = hostFiles,
      environmentVariables = environmentVariables,
      stdOut = stdOut.option,
      stdErr = stdErr.option,
      reuseContainer = reuseContainer,
      config = InputOutputConfig(),
      info = InfoConfig(),
      external = External(),
      containerPoolKey = containerPoolKey)
  }

  def prepare(containerSystem: ContainerSystem, image: ContainerImage, install: Seq[String])(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, workspace: Workspace) = {
    def cacheId(image: ContainerImage): Seq[String] =
      image match {
        case image: DockerImage ⇒ Seq(image.image, image.tag, image.registry)
        case image: SavedDockerImage ⇒
          import org.openmole.tool.hash._
          Seq(image.file.hash().toString)
      }

    val cacheKey: String = hashString((cacheId(image) ++ install).mkString("\n")).toString
    val cacheDirectory = workspace.tmpDirectory /> "container" /> "cached" /> cacheKey
    val serializedFlatImage = cacheDirectory / "flatimage.bin"

    //OutputManager.systemOutput.println("test " + serializedFlatImage + " " + serializedFlatImage.exists)

    cacheDirectory.withLockInDirectory {
      if (serializedFlatImage.exists) serializerService.deserialize[_root_.container.FlatImage](serializedFlatImage)
      else {
        val containerDirectory = cacheDirectory / "fs"
        val img = localImage(image, containerDirectory)
        val installedImage = executeInstall(containerSystem, img, install)
        serializerService.serialize(installedImage, serializedFlatImage)
        //OutputManager.systemOutput.println("created " + serializedFlatImage + " " + serializedFlatImage.exists)
        installedImage
      }
    }
  }

  def executeInstall(containerSystem: ContainerSystem, image: _root_.container.FlatImage, install: Seq[String])(implicit tmpDirectory: TmpDirectory, outputRedirection: OutputRedirection) =
    if (install.isEmpty) image
    else {
      val retCode = runCommandInContainer(containerSystem, image, install, logger = scala.sys.process.ProcessLogger.apply(s ⇒ outputRedirection.output.println(s), s ⇒ outputRedirection.error.println(s)))
      if (retCode != 0) throw new UserBadDataError(s"Process exited a non 0 return code ($retCode)")
      image
    }

  def localImage(image: ContainerImage, containerDirectory: File)(implicit networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, tmpDirectory: TmpDirectory) =
    image match {
      case image: DockerImage ⇒
        val savedImage = downloadImage(image, repositoryDirectory(workspace))
        _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)
      case image: SavedDockerImage ⇒
        tmpDirectory.withTmpDir { imageDirectory ⇒
          val savedImage = extractImage(image, imageDirectory)
          _root_.container.ImageBuilder.flattenImage(savedImage, containerDirectory)
        }
    }

  def newCacheKey = CacheKey[WithInstance[PreparedImage]]()

  type FileInfo = (External.DeployedFile, File)
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String
}

import ContainerTask._

@Lenses case class ContainerTask(
  containerSystem:      ContainerSystem,
  image:                PreparedImage,
  command:              Commands,
  workDirectory:        Option[String],
  hostFiles:            Seq[HostFile],
  environmentVariables: Seq[EnvironmentVariable],
  errorOnReturnValue:   Boolean,
  returnValue:          Option[Val[Int]],
  stdOut:               Option[Val[String]],
  stdErr:               Option[Val[String]],
  reuseContainer:       Boolean,
  config:               InputOutputConfig,
  external:             External,
  info:                 InfoConfig,
  containerPoolKey:     CacheKey[WithInstance[PreparedImage]]) extends Task with ValidateTask { self ⇒

  def validate = validateContainer(command.value, environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    import parameters._

    //    val (proot, noSeccomp, kernel) =
    //      containerSystem match {
    //        case proot: Proot ⇒
    //          executionContext.lockRepository.withLock(ContainerTask.installLockKey) {
    //            (ContainerTask.installProot(executionContext.moleExecutionDirectory), proot.noSeccomp, proot.kernel)
    //          }
    //      }

    def createPool =
      WithInstance { () ⇒
        val containersDirectory = executionContext.moleExecutionDirectory.newDir("container")
        _root_.container.ImageBuilder.duplicateFlatImage(image, containersDirectory)
      }(close = _.file.recursiveDelete, pooled = reuseContainer)

    val pool = executionContext.cache.getOrElseUpdate(containerPoolKey, createPool)

    val stdOutBuffer = new StringBuilder()
    val stdErrBuffer = new StringBuilder()

    def processOutput(s: String) = {
      executionContext.outputRedirection.output.println(s)
      if (stdOut.isDefined) {
        stdOutBuffer.append(s)
        stdOutBuffer.append('\n')
      }
    }

    def processErr(s: String) = {
      executionContext.outputRedirection.error.println(s)
      if (stdErr.isDefined) {
        stdErrBuffer.append(s)
        stdErrBuffer.append('\n')
      }
    }

    def prepareVolumes(
      preparedFilesInfo:     Iterable[FileInfo],
      containerPathResolver: String ⇒ File,
      hostFiles:             Seq[HostFile],
      volumesInfo:           List[VolumeInfo]   = List.empty[VolumeInfo]): Iterable[MountPoint] =
      preparedFilesInfo.map { case (f, d) ⇒ d.getAbsolutePath → containerPathResolver(f.expandedUserPath).toString } ++
        hostFiles.map { h ⇒ h.path → h.destination } ++
        volumesInfo.map { case (f, d) ⇒ f.toString → d }

    pool { container ⇒
      val workDirectoryValue = workDirectory.orElse(container.workDirectory.filter(!_.trim.isEmpty)).getOrElse("/")
      val inputDirectory = executionContext.taskExecutionDirectory /> "inputs"

      def containerPathResolver = inputPathResolver(File("/"), workDirectoryValue) _

      val (preparedContext, preparedFilesInfo) = External.deployAndListInputFiles(external, context, inputPathResolver(inputDirectory, workDirectoryValue))
      val volumes = prepareVolumes(preparedFilesInfo, containerPathResolver, hostFiles).toVector

      def outputPathResolverValue(rootDirectory: File) = outputPathResolver(
        preparedFilesInfo.map { case (f, d) ⇒ f.toString → d.toString },
        hostFiles.map { h ⇒ h.path → h.destination },
        inputDirectory,
        workDirectoryValue,
        rootDirectory
      ) _

      val containerEnvironmentVariables =
        environmentVariables.map { v ⇒ v.name.from(preparedContext) -> v.value.from(preparedContext) }

      val retCode =
        runCommandInContainer(
          containerSystem,
          image = container,
          commands = command.value.map(_.from(context)),
          workDirectory = Some(workDirectoryValue),
          logger = scala.sys.process.ProcessLogger.apply(processOutput, processErr),
          volumes = volumes,
          environmentVariables = containerEnvironmentVariables
        )

      //      val retCode =
      //        _root_.container.Proot.execute(
      //          container,
      //          executionContext.taskExecutionDirectory / "tmp",
      //          commands = command.value.map(_.from(context)),
      //          workDirectory = Some(workDirectoryValue),
      //          proot = proot.getAbsolutePath,
      //          logger = scala.sys.process.ProcessLogger.apply(processOutput, processErr),
      //          noSeccomp = noSeccomp,
      //          kernel = Some(kernel),
      //          bind = volumes,
      //          environmentVariables = containerEnvironmentVariables
      //        )

      if (errorOnReturnValue && !returnValue.isDefined && retCode != 0)
        throw new UserBadDataError(s"Process exited a non 0 return code ($retCode), you can chose ignore this by settings errorOnReturnValue = true")

      val rootDirectory = container.file / _root_.container.FlatImage.rootfsName
      val retContext = External.fetchOutputFiles(external, outputs, preparedContext, outputPathResolverValue(rootDirectory), Seq(rootDirectory, executionContext.taskExecutionDirectory))

      retContext ++
        returnValue.map(v ⇒ Variable(v, retCode)) ++
        stdOut.map(v ⇒ Variable(v, stdOut.toString)) ++
        stdErr.map(v ⇒ Variable(v, stdErr.toString))

    }
  }

}
