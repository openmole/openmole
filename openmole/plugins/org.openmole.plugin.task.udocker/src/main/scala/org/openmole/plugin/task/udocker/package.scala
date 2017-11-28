package org.openmole.plugin.task

import java.util.UUID

import cats.Applicative
import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.task.TaskExecutionContext
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.plugin.task.external.External
import org.openmole.plugin.task.systemexec._
import org.openmole.plugin.task.udocker.DockerMetadata.{ ContainerID, ImageManifestV2Schema1 }
import org.openmole.plugin.task.udocker.Registry.LayerElement
import org.openmole.tool.cache.{ CacheKey, WithInstance }
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.openmole.tool.lock._

package udocker {

  trait UDockerPackage {

    lazy val reuseContainer =
      new {
        def :=[T: ReuseContainer](b: Boolean) =
          implicitly[ReuseContainer[T]].reuseContainer.set(b)
      }

    lazy val uDockerUser =
      new {
        def :=[T: UDockerUser](b: String) =
          implicitly[UDockerUser[T]].uDockerUser.set(Some(b))
      }

  }
}

package object udocker extends UDockerPackage {

  object ContainerImage {
    implicit def fileToContainerImage(f: java.io.File) = SavedDockerImage(f)
    implicit def stringToContainerImage(s: String) = DockerImage(s)
  }

  sealed trait ContainerImage
  case class DockerImage(image: String, tag: String = "latest", registry: String = "https://registry-1.docker.io") extends ContainerImage
  case class SavedDockerImage(file: java.io.File) extends ContainerImage

  trait ReuseContainer[T] {
    def reuseContainer: Lens[T, Boolean]
  }

  trait UDockerUser[T] {
    def uDockerUser: Lens[T, Option[String]]
  }

  import cats.data._
  import cats.implicits._

  case class DockerImageData(imageAndTag: Option[Array[String]], manifest: String)
  case class ValidDockerImageData(imageAndTag: (String, String), manifest: String)

  case class Err(msg: String) {
    def +(o: Err) = Err(msg + o.msg)
  }

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

  // TODO refactor
  type LayerAndConfig[T <: LayerElement] = (T, File)

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

  def containerName(uuid: String): String = {
    uuid.filter(_ != '-').map {
      case c if c < 'a' ⇒ (c - '0' + 'g').toChar
      case c            ⇒ c
    }
  }

  type FileInfo = (External.ToPut, File)
  type HostFile = (String, Option[String])
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String

  // TODO review data structure
  case class LocalDockerImage(
    image:        String,
    tag:          String,
    layers:       Vector[(Registry.Layer, File)],
    layersConfig: Vector[(Registry.LayerConfig, File)],
    imageJSON:    String,
    manifest:     ImageManifestV2Schema1
  ) {
    lazy val id = UUID.randomUUID().toString
  }

  @Lenses case class UDocker(
    localDockerImage:     LocalDockerImage,
    command:              FromContext[String],
    environmentVariables: Vector[(String, FromContext[String])] = Vector.empty,
    hostFiles:            Vector[(String, Option[String])]      = Vector.empty,
    installCommands:      Vector[String]                        = Vector.empty,
    workDirectory:        Option[String]                        = None,
    reuseContainer:       Boolean                               = true,
    uDockerUser:          Option[String]                        = None
  )

  def uDockerRunCommand[A[_]: Applicative](
    user:                 Option[String],
    environmentVariables: Vector[(String, String)],
    volumes:              Vector[MountPoint],
    workDirectory:        String,
    uDocker:              File,
    runId:                String,
    command:              A[String]): A[String] = {

    def volumesArgument(volumes: Vector[MountPoint]) = volumes.map { case (host, container) ⇒ s"""-v "$host":"$container"""" }.mkString(" ")

    val userArgument = user match {
      case None    ⇒ ""
      case Some(x) ⇒ s"""--user="$x""""
    }

    val variablesArgument = environmentVariables.map { case (name, variable) ⇒ s"""-e ${name}="${variable}"""" }.mkString(" ")

    command.map { cmd ⇒ s"""${uDocker.getAbsolutePath} run --workdir="$workDirectory" $userArgument  $variablesArgument ${volumesArgument(volumes)} $runId $cmd""" }
  }

  def userWorkDirectory(uDocker: UDocker) = {
    import io.circe.generic.extras.auto._
    import io.circe.jawn.{ decode, decodeFile }
    import io.circe.syntax._
    import org.openmole.plugin.task.udocker.DockerMetadata._

    val imageJSONE = decode[ImageJSON](uDocker.localDockerImage.imageJSON)

    val dockerWorkDirectory =
      for {
        imageJSON ← imageJSONE
        workDir = imageJSON.config.WorkingDir
      } yield if (workDir.isEmpty) "/" else workDir

    uDocker.workDirectory.getOrElse(dockerWorkDirectory.getOrElse("/"))
  }

  lazy val installLockKey = LockKey()
  lazy val containerPoolKey = CacheKey[WithInstance[ContainerID]]()

  def installUDocker(executionContext: TaskExecutionContext, installDirectory: File) = {
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

  def uDockerEnvironmentVariables(
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

  def newContainer(uDocker: UDocker, uDockerExecutable: File, uDockerVariables: Vector[(String, String)], uDockerVolumes: Vector[(String, String)], imageId: String)(implicit newFile: NewFile) = newFile.withTmpDir { tmpDirectory ⇒
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
