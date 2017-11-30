package org.openmole.rest.server

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 12/1/13
 * Time: 5:40 PM
 */

import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

import org.openmole.core.workspace.Workspace
import org.apache.commons.codec.binary.Base64
import javax.servlet.http.HttpServletRequest

import org.openmole.core.preference.Preference
import org.openmole.tool.collection._
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.logger.JavaLogger

import scala.concurrent.stm._
import scala.util.{ Failure, Success, Try }

case class AuthenticationToken(hash: String, start: Long, end: Long) {
  def isValid = {
    val cTime = java.util.Calendar.getInstance.getTimeInMillis
    start <= cTime && end > cTime
  }
}

class TokenHandler extends DataHandler[String, AuthenticationToken] {

  private def clean[T](f: ⇒ T): T = {
    atomic { implicit t ⇒
      val outdated = map.filter { case (_, v) ⇒ !v.isValid }.map { case (k, _) ⇒ k }
      outdated.foreach(map.remove)
    }
    f
  }

  override def add(key: String, data: AuthenticationToken) = clean { super.add(key, data) }
  override def remove(key: String) = clean { super.remove(key) }
  override def get(key: String) = clean { super.get(key) }

}

case class InvalidPasswordException(message: String) extends Exception(message)

object Authentication extends JavaLogger

trait Authentication { self ⇒
  import Authentication.Log._

  /// FIXME remove outdated keys
  private val keyStorage = new TokenHandler

  lazy val sessionUUID = UUID.randomUUID().toString

  def issueToken(password: String)(implicit r: HttpServletRequest, preference: Preference) = {
    val cypher = Cypher(password)

    if (RESTServer.isPasswordCorrect(cypher)) {
      val signingKey = new SecretKeySpec(password.getBytes, "HmacSHA256")
      val mac = Mac.getInstance("HmacSHA256")
      mac.init(signingKey)
      val start = java.util.Calendar.getInstance().getTimeInMillis
      val end = start + (24 * 60 * 60 * 1000)
      val rawHmac = mac.doFinal((r.getRemoteHost + sessionUUID + start + end) getBytes ())
      val hash = new String(Base64.encodeBase64(rawHmac))
      val token = AuthenticationToken(hash, start, end)
      keyStorage.add(hash, token)
      Success(token)
    }
    else Failure(new InvalidPasswordException("Server password is incorrect"))
  }

  def checkToken(token: String): Boolean = {
    logger.info("checking token")

    keyStorage get token match {
      case Some(k) ⇒ k.isValid
      case _       ⇒ false
    }
  }
}

