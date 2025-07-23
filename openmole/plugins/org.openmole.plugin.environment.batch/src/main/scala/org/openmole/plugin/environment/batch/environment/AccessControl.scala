package org.openmole.plugin.environment.batch.environment

import org.openmole.tool.lock.PrioritySemaphore

object AccessControl:

  object Priority:
    def apply(i: Int): Priority = i

  object Bypass
  opaque type Priority = Int | Bypass.type

  def defaultPrirority[T](f: Priority ?=> T) =
    f(using 0)

  def bypassAccessControl[T](f: Priority ?=> T) =
    f(using Bypass)

  def withPermit[B](accessControl: AccessControl)(using priority: AccessControl.Priority)(f: Priority ?=> B) =
    priority match
      case Bypass => f(using Bypass)
      case p: Int =>
        if isPermitted(accessControl, Thread.currentThread())
        then f(using Bypass)
        else
          acquirePermit(accessControl, p)
          try f(using Bypass)
          finally releasePermit(accessControl)

  private def acquirePermit(accessControl: AccessControl, priority: Int) =
    accessControl.semaphore.acquire(priority)
    permit(accessControl, Thread.currentThread())

  private def releasePermit(accessControl: AccessControl) =
    revokePermit(accessControl, Thread.currentThread())
    accessControl.semaphore.release()

  private def isPermitted(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.contains(thread.threadId()))
  private def permit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.add(thread.threadId()))
  private def revokePermit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.remove(thread.threadId()))


case class AccessControl(nbTokens: Int):
  lazy val permittedThreads = collection.mutable.Set[Long]()
  lazy val semaphore = new PrioritySemaphore(nbTokens)

  def apply[T](f: AccessControl.Priority ?=> T)(using AccessControl.Priority): T =
    AccessControl.withPermit(this)(f)


