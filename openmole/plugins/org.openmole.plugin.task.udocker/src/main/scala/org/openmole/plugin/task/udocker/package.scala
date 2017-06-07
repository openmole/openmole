package org.openmole.plugin.task

import monocle.Lens
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.NewFile
import org.openmole.plugin.task.udocker.Registry.LayerElement
import org.openmole.tool.file._

package udocker {

  trait UDockerPackage {

    lazy val reuseContainer =
      new {
        def :=[T: ReuseContainer](b: Boolean) =
          implicitly[ReuseContainer[T]].reuseContainer.set(b)
      }

  }
}

package object udocker extends UDockerPackage {

  object ContainerImage {
    implicit def fileToContainerImage(f: java.io.File) = SavedDockerImage(f)
    implicit def strsingToContainerImage(s: String) = DockerImage(s)
  }

  sealed trait ContainerImage
  case class DockerImage(image: String, tag: String = "latest", registry: String = "https://registry-1.docker.io") extends ContainerImage
  case class SavedDockerImage(file: java.io.File) extends ContainerImage

  trait ReuseContainer[T] {
    def reuseContainer: Lens[T, Boolean]
  }

  import cats.data._
  import cats.implicits._

  case class DockerImageData(imageAndTag: Option[Array[String]], manifest: String)
  case class ValidDockerImageData(imageAndTag: (String, String), manifest: String)

  // FIXME turn to plain String if useless
  case class Err(msg: String)

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
  def moveLayerElement[T <: LayerElement](extracted: File, layerElement: String, elementSuffix: String, builder: (String) ⇒ T)(implicit newFile: NewFile, fileService: FileService): LayerAndConfig[T] = {

    import org.openmole.tool.hash._

    val fileName = layerElement.split("/").headOption.map(s ⇒ s"$s.$elementSuffix")
    val elementDestination: File = fileName match {
      case Some(f) ⇒ newFile.baseDir / f
      case None    ⇒ newFile.newFile(s"udocker$elementSuffix")
    }

    fileService.deleteWhenGarbageCollected(elementDestination)

    val source = extracted / layerElement
    val sourceHash = source.hash(SHA256)

    val identical =
      if (elementDestination.exists()) {

        val hashCandidate = elementDestination.hash(SHA256)
        hashCandidate.equals(sourceHash)
      }
      else false

    if (!identical) (extracted / layerElement) move elementDestination

    builder(s"$sourceHash.$elementSuffix") → elementDestination
  }

}
