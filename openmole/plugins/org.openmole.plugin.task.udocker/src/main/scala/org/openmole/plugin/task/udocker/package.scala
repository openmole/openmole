package org.openmole.plugin.task

import monocle.macros.Lenses
import org.openmole.plugin.task.container._
import org.openmole.plugin.task.external._
import org.openmole.tool.file._

package udocker {

  trait UDockerPackage {
    lazy val ContainerTask = UDockerTask
  }

}

package object udocker extends UDockerPackage {

  object ContainerImage {
    implicit def fileToContainerImage(f: java.io.File) = {
      def compressed = f.getName.endsWith(".tgz") || f.getName.endsWith(".gz")
      SavedDockerImage(f, compressed)
    }
    implicit def stringToContainerImage(s: String) =
      if (s.contains(":")) {
        val Vector(image, tag) = s.split(":").toVector
        DockerImage(image, tag)
      }
      else DockerImage(s)

  }

  sealed trait ContainerImage
  case class DockerImage(image: String, tag: String = "latest", registry: String = "https://registry-1.docker.io") extends ContainerImage
  case class SavedDockerImage(file: java.io.File, compressed: Boolean) extends ContainerImage

  /**
   * Trait for either string scripts or script file runnable in tasks based on the container task
   */
  object RunnableScript {
    implicit def stringToRunnableScript(s: String): RunnableScript = RawScript(s)
    implicit def fileToRunnableScript(f: File): RunnableScript = FileScript(f)

    def content(script: RunnableScript): String = {
      script match {
        case RawScript(s)  ⇒ s
        case FileScript(f) ⇒ f.content
      }
    }
  }

  sealed trait RunnableScript
  case class RawScript(rawscript: String) extends RunnableScript
  case class FileScript(file: File) extends RunnableScript

  import cats.implicits._

  case class Err(msg: String) {
    def +(o: Err) = Err(msg + o.msg)
  }

  def containerName(uuid: String): String = {
    uuid.filter(_ != '-').map {
      case c if c < 'a' ⇒ (c - '0' + 'g').toChar
      case c            ⇒ c
    }
  }

  type FileInfo = (External.DeployedFile, File)
  type VolumeInfo = (File, String)
  type MountPoint = (String, String)
  type ContainerId = String

  @Lenses case class UDockerArguments(
    localDockerImage:     UDocker.LocalDockerImage,
    environmentVariables: Vector[EnvironmentVariable] = Vector.empty,
    hostFiles:            Vector[HostFile]            = Vector.empty,
    workDirectory:        Option[String]              = None,
    reuseContainer:       Boolean                     = true,
    user:                 Option[String]              = None,
    mode:                 Option[String]              = None,
    noSeccomp:            Boolean                     = false)

  def userWorkDirectory(uDocker: UDockerArguments) = {
    import io.circe.generic.extras.auto._
    import io.circe.jawn.decode
    import org.openmole.plugin.task.udocker.DockerMetadata._

    val imageJSONE = decode[ImageJSON](uDocker.localDockerImage.imageJSON)

    val dockerWorkDirectory =
      for {
        imageJSON ← imageJSONE
        workDir = imageJSON.config.WorkingDir
      } yield if (workDir.isEmpty) "/" else workDir

    uDocker.workDirectory.getOrElse(dockerWorkDirectory.getOrElse("/"))
  }

}
