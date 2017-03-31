package org.openmole.plugin.task

import monocle.Lens

package udocker {

  trait UDockerPackage {

    lazy val sharedContainer =
      new {
        def :=[T: SharedContainer](b: Boolean) =
          implicitly[SharedContainer[T]].sharedContainer.set(b)
      }

  }
}

package object udocker extends UDockerPackage {

  object ContainerImage {
    implicit def fileToContainerImage(f: java.io.File): SavedDockerImage = SavedDockerImage(f)
    implicit def stringToContainerImage(s: String): DockerImage = DockerImage(s)
  }

  sealed trait ContainerImage
  case class DockerImage(image: String, registry: String = "https://registry-1.docker.io") extends ContainerImage
  case class SavedDockerImage(file: java.io.File) extends ContainerImage

  trait SharedContainer[T] {
    def sharedContainer: Lens[T, Boolean]
  }
}
