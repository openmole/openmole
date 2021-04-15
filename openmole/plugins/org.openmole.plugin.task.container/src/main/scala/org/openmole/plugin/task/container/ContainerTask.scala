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

import monocle.macros.Lenses
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.preference.{ PreferenceLocation, Preference }
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

  val RegistryTimeout = PreferenceLocation("ContainerTask", "RegistryTimeout", Some(1 minutes))
  val RegistryRetryOnError = PreferenceLocation("ContainerTask", "RegistryRetryOnError", Some(5))

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

    def proxyVariables =
      networkService.httpProxy match {
        case Some(proxy) ⇒
          Seq(
            "http_proxy" -> proxy.hostURI,
            "HTTP_PROXY" -> proxy.hostURI,
            "https_proxy" -> proxy.hostURI,
            "HTTPS_PROXY" -> proxy.hostURI
          )
        case None ⇒ Seq()
      }

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
              environmentVariables = proxyVariables ++ environmentVariables,
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
              environmentVariables = proxyVariables ++ environmentVariables,
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
    reuseContainer:         Boolean                                            = true,
    clearCache:             Boolean                                            = false,
    containerPoolKey:       CacheKey[WithInstance[_root_.container.FlatImage]] = CacheKey())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, tmpDirectory: TmpDirectory, networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, outputRedirection: OutputRedirection, serializerService: SerializerService, fileService: FileService) = {
    new ContainerTask(
      containerSystem,
      prepare(installContainerSystem, image, install),
      command,
      workDirectory = workDirectory.option,
      relativePathRoot = relativePathRoot,
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
      containerPoolKey = containerPoolKey) set (
      outputs += (Seq(returnValue.option, stdOut.option, stdErr.option).flatten: _*)
    )
  }

  def prepare(containerSystem: ContainerSystem, image: ContainerImage, install: Seq[String], volumes: Seq[(String, String)] = Seq.empty, errorDetail: Int ⇒ Option[String] = _ ⇒ None, clearCache: Boolean = false)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, workspace: Workspace, fileService: FileService) = {
    import org.openmole.tool.hash._

    def cacheId(image: ContainerImage): Seq[String] =
      image match {
        case image: DockerImage      ⇒ Seq(image.image, image.tag, image.registry)
        case image: SavedDockerImage ⇒ Seq(image.file.hash().toString)
      }

    val volumeCacheKey = volumes.map { case (f, _) ⇒ fileService.hashNoCache(f).toString } ++ volumes.map { case (_, d) ⇒ d }
    val cacheKey: String = hashString((cacheId(image) ++ install ++ volumeCacheKey).mkString("\n")).toString
    val cacheDirectory = workspace.tmpDirectory /> "container" /> "cached" /> cacheKey
    val serializedFlatImage = cacheDirectory / "flatimage.bin"

    cacheDirectory.withLockInDirectory {
      val containerDirectory = cacheDirectory / "fs"

      if (clearCache) {
        serializedFlatImage.delete
        containerDirectory.recursiveDelete
      }

      if (serializedFlatImage.exists) serializerService.deserialize[_root_.container.FlatImage](serializedFlatImage)
      else {
        val img = localImage(image, containerDirectory, clearCache = clearCache)
        val installedImage = executeInstall(containerSystem, img, install, volumes = volumes, errorDetail = errorDetail)
        serializerService.serialize(installedImage, serializedFlatImage)
        installedImage
      }
    }
  }

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
  relativePathRoot:     Option[String],
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

  def validate(inputs: Seq[Val[_]]) = validateContainer(command.value, environmentVariables, external, inputs)

  override def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    import parameters._
    import executionContext.networkService

    def createPool =
      executionContext.remote match {
        case Some(r) if r.threads == 1 ⇒ WithInstance { () ⇒ image }(pooled = false)
        case _ ⇒
          WithInstance { () ⇒
            val containersDirectory = executionContext.moleExecutionDirectory.newDir("container")
            _root_.container.ImageBuilder.duplicateFlatImage(image, containersDirectory)
          }(close = _.file.recursiveDelete, pooled = reuseContainer)
      }

    val pool = executionContext.cache.getOrElseUpdate(containerPoolKey, createPool)

    val outBuilder = new StringOutputStream
    val errBuilder = new StringOutputStream

    val out: PrintStream =
      if (stdOut.isDefined) new PrintStream(MultiplexedOutputStream(outBuilder, executionContext.outputRedirection.output))
      else executionContext.outputRedirection.output

    val err =
      if (stdErr.isDefined) new PrintStream(MultiplexedOutputStream(errBuilder, executionContext.outputRedirection.error))
      else executionContext.outputRedirection.error

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
      val relativePathRootValue = relativePathRoot.getOrElse(workDirectoryValue)
      val inputDirectory = executionContext.taskExecutionDirectory /> "inputs"

      def containerPathResolver = inputPathResolver(File("/"), relativePathRootValue) _

      val (preparedContext, preparedFilesInfo) = External.deployAndListInputFiles(external, context, inputPathResolver(inputDirectory, relativePathRootValue))

      val volumes = prepareVolumes(preparedFilesInfo, containerPathResolver, hostFiles).toVector

      val containerEnvironmentVariables =
        environmentVariables.map { v ⇒ v.name.from(preparedContext) -> v.value.from(preparedContext) }

      val retCode =
        runCommandInContainer(
          containerSystem,
          image = container,
          commands = command.value.map(_.from(context)),
          workDirectory = Some(workDirectoryValue),
          output = out,
          error = err,
          volumes = volumes,
          environmentVariables = containerEnvironmentVariables
        )

      if (errorOnReturnValue && !returnValue.isDefined && retCode != 0)
        throw new UserBadDataError(s"Process exited a non 0 return code ($retCode), you can chose ignore this by settings errorOnReturnValue = true")

      val rootDirectory = container.file / _root_.container.FlatImage.rootfsName

      def outputPathResolverValue =
        outputPathResolver(
          preparedFilesInfo.map { case (f, d) ⇒ f.toString → d.toString },
          hostFiles.map { h ⇒ h.path → h.destination },
          inputDirectory,
          relativePathRootValue,
          rootDirectory
        ) _

      val retContext = External.fetchOutputFiles(external, outputs, preparedContext, outputPathResolverValue, Seq(rootDirectory, executionContext.taskExecutionDirectory))

      retContext ++
        returnValue.map(v ⇒ Variable(v, retCode)) ++
        stdOut.map(v ⇒ Variable(v, outBuilder.toString)) ++
        stdErr.map(v ⇒ Variable(v, errBuilder.toString))

    }
  }

}
