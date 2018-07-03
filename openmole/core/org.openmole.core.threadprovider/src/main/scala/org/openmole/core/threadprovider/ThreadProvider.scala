package org.openmole.core.threadprovider

import java.util.UUID
import java.util.concurrent._

import org.openmole.core.preference._
import org.openmole.tool.collection._

import scala.concurrent.ExecutionContext

object ThreadProvider {

  val maxPoolSize = ConfigurationLocation("ThreadProvider", "MaxPoolSize", Some(50))

  def apply(maxPoolSize: Option[Int] = None)(implicit preference: Preference) =
    new ThreadProvider(maxPoolSize.getOrElse(preference(ThreadProvider.maxPoolSize)))

  type Closure = () ⇒ Unit

  class RunClosure(queue: PriorityQueue[Closure]) extends Runnable {
    override def run = {
      val job = queue.dequeue
      job.apply()
    }
  }

  def threadFactory(parentGroup: Option[ThreadGroup] = None): ThreadFactory = new ThreadFactory {
    override def newThread(r: Runnable): Thread = {
      val t = parentGroup match {
        case Some(p) ⇒ new Thread(p, r)
        case None    ⇒ new Thread(r)
      }
      t.setDaemon(true)
      t
    }
  }

  def apply(poolSize: Int) = new ThreadProvider(poolSize)

  def background[T](threadProvider: ThreadProvider)(closure: ⇒ T): Future[T] =
    threadProvider.pool.submit(() ⇒ closure)

}

class ThreadProvider(poolSize: Int) {

  lazy val parentGroup = new ThreadGroup("provider-" + UUID.randomUUID().toString)

  implicit lazy val pool =
    new ThreadPoolExecutor(poolSize, poolSize, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue[Runnable](), threadFactory)

  lazy val scheduler = Executors.newScheduledThreadPool(1, threadFactory)
  lazy val taskQueue = PriorityQueue[ThreadProvider.Closure](true)

  var stopped = false

  def stop() = synchronized {
    stopped = true
    scheduler.shutdown()
    pool.shutdown()
    parentGroup.interrupt()
  }

  def submit(task: ThreadProvider.Closure, priority: Int) = {
    taskQueue.enqueue(task, priority)
    pool.submit(new ThreadProvider.RunClosure(taskQueue))
  }

  def newThread(runnable: Runnable, groupName: Option[String] = None) = synchronized {
    if (stopped) throw new RuntimeException("Thread provider has been stopped")
    val group = groupName.map(n ⇒ new ThreadGroup(parentGroup, n)).getOrElse(parentGroup)
    val t = new Thread(group, runnable)
    t.setDaemon(true)
    t
  }

  def threadFactory: ThreadFactory = ThreadProvider.threadFactory(Some(parentGroup))

}
