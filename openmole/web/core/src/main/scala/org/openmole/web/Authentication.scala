package org.openmole.web

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 12/1/13
 * Time: 5:40 PM
 */

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.HashMap
import akka.actor.ActorSystem
import org.apache.commons.codec.binary.Base64

trait Authentication {

  def system: ActorSystem

  case class Key(hash: String, start: Long, end: Long) {
    def isValid = {
      val cTime = java.util.Calendar.getInstance.getTimeInMillis
      start <= cTime && end > cTime
    }
  }

  private val keyStorage = new DataHandler[String, Key](system)

  def issueKey(pwH: String, hostname: String): String = {
    if (Workspace.passwordIsCorrect(pwH)) { //TODO this is probably terribly unsafe
      val signingKey = new SecretKeySpec(pwH.getBytes, "HmacSHA256")
      val mac = Mac.getInstance("HmacSHA256")
      mac.init(signingKey)
      val rawHmac = mac.doFinal((hostname + Workspace.uniqueID) getBytes ())

      val hash = new String(Base64.encodeBase64(rawHmac))
      val start = java.util.Calendar.getInstance().getTimeInMillis
      val end = start + (24 * 60 * 60 * 1000)

      keyStorage.add(hostname, Key(hash, start, end))

      hash
    }
    else ???
  }

  def checkKey(key: String, hostname: String): Boolean = {
    keyStorage get hostname match {
      case Some(k) ⇒ k.isValid && k.hash == key
      case _       ⇒ false
    }

    true
  }
}
