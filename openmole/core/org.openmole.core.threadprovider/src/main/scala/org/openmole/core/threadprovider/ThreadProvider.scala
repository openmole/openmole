package org.openmole.core.threadprovider

import java.util.UUID
import java.util.concurrent._

import org.openmole.core.preference._
import org.openmole.tool.collection._

import scala.concurrent.ExecutionContext

object ThreadProvider:

  val maxPriority = Int.MaxValue
  val maxPoolSize = PreferenceLocation("ThreadProvider", "MaxPoolSize", Some(50))

  def apply(maxPoolSize: Option[Int] = None)(implicit preference: Preference) =
    new ThreadProvider(maxPoolSize.getOrElse(preference(ThreadProvider.maxPoolSize)))

  def stub() = apply()(Preference.stub())

  type Closure = () ⇒ Unit

  class RunClosure(queue: PriorityQueue[Closure]) extends Runnable:
    override def run =
      val job = queue.dequeue()
      job.apply()

  def threadFactory(parentGroup: Option[ThreadGroup] = None): ThreadFactory = r =>
      val t = parentGroup match
        case Some(p) ⇒ new Thread(p, r)
        case None    ⇒ new Thread(r)

      t.setDaemon(true)
      t


  def apply(poolSize: Int) = new ThreadProvider(poolSize)

  extension (t: ThreadProvider)
    def newSingleThreadExecutor = Executors.newSingleThreadExecutor(t.threadFactory)


class ThreadProvider(poolSize: Int):

  lazy val parentGroup = new ThreadGroup("provider-" + UUID.randomUUID().toString)

  lazy val virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor()
  given executionContext: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.fromExecutor(virtualThreadPool)

  lazy val scheduler = Executors.newScheduledThreadPool(1, threadFactory)

  var stopped = false

  def stop() = synchronized:
    stopped = true
    scheduler.shutdown()
    parentGroup.interrupt()

  def virtual(task: ThreadProvider.Closure) = virtualThreadPool.execute(() => task())

  def submit[T](t: ⇒ T) = scala.concurrent.Future[T] { t }

  def newThread(runnable: Runnable, groupName: Option[String] = None) =
    synchronized:
      if (stopped) throw new RuntimeException("Thread provider has been stopped")
      val group = groupName.map(n ⇒ new ThreadGroup(parentGroup, n)).getOrElse(parentGroup)
      val t = new Thread(group, runnable)
      t.setDaemon(true)
      t

  def threadFactory: ThreadFactory = ThreadProvider.threadFactory(Some(parentGroup))

