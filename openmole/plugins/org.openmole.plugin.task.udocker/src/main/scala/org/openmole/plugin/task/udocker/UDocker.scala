package org.openmole.plugin.task.udocker

import java.io.PrintStream
import java.util.UUID

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
import cats.data.{ Validated, ValidatedNel }
import io.circe.generic.extras.auto._
import io.circe.jawn.{ decode, decodeFile }
import io.circe.syntax._
import monocle.macros.Lenses
import org.openmole.core.outputredirection.OutputRedirection
import org.openmole.plugin.task.systemexec.{ commandLine, execute, executeAll }
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.openmole.tool.stream.withClosable

object UDocker {

  object LocalDockerImage {

    case class Layered(
      layers:       Vector[(Registry.Layer, File)],
      layersConfig: Vector[(Registry.LayerConfig, File)])
  }

  @Lenses case class LocalDockerImage(
    image:     String,
    tag:       String,
    content:   LocalDockerImage.Layered,
    imageJSON: String,
    manifest:  ImageManifestV2Schema1,
    container: Option[File]             = None
  ) {
    lazy val id = UUID.randomUUID().toString
  }

  def downloadImage(dockerImage: DockerImage, manifestDirectory: File, layersDirectory: File, timeout: Time)(implicit newFile: NewFile, threadProvider: ThreadProvider, outputRedirection: OutputRedirection) = {
    import Registry._

    def layerFile(layer: Layer) = layersDirectory / layer.digest
    lazy val manifestFile = manifestDirectory / s"${dockerImage.image}_${dockerImage.tag}"

    manifestFile.getParentFile.mkdirs()

    val manifestContent = util.Try(downloadManifest(dockerImage, timeout)) match {
      case util.Failure(e) ⇒
        if (manifestFile.exists) manifestFile.withLock { _ ⇒ manifestFile.content }
        else throw e
      case util.Success(c) ⇒
        manifestFile.withLock { _ ⇒ manifestFile.content = c }
        c
    }

    val manifestData = manifest(dockerImage, manifestContent)

    def localLayersFutures =
      for {
        m ← manifestData.toSeq.toVector
        l ← layers(m.value)
      } yield Future {
        val lf = layerFile(l)
        if (!lf.exists) {
          outputRedirection.output.println(s"Downloading docker layer: ${l.digest}")
          downloadLayer(dockerImage, l, layersDirectory, lf, timeout)
        }
        l → lf
      }

    val localLayers = localLayersFutures.map(f ⇒ Await.result(f, Duration.Inf)).reverse

    for {
      m ← manifestData
      history = m.value.history
      hist = Either.cond(history.isDefined, history.get, Err("No history field in Docker manifest"))
      v1 ← hist.map(_.head)
    } yield {
      val content = LocalDockerImage.Layered(localLayers, Vector.empty)
      // FIXME how reliable is it to assume config in first v1Compat string?
      val imageConfig = v1.v1Compatibility
      LocalDockerImage(dockerImage.image, dockerImage.tag, content, imageConfig, m.value)
    }
  }

  def buildRepoV2(repositoryDirectory: File, layersDirectory: File, localDockerImage: LocalDockerImage): Unit = {
    val imageRepositoryDirectory = repositoryDirectory /> localDockerImage.image /> localDockerImage.tag

    // move layer file in persistent/layers and create link to layer in persistent/repos
    for {
      layer ← localDockerImage.content.layers
    } {
      val layerFileInLayers = layersDirectory / layer._1.digest
      val layerFileInRepos = imageRepositoryDirectory / layer._1.digest

      // lock before existence tests to account for incomplete files
      layersDirectory.withLockInDirectory {
        if (!layerFileInLayers.exists()) layer._2 move layerFileInLayers
        if (!layerFileInRepos.exists()) {
          // clean potential broken link before recreating a valid one
          if (layerFileInRepos.isBrokenSymbolicLink) layerFileInRepos.delete()
          layerFileInRepos createLinkTo layerFileInLayers.getAbsolutePath
        }
      }
    }

    val imageId = s"${localDockerImage.image}:${localDockerImage.tag}"

    imageRepositoryDirectory / "manifest" content = localDockerImage.manifest.asJson.toString
    imageRepositoryDirectory / "TAG" content = s"$repositoryDirectory/$imageId"
    imageRepositoryDirectory / "v2" content = ""
  }

  /**
   * Indistinctly moves a file part of a layer (tar or json) to target destination in case it's not already present (based on SHA256 hash).
   *
   * @param extracted File containing extracted Docker image
   * @param layerElement tar or json config
   * @param elementSuffix <"layer"|"json">
   * @return newly created destination file
   */
  def moveLayerElement[T <: LayerElement](extracted: File, layerElement: String, elementSuffix: String, builder: (String) ⇒ T, layerDir: File): LayerAndConfig[T] = {

    import org.openmole.tool.hash._

    val fileName = layerElement.split("/").headOption.map(s ⇒ s"$s.$elementSuffix")

    val source = extracted / layerElement
    val sourceHash = source.hash(SHA256)

    val elementDestination: File = fileName match {
      case Some(f) ⇒ layerDir / f
      case None    ⇒ layerDir / s"$sourceHash.$elementSuffix"
    }

    val identical =
      if (elementDestination.exists()) {

        val hashCandidate = elementDestination.hash(SHA256)
        hashCandidate.equals(sourceHash)
      }
      else false

    if (!identical) (extracted / layerElement) move elementDestination

    builder(s"$sourceHash.$elementSuffix") → elementDestination
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

      val conent = LocalDockerImage.Layered(imageLayers, configs)

      LocalDockerImage(image, tag, conent, imageJSONString, manifest)
    }
  }

  def install(installDirectory: File) = {
    val udockerInstallDirectory = installDirectory /> "udocker"
    val udockerTarBall = udockerInstallDirectory / "udockertarball.tar.gz"
    val udockerFile = udockerInstallDirectory / "udocker"

    def retrieveResource(candidateFile: File, resourceName: String, executable: Boolean = false) =
      if (!candidateFile.exists()) {
        withClosable(this.getClass.getClassLoader.getResourceAsStream(resourceName))(_.copy(candidateFile))
        if (executable) candidateFile.setExecutable(true)
        candidateFile
      }

    retrieveResource(udockerTarBall, "udocker.tar.gz")
    retrieveResource(udockerFile, "udocker", executable = true)

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

  def runCommands(
    uDocker:           UDockerArguments,
    uDockerExecutable: File,
    uDockerVariables:  Vector[(String, String)],
    uDockerVolumes:    Vector[(String, String)],
    container:         String,
    commands:          Seq[String],
    captureOutput:     Boolean                  = false,
    captureError:      Boolean                  = false,
    displayOutput:     Boolean                  = true,
    displayError:      Boolean                  = true,
    stdOut:            PrintStream              = System.out,
    stdErr:            PrintStream              = System.err)(implicit newFile: NewFile) = newFile.withTmpDir { tmpDirectory ⇒
    val runInstall = commands.map {
      ic ⇒
        uDockerRunCommand(
          uDocker.user,
          Vector.empty,
          uDockerVolumes,
          userWorkDirectory(uDocker),
          uDockerExecutable,
          container,
          ic: Id[String])
    }

    executeAll(
      tmpDirectory,
      uDockerVariables,
      commands = runInstall.toList,
      captureOutput = captureOutput,
      captureError = captureError,
      displayOutput = displayOutput,
      displayError = displayError,
      stdOut = stdOut,
      stdErr = stdErr
    )
  }

  def createContainer(uDocker: UDockerArguments, uDockerExecutable: File, containerDirectory: File, uDockerVariables: Vector[(String, String)], uDockerVolumes: Vector[(String, String)], imageId: String)(implicit newFile: NewFile) = newFile.withTmpDir { tmpDirectory ⇒

    val id =
      uDocker.localDockerImage.container match {
        case None ⇒

          val cl = commandLine(s"${uDockerExecutable.getAbsolutePath} create $imageId")
          execute(cl, tmpDirectory, uDockerVariables, captureOutput = true, captureError = true, displayOutput = false, displayError = false).output.get.split("\n").head
        case Some(directory) ⇒
          val name = containerName(UUID.randomUUID().toString) //.take(10)
          directory.copy(containerDirectory / name)
          name
      }

    uDocker.mode.foreach { mode ⇒
      val cl = commandLine(s"""${uDockerExecutable.getAbsolutePath} setup --execmode=$mode $id""")
      execute(cl, tmpDirectory, uDockerVariables, captureOutput = true, captureError = true)
    }

    id
  }

  def imageId(uDocker: UDockerArguments) = s"${uDocker.localDockerImage.image}:${uDocker.localDockerImage.tag}"

  def uDockerRunCommand(
    user:                 Option[String],
    environmentVariables: Vector[(String, String)],
    volumes:              Vector[MountPoint],
    workDirectory:        String,
    uDocker:              File,
    runId:                String,
    command:              String): String = {

    def volumesArgument(volumes: Vector[MountPoint]) = volumes.map { case (host, container) ⇒ s"""-v "$host":"$container"""" }.mkString(" ")

    val userArgument = user match {
      case None    ⇒ ""
      case Some(x) ⇒ s"""--user="$x""""
    }

    val variablesArgument = environmentVariables.map { case (name, variable) ⇒ s"""-e ${name}="${variable}"""" }.mkString(" ")

    s"""${uDocker.getAbsolutePath} run --workdir="$workDirectory" $userArgument  $variablesArgument ${volumesArgument(volumes)} $runId $command"""
  }

  // TODO refactor
  type LayerAndConfig[T <: LayerElement] = (T, File)

  case class DockerImageData(imageAndTag: Option[Array[String]], manifest: String)
  case class ValidDockerImageData(imageAndTag: (String, String), manifest: String)

  def validateNonEmpty(imageAndTag: Option[Array[String]]): ValidatedNel[Err, (String, String)] = imageAndTag match {
    case Some(Array(image, tag)) ⇒ Validated.valid((image, tag))
    case _                       ⇒ Validated.invalidNel(Err("Could not parse image and tag from Docker image's metadata"))
  }

  def validateManifestName(manifestOpt: String): ValidatedNel[Err, String] = manifestOpt match {
    case manifest if !manifest.isEmpty ⇒ Validated.valid(manifest)
    case _                             ⇒ Validated.invalidNel(Err("Could not parse manifest name from Docker image's metadata"))
  }

  def validateDockerImage(data: DockerImageData) = {
    val validImageAndTag = validateNonEmpty(data.imageAndTag)
    val validManifest = validateManifestName(data.manifest)

    (validImageAndTag |@| validManifest).map(ValidDockerImageData)
  }
}
