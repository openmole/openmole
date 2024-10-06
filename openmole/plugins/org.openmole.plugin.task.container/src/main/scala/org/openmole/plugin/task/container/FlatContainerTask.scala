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
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.plugin.task.external.*
import org.openmole.tool.cache.{CacheKey, WithInstance}
import org.openmole.tool.hash.hashString
import org.openmole.tool.lock.*
import org.openmole.tool.outputredirection.*
import org.openmole.tool.stream.*

object FlatContainerTask:

  def install(containerSystem: SingularityFlatImage, image: ContainerImage, install: Seq[String], volumes: Seq[(File, String)] = Seq.empty, errorDetail: Int ⇒ Option[String] = _ ⇒ None, clearCache: Boolean = false)(implicit tmpDirectory: TmpDirectory, serializerService: SerializerService, outputRedirection: OutputRedirection, networkService: NetworkService, threadProvider: ThreadProvider, preference: Preference, workspace: Workspace, fileService: FileService) =
    import org.openmole.tool.hash._

    def cacheId(image: ContainerImage): Seq[String] =
      image match
        case image: DockerImage      ⇒ Seq(image.image, image.tag, image.registry)
        case image: SavedDockerImage ⇒ Seq(image.file.hash().toString)

    val volumeCacheKey = volumes.map { (f, _) ⇒ fileService.hashNoCache(f).toString } ++ volumes.map { (_, d) ⇒ d }
    val cacheKey: String = hashString((cacheId(image) ++ install ++ volumeCacheKey ++ "flat").mkString("\n")).toString
    val cacheDirectory = workspace.tmpDirectory /> "container" /> "cached" /> cacheKey
    val serializedFlatImage = cacheDirectory / "flatimage.bin"

    val installedImage =
      cacheDirectory.withLockInDirectory:
        val containerDirectory = cacheDirectory / "fs"

        if clearCache
        then
          serializedFlatImage.delete
          containerDirectory.recursiveDelete

        if serializedFlatImage.exists
        then serializerService.deserialize[_root_.container.FlatImage](serializedFlatImage)
        else
          val img = ContainerTask.localImage(image, containerDirectory, clearCache = clearCache)
          val installedImage = ContainerTask.executeInstall(img, install, volumes = volumes, errorDetail = errorDetail)
          serializerService.serialize(installedImage, serializedFlatImage)
          installedImage

    ContainerSystem.InstalledFlatImage(installedImage, containerSystem)

  export ContainerTask.Commands

  type FileInfo = (External.DeployedFile, File)
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String

  case class Cached(image: _root_.container.FlatImage, isolatedDirectories: Seq[VolumeInfo])

  def process(
    image:                  ContainerSystem.InstalledFlatImage,
    command:                FlatContainerTask.Commands,
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
      import _root_.container.FlatImage
      import parameters.*
      import executionContext.networkService

      case class OutputMapping(origin: String, resolved: File, directory: String, file: File)

      def workDirectoryValue(image: FlatImage) = workDirectory.orElse(image.workDirectory.filter(_.trim.nonEmpty)).getOrElse("/")
      def relativeWorkDirectoryValue(image: FlatImage) = relativePathRoot.getOrElse(workDirectoryValue(image))
      def rootDirectory(image: FlatImage) = image.file / _root_.container.FlatImage.rootfsName


      def containerPathResolver(image: FlatImage, path: String): File =
        val rootDirectory = File("/")
        if File(path).isAbsolute
        then rootDirectory / path
        else rootDirectory / relativeWorkDirectoryValue(image) / path

      val isolatedDirectories = image.containerSystem.isolatedDirectories ++ workDirectory

      def createPool =
        if executionContext.localEnvironment.remote && executionContext.localEnvironment.threads == 1
        then WithInstance[FlatContainerTask.Cached](pooled = false) { () ⇒ FlatContainerTask.Cached(image.image, Seq()) }
        else
          WithInstance[FlatContainerTask.Cached](pooled = image.containerSystem.reuseContainer): () ⇒
            val pooledImage =
              if image.containerSystem.duplicateImage && isolatedDirectories.isEmpty
              then
                val containerDirectory = executionContext.moleExecutionDirectory.newDirectory("container")
                _root_.container.ImageBuilder.duplicateFlatImage(image.image, containerDirectory)
              else image.image

            val isolatedDirectoryMapping =
              isolatedDirectories.map: d =>
                val isolatedDirectory = executionContext.moleExecutionDirectory.newDirectory("isolated")
                val containerPathValue = containerPathResolver(image.image, d).getPath
                val containerPath = rootDirectory(image.image) / containerPathValue

                if containerPath.exists()
                then containerPath.copy(isolatedDirectory)
                else isolatedDirectory.mkdirs()

                isolatedDirectory -> containerPathValue

            FlatContainerTask.Cached(image = pooledImage, isolatedDirectories = isolatedDirectoryMapping)

      val pool = executionContext.cache.getOrElseUpdate(image.containerSystem.cacheKey)(createPool)

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

        val commandValue =
          val value = command.value.map(_.from(context))
          if !errorOnReturnValue || returnValue.isDefined
          then Seq(s"(${value.mkString(" && ")} ; true)")
          else value

        // Prepare the copy of output files
        val resultDirectory = executionContext.moleExecutionDirectory.newDirectory("result", create = true)
        val resultDirectoryBind = "/_result_"

        val outputFileMapping =
          external.outputFiles.map: f =>
            val origin = f.origin.from(context)
            val directory = java.util.UUID.randomUUID.toString
            val resolved = containerPathResolver(image.image, origin)
            OutputMapping(origin, resolved, directory, resultDirectory / directory / resolved.getName)

        val copyCommand: Seq[String] =
          outputFileMapping.flatMap: m =>
            val destinationDirectory = s"$resultDirectoryBind/${m.directory}/"
            Seq(s"""mkdir -p \"$destinationDirectory\"""", s"""cp -ra \"${m.resolved}\" \"$destinationDirectory\"""")

        val copyVolume = resultDirectory.getAbsolutePath -> resultDirectoryBind

        val retCode =
          ContainerTask.runCommandInFlatImageContainer(
            image = container.image,
            commands = commandValue ++ copyCommand,
            workDirectory = Some(workDirectoryValue(container.image)),
            output = out,
            error = err,
            volumes = volumes ++ Seq(copyVolume),
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

