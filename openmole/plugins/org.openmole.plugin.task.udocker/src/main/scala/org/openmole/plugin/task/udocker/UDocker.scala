package org.openmole.plugin.task.udocker

import java.util.UUID

import org.openmole.core.preference._
import org.openmole.core.threadprovider._
import org.openmole.core.workspace._
import org.openmole.plugin.task.udocker.DockerMetadata._
import org.openmole.plugin.task.udocker.Registry._
import org.openmole.plugin.task.udocker.UDockerTask._
import squants._

import scala.concurrent._
import scala.concurrent.duration._
import cats.implicits._
import cats._
import io.circe.generic.extras.auto._
import io.circe.jawn.{ decode, decodeFile }
import io.circe.syntax._
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.plugin.task.systemexec.{ commandLine, execute, executeAllNoExpand }
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.openmole.tool.stream.withClosable

object UDocker {
  def downloadImage(dockerImage: DockerImage, timeout: Time)(implicit newFile: NewFile, workspace: Workspace, threadProvider: ThreadProvider) = {
    import Registry._

    def layerFile(workspace: Workspace, layer: Layer) = layersDirectory(workspace) / layer.digest

    val m = manifest(dockerImage, timeout)
    val lDirectory = layersDirectory(workspace)

    def localLayersFutures =
      for {
        manif ← m.toSeq.toVector
        l ← layers(manif.value)
      } yield Future {
        val lf = layerFile(workspace, l)
        if (!lf.exists) downloadLayer(dockerImage, l, lDirectory, lf, timeout)
        l → lf
      }

    val localLayers = localLayersFutures.map(f ⇒ Await.result(f, Duration.Inf))

    for {
      manif ← m
      history = manif.value.history
      hist = Either.cond(history.isDefined, history.get, Err("No history field in Docker manifest"))
      v1 ← hist.map(_.head)
    } yield {

      // FIXME how reliable is it to assume config in first v1Compat string?
      val imageConfig = v1.v1Compatibility
      LocalDockerImage(dockerImage.image, dockerImage.tag, localLayers, Vector.empty, imageConfig, manif.value)
    }
  }

  def buildRepoV2(localDockerImage: LocalDockerImage)(implicit workspace: Workspace): Unit = {
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

    imageRepositoryDirectory / "manifest" content = localDockerImage.manifest.asJson.toString
    imageRepositoryDirectory / "TAG" content = s"$reposDir/$imageId"
    imageRepositoryDirectory / "v2" content = ""
  }

  /**
   * Mimic `docker load -i` to reconstruct a registry entry for a previously saved image (using `docker save`).
   *
   * Assume v1.x Image JSON format and registry protocol v2 Schema 1
   */
  def loadImage(dockerImage: SavedDockerImage)(implicit newFile: NewFile, workspace: Workspace): Either[Err, LocalDockerImage] = newFile.withTmpDir { extractedImage ⇒

    import org.openmole.tool.tar._

    dockerImage.file.extract(extractedImage)

    val manifestContent = (extractedImage / "manifest.json").content
    val topLevelManifests = decode[List[TopLevelImageManifest]](manifestContent)
    val topLevelImageManifest = topLevelManifests.map(_.head)

    val validatedInfo = for {
      layersNames ← topLevelImageManifest.map(_.Layers.distinct)
      topLevelManifest ← topLevelManifests.map(_.head)
    } yield {

      val layersConfigs = layersNames.map(path ⇒ s"${path.split("/").head}/json")

      val imageLayers =
        layersNames.map(l ⇒ moveLayerElement[Layer](extractedImage, l, "layer", Layer.apply, layersDirectory(workspace)))
      val configs = layersConfigs.map(c ⇒ moveLayerElement[LayerConfig](extractedImage, c, "json", LayerConfig.apply, layersDirectory(workspace)))

      val imageAndTag = topLevelManifest.RepoTags.map(_.split(":")).headOption
      val imageJSONName = topLevelManifest.Config

      validateDockerImage(DockerImageData(imageAndTag, imageJSONName)) bimap (
        errors ⇒ errors.foldLeft(Err("\n")) { case (acc, err) ⇒ acc + err },
        validImage ⇒ {
          val ValidDockerImageData((image: String, tag: String), imageJSONName) = validImage
          val imageJSONString = (extractedImage / imageJSONName).content
          (image, tag, imageJSONString, imageLayers, configs)
        }
      )
    }

    for {
      v ← validatedInfo.leftMap(l ⇒ Err(l.getMessage))
      infos ← v.toEither
      (image, tag, imageJSONString, imageLayers, configs) = infos
      imageJSON ← decode[ImageJSON](imageJSONString).leftMap(l ⇒ Err(l.toString))
    } yield {

      val (fsLayers, history) = imageLayers.zip(configs).reverse.map {
        case ((imageLayer, _), (_, layerConfigFile)) ⇒
          val layerDigest = DockerMetadata.Digest(imageLayer.digest)
          val imageJSON = decodeFile[ImageJSON](layerConfigFile)

          (layerDigest, imageJSON)
      }.toList.unzip

      val manifest = DockerMetadata.ImageManifestV2Schema1(
        name = Some(image),
        tag = Some(tag),
        architecture = Some(imageJSON.architecture),
        fsLayers = Some(fsLayers),
        history = history.sequence.toOption.map(histList ⇒ histList.map(h ⇒ DockerMetadata.V1History(h.asJson.toString))),
        schemaVersion = Some(1)
      )

      LocalDockerImage(image, tag, imageLayers, configs, imageJSONString, manifest)
    }
  }

  def toLocalImage(containerImage: ContainerImage)(implicit preference: Preference, newFile: NewFile, workspace: Workspace, threadProvider: ThreadProvider): Either[Err, LocalDockerImage] =
    containerImage match {
      case i: DockerImage      ⇒ downloadImage(i, preference(RegistryTimeout))
      case i: SavedDockerImage ⇒ loadImage(i)
    }

  def install(executionContext: TaskExecutionContext, installDirectory: File) = {
    val udockerInstallDirectory = installDirectory /> "udocker"
    val udockerTarBall = udockerInstallDirectory / "udockertarball.tar.gz"
    val udockerFile = udockerInstallDirectory / "udocker"

    def retrieveResource(candidateFile: File, resourceName: String, executable: Boolean = false) =
      if (!candidateFile.exists()) {
        withClosable(this.getClass.getClassLoader.getResourceAsStream(resourceName))(_.copy(candidateFile))
        if (executable) candidateFile.setExecutable(true)
        candidateFile
      }

    // lock in any case since file.exists() might return for an incomplete file
    executionContext.lockRepository.withLock(installLockKey) {
      retrieveResource(udockerTarBall, "udocker.tar.gz")
      retrieveResource(udockerFile, "udocker", executable = true)
    }

    (udockerFile, udockerInstallDirectory, udockerTarBall)
  }

  def environmentVariables(
    tmpDirectory:        File,
    homeDirectory:       File,
    containersDirectory: File,
    repositoryDirectory: File,
    layersDirectory:     File,
    installDirectory:    File,
    tarball:             File,
    logLevel:            Int  = 1) =
    Vector(
      "UDOCKER_TMPDIR" → tmpDirectory.getAbsolutePath,
      "UDOCKER_LOGLEVEL" → logLevel.toString,
      "HOME" → homeDirectory.getAbsolutePath,
      "UDOCKER_CONTAINERS" → containersDirectory.getAbsolutePath,
      "UDOCKER_REPOS" → repositoryDirectory.getAbsolutePath,
      "UDOCKER_LAYERS" → layersDirectory.getAbsolutePath,
      "UDOCKER_DIR" → installDirectory.getAbsolutePath,
      "UDOCKER_TARBALL" → tarball.getAbsolutePath
    )

  def newContainer(uDocker: UDockerArguments, uDockerExecutable: File, uDockerVariables: Vector[(String, String)], uDockerVolumes: Vector[(String, String)], imageId: String)(implicit newFile: NewFile) = newFile.withTmpDir { tmpDirectory ⇒
    val name = containerName(UUID.randomUUID().toString).take(10)

    val cl = commandLine(s"${uDockerExecutable.getAbsolutePath} create --name=$name $imageId")
    execute(cl.toArray, tmpDirectory, uDockerVariables, returnOutput = true, returnError = true)

    import cats._

    if (!uDocker.installCommands.isEmpty) {
      val runInstall = uDocker.installCommands.map {
        ic ⇒
          uDockerRunCommand(
            uDocker.uDockerUser,
            Vector.empty,
            uDockerVolumes,
            userWorkDirectory(uDocker),
            uDockerExecutable,
            name,
            ic: Id[String])
      }

      executeAllNoExpand(
        tmpDirectory,
        uDockerVariables,
        true,
        None,
        None,
        None,
        runInstall.toList
      )
    }

    name
  }
}
