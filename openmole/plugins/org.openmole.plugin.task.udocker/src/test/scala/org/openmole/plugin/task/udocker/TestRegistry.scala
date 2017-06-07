package org.openmole.plugin.task.udocker

import Registry._
import squants.time.TimeConversions._
import org.openmole.tool.file._
import io.circe.generic.extras.auto._
import io.circe.syntax._
import org.openmole.plugin.task.udocker.DockerMetadata._

object TestRegistry extends App {

  val image = DockerImage("ubuntu", "latest")
  val m = manifest(image, 1 minutes)
  val l = layers(m.value)

  val localRepo = File("/tmp/") /> "repository"
  val localLayers = localRepo /> "layers"
  val localRepoRepo = localRepo /> "repo" /> image.image /> image.tag

  val relativePathToLayers = localRepoRepo.relativize(localLayers)

  l.foreach { l ⇒ blob(image, l, localLayers / l.digest, 1 minutes) }
  l.foreach { l ⇒ (localRepoRepo / l.digest) createLinkTo (relativePathToLayers.toFile / l.digest getPath) }

  localRepoRepo / "manifest" content = m.value.asJson.toString
  localRepoRepo / "TAG" content = s"$localRepo/${image.image}:${image.tag}"

}
