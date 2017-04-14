package org.openmole.plugin.task.udocker

import org.openmole.tool.file._
import org.openmole.tool.tar._
import org.openmole.tool.hash._
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._

object TestExtractArchive extends App {

  val tmp = File("/tmp")
  val archive = tmp / "lam.tar"
  val extracted = tmp /> "extract"

  val layersDirectory = tmp /> "layers"

  extracted.recursiveDelete
  layersDirectory.recursiveDelete

  archive.extract(extracted)

  val manifest = parse(extracted / "manifest.json")

  implicit def formats = org.json4s.DefaultFormats

  val layersName = (manifest \ "Layers").children.flatMap(_.extract[Array[String]])

  layersName.foreach {
    l ⇒
      val hash = (extracted / l).hash(SHA256)
      (extracted / l) copy (layersDirectory / s"sha256:$hash")
  }

  val Array(image, tag) = (manifest \\ "RepoTags").children.map(_.extract[String]).head.split(":")
  println(image)
  println(tag)

  //  def extractArchive(savedDockerImage: SavedDockerImage) = {
  //    val layerDirectory = arc
  //  }
}
