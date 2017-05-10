package org.openmole.plugin.task

import monocle.Lens

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

  case class DockerImageData(imageAndTag: List[String], manifest: Option[String])
  case class ValidDockerImageData(imageAndTag: (String, String), manifest: String)

  // FIXME turn to plain String if useless
  case class Err(msg: String)

  def validateNonEmpty(imageAndTag: List[String]): ValidatedNel[Err, (String, String)] = imageAndTag match {
    case List(image, tag) ⇒ Validated.valid((image, tag))
    case _                ⇒ Validated.invalidNel(Err("Could not parse image and tag from Docker image's metadata"))
  }

  def validateManifestName(manifestOpt: Option[String]): ValidatedNel[Err, String] = manifestOpt match {
    case Some(manifest) ⇒ Validated.valid(manifest)
    case _              ⇒ Validated.invalidNel(Err("Could not parse manifest name from Docker image's metadata"))
  }

  def validateDockerImage(data: DockerImageData) = {
    val validImageAndTag = validateNonEmpty(data.imageAndTag)
    val validManifest = validateManifestName(data.manifest)

    (validImageAndTag |@| validManifest).map(ValidDockerImageData)
  }

}
