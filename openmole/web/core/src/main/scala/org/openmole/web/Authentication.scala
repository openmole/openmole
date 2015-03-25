package org.openmole.web

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 12/1/13
 * Time: 5:40 PM
 */

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.openmole.core.tools.service.Logger
import org.openmole.core.workspace.Workspace
import org.apache.commons.codec.binary.Base64
import javax.servlet.http.HttpServletRequest

import scala.concurrent.stm._
import scala.util.{ Failure, Success, Try }

case class Key(hash: String, start: Long, end: Long) {
  def isValid = {
    val cTime = java.util.Calendar.getInstance.getTimeInMillis
    start <= cTime && end > cTime
  }
}

class TokenHandler extends DataHandler[String, Key] {

  private def clean[T](f: ⇒ T): T = {
    atomic { implicit t ⇒
      val outdated = map.filter { case (_, v) ⇒ !v.isValid }.map { case (k, _) ⇒ k }
      outdated.foreach(map.remove)
    }
    f
  }

  override def add(key: String, data: Key) = clean { super.add(key, data) }
  override def remove(key: String) = clean { super.remove(key) }
  override def get(key: String) = clean { super.get(key) }

}

class InvalidPasswordException(cause: String) extends Exception(cause)

object Authentication extends Logger

trait Authentication { self ⇒
  import Authentication.Log._

  /// FIXME remove outdated keys
  private val keyStorage = new TokenHandler

  def issueKey(pwH: String)(implicit r: HttpServletRequest): Try[String] = {
    if (RESTServer.isPasswordCorrect(pwH)) {
      val signingKey = new SecretKeySpec(pwH.getBytes, "HmacSHA256")
      val mac = Mac.getInstance("HmacSHA256")
      mac.init(signingKey)
      val start = java.util.Calendar.getInstance().getTimeInMillis
      val end = start + (24 * 60 * 60 * 1000)
      val rawHmac = mac.doFinal((r.getRemoteHost + Workspace.sessionUUID + start + end) getBytes ())
      val hash = new String(Base64.encodeBase64(rawHmac))
      keyStorage.add(hash, Key(hash, start, end))
      Success(hash)
    }
    else {
      logger.info("Submitted password was incorrect")
      Failure(new InvalidPasswordException("Submitted password was incorrect"))
    }
  }

  def checkKey(key: String): Boolean = {
    keyStorage get key match {
      case Some(k) ⇒ {
        logger.info(s"key is valid: ${k.isValid}")
        logger.info(s"key matches key given: ${k.hash == key}")
        logger.info(s"stored key: ${k.hash}")
        logger.info(s"given key: $key")

        k.isValid && k.hash == key
      }
      case _ ⇒ false
    }
  }
}

