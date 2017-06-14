package org.openmole.plugin.task.udocker

import org.openmole.tool.file._
import org.openmole.tool.tar._
import org.openmole.tool.hash._
import org.openmole.plugin.task.udocker.DockerMetadata._
import io.circe.generic.extras.auto._
import io.circe.jawn.decodeFile
import cats.implicits._

object TestExtractArchive extends App {

  val tmp = File("/tmp")
  val archive = tmp / "lam.tar"
  val extracted = tmp /> "extract"

  val layersDirectory = tmp /> "layers"

  extracted.recursiveDelete
  layersDirectory.recursiveDelete

  archive.extract(extracted)

  def copyLayers(layersName: Seq[String]) =
    layersName.foreach {
      l ⇒
        val hash = (extracted / l).hash(SHA256)
        (extracted / l) copy (layersDirectory / s"sha256:$hash")
    }

  val manifests = decodeFile[List[TopLevelImageManifest]](extracted / "manifest.json").toOption.sequenceU

  val imageTags = for {
    manifest ← manifests
    m ← manifest
  } yield {
    val layersName = m.Layers

    copyLayers(layersName)

    m.RepoTags.head.split(":")
  }

  val Array(image, tag) = imageTags.head
  println(image)
  println(tag)

  //  def extractArchive(savedDockerImage: SavedDockerImage) = {
  //    val layerDirectory = arc
  //  }
}
