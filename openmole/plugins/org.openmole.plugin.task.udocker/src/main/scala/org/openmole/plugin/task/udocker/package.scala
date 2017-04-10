package org.openmole.plugin.task

import java.util.UUID
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
}
