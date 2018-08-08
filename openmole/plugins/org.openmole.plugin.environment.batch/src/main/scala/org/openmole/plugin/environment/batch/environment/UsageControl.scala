package org.openmole.plugin.environment.batch.environment

import org.openmole.core.exception._
import scala.concurrent.stm._

case class AccessToken() {
  def access[T](op: ⇒ T): T = synchronized(op)
}

object UsageControl {

  def faucetToken = AccessToken()

  def mapToken[B](usageControl: UsageControl)(f: AccessToken ⇒ B) = {
    val t = tryGetToken(usageControl)
    try t.map(f)
    finally t.foreach(releaseToken(usageControl, _))
  }

  def tryWithToken[B](usageControl: UsageControl)(f: Option[AccessToken] ⇒ B) = {
    val t = tryGetToken(usageControl)
    try f(t)
    finally t.foreach(releaseToken(usageControl, _))
  }

  def withToken[B](usageControl: UsageControl)(f: AccessToken ⇒ B) = {
    val t = getToken(usageControl)
    try f(t)
    finally releaseToken(usageControl, t)
  }

  private def releaseToken(usageControl: UsageControl, token: AccessToken) = atomic { implicit txn ⇒
    usageControl.usedToken -= token
    usageControl.tokens() = token :: usageControl.tokens()
  }

  private def tryGetToken(usageControl: UsageControl): Option[AccessToken] = atomic { implicit txn ⇒
    if (usageControl.stopped()) None
    else
      usageControl.tokens() match {
        case h :: t ⇒
          usageControl.tokens() = t
          usageControl.usedToken += h
          Some(h)
        case Nil ⇒ None
      }
  }

  private def getToken(usageControl: UsageControl) = atomic { implicit txn ⇒
    if (usageControl.stopped()) throw new InternalProcessingError("Service has been stopped")
    tryGetToken(usageControl) match {
      case Some(t) ⇒ t
      case None    ⇒ retry
    }
  }

  def freeze(usageControl: UsageControl) = {
    usageControl.stopped.single() = true
  }

  def unfreeze(usageControl: UsageControl) = {
    usageControl.stopped.single() = false
  }

  def waitUnused(usageControl: UsageControl) = atomic {
    implicit txn ⇒ if (!usageControl.usedToken.isEmpty) retry
  }

}

case class UsageControl(nbTokens: Int) { la ⇒
  private lazy val tokens = Ref(List.fill(nbTokens)(AccessToken()))
  private lazy val usedToken = TSet[AccessToken]()
  private lazy val stopped = Ref(false)
}

