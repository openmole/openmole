package org.openmole.plugin.environment.batch.environment

import java.util.concurrent.Semaphore

object AccessControl {

  def tryWithPermit[B](accessControl: AccessControl)(f: ⇒ B) = {
    val t = tryAquirePermit(accessControl)
    if (t) try Some(f) finally releasePermit(accessControl)
    else None
  }

  def withPermit[B](accessControl: AccessControl)(f: ⇒ B) = {
    aquirePermit(accessControl)
    try f
    finally releasePermit(accessControl)
  }

  private def tryAquirePermit(accessControl: AccessControl) = {
    val aq = accessControl.semaphore.tryAcquire()
    if (aq) {
      accessControl.all.acquire()
      permit(accessControl, Thread.currentThread())
    }
    aq
  }

  private def aquirePermit(accessControl: AccessControl) =
    if (!isPermitted(accessControl, Thread.currentThread())) {
      accessControl.semaphore.acquire()
      accessControl.all.acquire()
      permit(accessControl, Thread.currentThread())
    }

  private def releasePermit(accessControl: AccessControl) = {
    revokePermit(accessControl, Thread.currentThread())
    accessControl.semaphore.release()
    accessControl.all.release()
  }

  private def isPermitted(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.contains(thread.getId))
  private def permit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.add(thread.getId))
  private def revokePermit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.remove(thread.getId))

  def freeze(accessControl: AccessControl) = {
    accessControl.semaphore.setMaxPermits(0)
  }

  def unfreeze(accessControl: AccessControl) = {
    accessControl.semaphore.setMaxPermits(accessControl.nbTokens)
  }

  def waitUnused(accessControl: AccessControl) = {
    accessControl.all.acquire(accessControl.nbTokens)
    accessControl.all.release(accessControl.nbTokens)
  }

  import java.util.concurrent.Semaphore

  final class AdjustableSemaphore(var maxPermits: Int) extends Semaphore(maxPermits) {

    def setMaxPermits(newMax: Int): Unit = synchronized {
      if (newMax < 0) throw new IllegalArgumentException("Semaphore size must be at least 0," + " was " + newMax)

      val delta = newMax - maxPermits

      if (delta == 0) return
      else if (delta > 0) release(delta)
      else reducePermits(-delta)

      maxPermits = newMax
    }

  }

}

case class AccessControl(nbTokens: Int) { la ⇒
  lazy val permittedThreads = collection.mutable.Set[Long]()
  lazy val semaphore = new AccessControl.AdjustableSemaphore(nbTokens)
  lazy val all = new Semaphore(nbTokens)
}

