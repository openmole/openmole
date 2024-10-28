package org.openmole.plugin.environment.batch.environment

import org.openmole.tool.lock.PrioritySemaphore

object AccessControl:

  object Priority:
    def apply(i: Int): Priority = i

  opaque type Priority = Int
  
  def defaultPrirority[T](f: Priority ?=> T) =
    f(using 0)

  def withPermit[B](accessControl: AccessControl)(using AccessControl.Priority)(f: ⇒ B) =
    aquirePermit(accessControl)
    try f
    finally releasePermit(accessControl)

  private def aquirePermit(accessControl: AccessControl)(using priority: AccessControl.Priority) =
    if !isPermitted(accessControl, Thread.currentThread())
    then
      accessControl.semaphore.acquire(priority)
      permit(accessControl, Thread.currentThread())

  private def releasePermit(accessControl: AccessControl) =
    revokePermit(accessControl, Thread.currentThread())
    accessControl.semaphore.release()

  private def isPermitted(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.contains(thread.getId))
  private def permit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.add(thread.getId))
  private def revokePermit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.remove(thread.getId))


case class AccessControl(nbTokens: Int):
  lazy val permittedThreads = collection.mutable.Set[Long]()
  lazy val semaphore = new PrioritySemaphore(nbTokens)

  def apply[T](f: ⇒ T)(using AccessControl.Priority): T = AccessControl.withPermit(this)(f)


