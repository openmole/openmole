package org.openmole.web

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 12/1/13
 * Time: 5:40 PM
 */

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.openmole.core.workspace.Workspace

import scala.collection.mutable.HashMap
import akka.actor.ActorSystem
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory
import org.scalatra.ScalatraBase
import javax.servlet.http.HttpServletRequest
import org.openmole.web.cache.DataHandler
import org.openmole.web.mole.MoleHandling

trait Authentication { self ⇒

  //private val logger = LoggerFactory.getLogger(getClass)

  def system: ActorSystem

  class InvalidPasswordException(cause: String) extends Exception(cause)

  case class Key(hash: String, start: Long, end: Long) {
    def isValid = {
      val cTime = java.util.Calendar.getInstance.getTimeInMillis
      start <= cTime && end > cTime
    }
  }

  private val keyStorage = new DataHandler[String, Key](system)

  def issueKey(pwH: String)(implicit r: HttpServletRequest): String = {
    if (Workspace.passwordIsCorrect(pwH)) { //TODO this is probably terribly unsafe
      val signingKey = new SecretKeySpec(pwH.getBytes, "HmacSHA256")
      val mac = Mac.getInstance("HmacSHA256")
      mac.init(signingKey)
      val start = java.util.Calendar.getInstance().getTimeInMillis
      val end = start + (24 * 60 * 60 * 1000)
      val rawHmac = mac.doFinal((r.getRemoteHost + Workspace.sessionUUID + start + end) getBytes ())

      val hash = new String(Base64.encodeBase64(rawHmac))

      keyStorage.add(r.getRemoteHost, Key(hash, start, end))

      hash
    }
    else {
      //logger.info("Submitted password was incorrect")
      println("Submitted password was incorrect")
      throw new InvalidPasswordException("Submitted password was incorrect")
    }
  }

  def requireAuth[T](key: ⇒ Option[String])(success: ⇒ T)(fail: ⇒ T = { throw new InvalidPasswordException("Api-key is not valid") })(implicit r: HttpServletRequest): T = {
    if (key map (checkKey(_, r.getRemoteHost)) getOrElse false) success else fail
  }

  def checkKey(key: String, hostname: String): Boolean = {
    keyStorage get hostname match {
      case Some(k) ⇒ {
        println(s"key is valid: ${k.isValid}")
        println(s"key matches key given: ${k.hash == key}")
        println(s"stored key: ${k.hash}")
        println(s"given key: $key")

        k.isValid && k.hash == key
      }
      case _ ⇒ false
    }
  }
}
