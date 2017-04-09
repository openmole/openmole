package org.openmole.plugin.task.udocker

import Registry._
import squants.time.TimeConversions._
import org.openmole.tool.file._
import org.json4s.jackson.JsonMethods._

object TestRegistry extends App {

  val image = Image("library/ubuntu", "latest")
  val m = manifest(image, 1 minutes)
  val l = layers(m)

  val localRepo = File("/tmp/") /> "repository"
  val localLayers = localRepo /> "layers"
  val localRepoRepo = localRepo /> "repo" /> image.image /> image.tag

  val relativePathToLayers = localRepoRepo.relativize(localLayers)

  l.foreach { l ⇒ blob(l, localLayers / l.digest, 1 minutes) }
  l.foreach { l ⇒ (localRepoRepo / l.digest) createLink (relativePathToLayers.toFile / l.digest getPath) }
  localRepoRepo / "manifest" content = compact(m.value)
  localRepoRepo / "TAG" content = s"$localRepo/${image.image}:${image.tag}"

}
