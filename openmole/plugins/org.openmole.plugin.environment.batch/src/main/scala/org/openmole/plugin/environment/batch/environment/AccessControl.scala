package org.openmole.plugin.environment.batch.environment

import java.util.concurrent.Semaphore

object AccessControl:

  object Priority:
    def apply(i: Int): Priority = i

  opaque type Priority = Int
  
  def defaultPrirority[T](f: Priority ?=> T) =
    f(using 0)

  def tryWithPermit[B](accessControl: AccessControl)(using AccessControl.Priority)(f: ⇒ B) =
    val t = tryAcquirePermit(accessControl)
    if t
    then
      try Some(f)
      finally releasePermit(accessControl)
    else None

  def withPermit[B](accessControl: AccessControl)(using AccessControl.Priority)(f: ⇒ B) =
    aquirePermit(accessControl)
    try f
    finally releasePermit(accessControl)

  private def tryAcquirePermit(accessControl: AccessControl) =
    val aq = accessControl.all.tryAcquire()
    if aq then permit(accessControl, Thread.currentThread())
    aq

  private def aquirePermit(accessControl: AccessControl) =
    if !isPermitted(accessControl, Thread.currentThread())
    then
      accessControl.all.acquire()
      permit(accessControl, Thread.currentThread())

  private def releasePermit(accessControl: AccessControl) =
    revokePermit(accessControl, Thread.currentThread())
    accessControl.all.release()

  private def isPermitted(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.contains(thread.getId))
  private def permit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.add(thread.getId))
  private def revokePermit(accessControl: AccessControl, thread: Thread) = accessControl.permittedThreads.synchronized(accessControl.permittedThreads.remove(thread.getId))


case class AccessControl(nbTokens: Int):
  lazy val permittedThreads = collection.mutable.Set[Long]()
  lazy val all = new Semaphore(nbTokens)

  def apply[T](f: ⇒ T)(using AccessControl.Priority): T = AccessControl.withPermit(this)(f)


