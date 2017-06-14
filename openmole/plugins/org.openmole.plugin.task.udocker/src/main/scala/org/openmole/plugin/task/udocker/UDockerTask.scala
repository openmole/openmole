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
import java.util.concurrent.locks.ReentrantLock

import monocle.macros._
import cats.implicits._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace._
import org.openmole.core.context._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.builder._
import org.openmole.tool.stream._
import org.openmole.core.expansion._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.systemexec._
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference._
import org.openmole.plugin.task.container
import org.openmole.plugin.task.udocker.DockerMetadata._
import org.openmole.plugin.task.udocker.Registry._
import org.openmole.plugin.task.udocker.UDockerTask.LocalDockerImage
import org.openmole.tool.cache._
import squants._
import squants.time.TimeConversions._
import org.openmole.tool.file._
import org.openmole.tool.lock._
import io.circe.generic.extras.auto._
import io.circe.jawn.{ decode, decodeFile }
import io.circe.syntax._

import scala.language.postfixOps

object UDockerTask {

  val RegistryTimeout = ConfigurationLocation("UDockerTask", "RegistryTimeout", Some(1 minutes))

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
  )(implicit name: sourcecode.Name, newFile: NewFile, workspace: Workspace, preference: Preference, fileService: FileService): UDockerTask = {

    val dockerImage = toLocalImage(image) match {
      case Right(x) ⇒ x
      case Left(x)  ⇒ throw new UserBadDataError(x)
    }

    new UDockerTask(
      localDockerImage = dockerImage,
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

  // TODO make vals??
  def layersDirectory(workspace: Workspace) = workspace.persistentDir /> "udocker" /> "layers"

  def repositoriesDirectory(workspace: Workspace) = workspace.persistentDir /> "udocker" /> "repos"

  def downloadImage(dockerImage: DockerImage, timeout: Time)(implicit newFile: NewFile, workspace: Workspace) = {
    import Registry._

    def layerFile(workspace: Workspace, layer: Layer) = layersDirectory(workspace) / layer.digest

    val m = manifest(dockerImage, timeout)
    val ls = layers(m.value)
    val lDirectory = layersDirectory(workspace)

    val localLayers =
      for { l ← ls } yield {
        val lf = layerFile(workspace, l)
        // TODO unify with buildRepoV2 (duplicate code)
        def downloadLayer =
          newFile.withTmpFile { tmpFile ⇒
            blob(dockerImage, l, tmpFile, timeout)
            lDirectory.withLockInDirectory {
              if (!lf.exists) tmpFile move lf
            }
          }

        if (!lf.exists) downloadLayer
        l → lf
      }

    // FIXME how reliable is it to assume config in first v1Compat string?
    val imageConfig = for {
      v1 ← m.value.history.map(_.head)
    } yield v1.v1Compatibility

    val localImage = LocalDockerImage(dockerImage.image, dockerImage.tag, localLayers.toVector, Vector.empty, imageConfig.getOrElse(""))

    buildRepoV2(localImage, m.value)

    localImage
  }

  def buildRepoV2(localDockerImage: LocalDockerImage, manifest: ImageManifestV2Schema1)(implicit workspace: Workspace): Unit = {
    val layerDir = layersDirectory(workspace)
    val reposDir = repositoriesDirectory(workspace)
    val imageRepositoryDirectory = reposDir /> localDockerImage.image /> localDockerImage.tag

    // move layer file in persistent/layers and create link to layer in persistent/repos
    for {
      layer ← localDockerImage.layers
    } {

      val layerFileInLayers = layerDir / layer._1.digest
      val layerFileInRepos = imageRepositoryDirectory / layer._1.digest

      // lock before existence tests to account for incomplete files
      layerDir.withLockInDirectory {
        if (!layerFileInLayers.exists()) layer._2 move layerFileInLayers
        if (!layerFileInRepos.exists()) layerFileInRepos createLinkTo layerFileInLayers.getAbsolutePath
      }
    }

    val imageId = s"${localDockerImage.image}:${localDockerImage.tag}"

    imageRepositoryDirectory / "manifest" content = manifest.asJson.toString
    imageRepositoryDirectory / "TAG" content = s"$reposDir/$imageId"
    imageRepositoryDirectory / "v2" content = ""
  }

  /**
   * Mimic `docker load -i` to reconstruct a registry entry for a previously saved image (using `docker save`).
   *
   * Assume v1.x Image JSON format and registry protocol v2 Schema 1
   */
  def loadImage(dockerImage: SavedDockerImage)(implicit newFile: NewFile, fileService: FileService, workspace: Workspace): Either[String, LocalDockerImage] = newFile.withTmpDir { extractedImage ⇒

    import org.openmole.tool.tar._

    dockerImage.file.extract(extractedImage)

    val manifestContent = (extractedImage / "manifest.json").content
    val topLevelManifests = decode[List[TopLevelImageManifest]](manifestContent)
    val topLevelImageManifest = topLevelManifests.map(_.head)
    val layersNamesE = topLevelImageManifest.map(_.Layers.distinct)
    val layersConfigsE = layersNamesE.map(layers ⇒ layers.map(path ⇒ s"${path.split("/").head}/json"))

    // FIXME change pattern matching
    val Right((layersNames, layersConfigs)) = for {
      layersNames ← layersNamesE
      layersConfigs ← layersConfigsE
    } yield (layersNames, layersConfigs)

    val layers =
      layersNames.map(l ⇒ moveLayerElement[Layer](extractedImage, l, "layer", Layer.apply))
    val configs = layersConfigs.map(c ⇒ moveLayerElement[LayerConfig](extractedImage, c, "json", LayerConfig.apply))

    val Right((imageAndTag, imageJSOName)) = for {
      topLevelManifest ← topLevelImageManifest
      imageAndTag = topLevelManifest.RepoTags.map(_.split(":")).headOption
      imageJSONName = topLevelManifest.Config
    } yield (imageAndTag, imageJSONName)

    val v = validateDockerImage(DockerImageData(imageAndTag, imageJSOName)) bimap (

      errors ⇒ errors.foldLeft("\n") { case (acc, err) ⇒ acc + err.msg },

      validImage ⇒ {

        val ValidDockerImageData((image, tag), imageJSONName) = validImage
        val imageJSONString = (extractedImage / imageJSONName).content
        LocalDockerImage(image, tag, layers, configs, imageJSONString)
      }
    )

    for {
      localImage ← v.toEither
      imageJSON ← decode[ImageJSON](localImage.imageJSON).leftMap(_.toString)
    } yield {

      val (fsLayers, history) = localImage.layers.zip(localImage.layersConfig).map {
        case ((layer, _), (_, layerConfigFile)) ⇒
          val layerDigest = DockerMetadata.Digest(layer.digest)
          val imageJSON = decodeFile[ImageJSON](layerConfigFile)

          (layerDigest, imageJSON)
      }.toList.unzip

      val manifest = DockerMetadata.ImageManifestV2Schema1(
        name = Some(localImage.image),
        tag = Some(localImage.tag),
        architecture = Some(imageJSON.architecture),
        fsLayers = Some(fsLayers),
        history = history.sequenceU.toOption.map(histList ⇒ histList.map(h ⇒ DockerMetadata.V1History(h.asJson.toString))),
        schemaVersion = Some(1)
      )

      buildRepoV2(localImage, manifest)
      localImage
    }
  }

  def toLocalImage(containerImage: ContainerImage)(implicit preference: Preference, newFile: NewFile, workspace: Workspace, fileService: FileService): Either[String, LocalDockerImage] =
    containerImage match {
      // TODO Eitherify download
      // Left(s"Failed to download docker image $i")
      case i: DockerImage      ⇒ Right(downloadImage(i, preference(RegistryTimeout)))
      case i: SavedDockerImage ⇒ loadImage(i)
    }

  // TODO review data structure
  case class LocalDockerImage(image: String, tag: String, layers: Vector[(Registry.Layer, File)], layersConfig: Vector[(Registry.LayerConfig, File)], imageJSON: String) {
    lazy val id = UUID.randomUUID().toString
  }

}

@Lenses case class UDockerTask(
    localDockerImage:     LocalDockerImage,
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
) extends Task with ValidateTask { self ⇒
  override def config = InputOutputConfig.outputs.modify(_ ++ Seq(stdOut, stdErr, returnValue).flatten)(_config)
  override def validate = container.validateContainer(command, environmentVariables, external, inputs)

  type FileInfo = (External.ToPut, File)
  type HostFile = (String, Option[String])
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)

  type ContainerId = String
  lazy val containerPoolKey = CacheKey[WithInstance[ContainerId]]()

  @transient lazy val udockerInstallLock = new ReentrantLock

  override protected def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒

    import parameters._

    val udockerInstallDirectory = executionContext.tmpDirectory /> "udocker"
    val udockerTarBall = udockerInstallDirectory / "udockertarball.tar.gz"
    val udocker = udockerInstallDirectory / "udocker"

    val imageJSONE = decode[ImageJSON](localDockerImage.imageJSON)

    def installUDocker() = {

      def retrieveResource(candidateFile: File, resourceName: String, executable: Boolean = false) =
        if (!candidateFile.exists()) {
          withClosable(this.getClass.getClassLoader.getResourceAsStream(resourceName))(_.copy(candidateFile))
          if (executable) candidateFile.setExecutable(true)

          candidateFile
        }

      // lock in any case since file.exists() might return for an incomplete file
      udockerInstallLock {
        retrieveResource(udockerTarBall, "udocker.tar.gz")
        retrieveResource(udocker, "udocker", executable = true)
      }
    }

    installUDocker()

    External.withWorkDir(executionContext) { taskWorkDirectory ⇒
      taskWorkDirectory.mkdirs()

      val containersDirectory =
        if (reuseContainer) executionContext.tmpDirectory /> "containers" /> localDockerImage.id
        else taskWorkDirectory /> "containers" /> localDockerImage.id

      def udockerVariables(logLevel: Int = 1) =
        Vector(
          "UDOCKER_DIR" → udockerInstallDirectory.getAbsolutePath,
          "UDOCKER_TMPDIR" → executionContext.tmpDirectory.getAbsolutePath,
          "UDOCKER_TARBALL" → udockerTarBall.getAbsolutePath,
          "UDOCKER_LOGLEVEL" → logLevel.toString,
          "HOME" → taskWorkDirectory.getAbsolutePath,
          "UDOCKER_CONTAINERS" → containersDirectory.getAbsolutePath,
          "UDOCKER_REPOS" → UDockerTask.repositoriesDirectory(executionContext.workspace).getAbsolutePath,
          "UDOCKER_LAYERS" → UDockerTask.layersDirectory(executionContext.workspace).getAbsolutePath
        )

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

      val dockerWorkDirectory =
        for {
          imageJSON ← imageJSONE
          workDir = imageJSON.config.WorkingDir
        } yield if (workDir.isEmpty) "/" else workDir

      val userWorkDirectory = workDirectory.getOrElse(dockerWorkDirectory.getOrElse("/"))

      def newContainer(imageId: String)() = newFile.withTmpDir { tmpDirectory ⇒

        val name = containerName(UUID.randomUUID().toString)
        val commandline = commandLine(s"${udocker.getAbsolutePath} create --name=$name $imageId")
        execute(commandline, tmpDirectory, udockerVariables(), returnOutput = true, returnError = true)
        name
      }

      val imageId = s"${localDockerImage.image}:${localDockerImage.tag}"

      val pool =
        if (reuseContainer) executionContext.cache.getOrElseUpdate(
          containerPoolKey, Pool[ContainerId](newContainer(imageId))
        )
        else WithNewInstance[ContainerId](newContainer(imageId))

      pool { runId ⇒
        val inputDirectory = taskWorkDirectory /> "inputs"

        val context = parameters.context + (External.PWD → userWorkDirectory)
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

        def runContainer = {
          val rootDirectory = containersDirectory / runId / "ROOT"

          def runCommand: FromContext[String] = {
            val variablesArgument = environmentVariables.map { case (name, variable) ⇒ s"""-e $name="${variable.from(context)}"""" }.mkString(" ")
            command.map(cmd ⇒ s"""${udocker.getAbsolutePath} run --workdir="$userWorkDirectory" $variablesArgument ${volumesArgument(volumes)} $runId $cmd""")
          }

          val executionResult = executeAll(
            taskWorkDirectory,
            udockerVariables(),
            errorOnReturnValue,
            returnValue,
            stdOut,
            stdErr,
            List(runCommand)
          )(parameters.copy(context = preparedContext))

          val retContext = external.fetchOutputFiles(outputs, preparedContext, outputPathResolver(rootDirectory), rootDirectory)
          external.cleanWorkDirectory(outputs, retContext, taskWorkDirectory)
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

  def containerName(uuid: String): String = {
    uuid.filter(_ != '-').map {
      case c if c < 'a' ⇒ (c - '0' + 'g').toChar
      case c            ⇒ c
    }
  }

}
