package org.openmole.gui.client.core.files

import rx._
import org.openmole.gui.ext.data.SafePath

object HashService {

  val hashes: Var[Map[SafePath, Option[String]]] = Var(Map())

  def set(safePath: SafePath, hash: String) = {
    hashes.update(hashes.now.updated(safePath, Some(hash)))
  }

  def latestHash(safePath: SafePath) = hashes.now.find(h ⇒ h._1 == safePath).flatMap { x ⇒ x._2 }

  def isSimilarToLatest(safePath: SafePath, hash: String) = latestHash(safePath) == Some(hash)
}
