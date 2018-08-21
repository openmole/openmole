package org.openmole.plugin.environment.batch.environment

import java.util.concurrent.Semaphore

object UsageControl {

  def tryWithPermit[B](usageControl: UsageControl)(f: ⇒ B) = {
    val t = tryAquirePermit(usageControl)
    if (t) try Some(f) finally releasePermit(usageControl)
    else None
  }

  def withPermit[B](usageControl: UsageControl)(f: ⇒ B) = {
    aquirePermit(usageControl)
    try f
    finally releasePermit(usageControl)
  }

  private def tryAquirePermit(usageControl: UsageControl) = {
    val aq = usageControl.semaphore.tryAcquire()
    if (aq) {
      usageControl.all.acquire()
      permit(usageControl, Thread.currentThread())
    }
    aq
  }

  private def aquirePermit(usageControl: UsageControl) =
    if (!isPermitted(usageControl, Thread.currentThread())) {
      usageControl.semaphore.acquire()
      usageControl.all.acquire()
      permit(usageControl, Thread.currentThread())
    }

  private def releasePermit(usageControl: UsageControl) = {
    revokePermit(usageControl, Thread.currentThread())
    usageControl.semaphore.release()
    usageControl.all.release()
  }

  private def isPermitted(usageControl: UsageControl, thread: Thread) = usageControl.permittedThreads.synchronized(usageControl.permittedThreads.contains(thread.getId))
  private def permit(usageControl: UsageControl, thread: Thread) = usageControl.permittedThreads.synchronized(usageControl.permittedThreads.add(thread.getId))
  private def revokePermit(usageControl: UsageControl, thread: Thread) = usageControl.permittedThreads.synchronized(usageControl.permittedThreads.remove(thread.getId))

  def freeze(usageControl: UsageControl) = {
    usageControl.semaphore.setMaxPermits(0)
  }

  def unfreeze(usageControl: UsageControl) = {
    usageControl.semaphore.setMaxPermits(usageControl.nbTokens)
  }

  def waitUnused(usageControl: UsageControl) = {
    usageControl.all.acquire(usageControl.nbTokens)
    usageControl.all.release(usageControl.nbTokens)
  }

}

case class UsageControl(nbTokens: Int) { la ⇒
  lazy val permittedThreads = collection.mutable.Set[Long]()
  lazy val semaphore = new AdjustableSemaphore(nbTokens)
  lazy val all = new Semaphore(nbTokens)
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