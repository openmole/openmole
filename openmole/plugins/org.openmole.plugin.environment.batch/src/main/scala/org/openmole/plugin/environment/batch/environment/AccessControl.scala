package org.openmole.plugin.environment.batch.environment

import java.util.concurrent.Semaphore

import scala.tools.reflect.WrappedProperties

object AccessControl {

  def tryWithPermit[B](accessControl: AccessControl)(f: ⇒ B) = {
    val t = tryAcquirePermit(accessControl)
    if (t) try Some(f) finally releasePermit(accessControl)
    else None
  }

  def withPermit[B](accessControl: AccessControl)(f: ⇒ B) = {
    aquirePermit(accessControl)
    try f
    finally releasePermit(accessControl)
  }

  private def tryAcquirePermit(accessControl: AccessControl) = {
    val aq = accessControl.all.tryAcquire()
    if (aq) permit(accessControl, Thread.currentThread())
    aq
  }

  private def aquirePermit(accessControl: AccessControl) =
    if (!isPermitted(accessControl, Thread.currentThread())) {
      accessControl.all.acquire()
      permit(accessControl, Thread.currentThread())
    }

  private def releasePermit(accessControl: AccessControl) = {
    revokePermit(accessControl, Thread.currentThread())
    accessControl.all.release()
  }

  private def isPermitted(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.contains(thread.getId))
  private def permit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.add(thread.getId))
  private def revokePermit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.remove(thread.getId))

}

case class AccessControl(nbTokens: Int) { la ⇒
  lazy val permittedThreads = collection.mutable.Set[Long]()
  lazy val all = new Semaphore(nbTokens)

  def apply[T](f: ⇒ T): T = AccessControl.withPermit(this)(f)
}

