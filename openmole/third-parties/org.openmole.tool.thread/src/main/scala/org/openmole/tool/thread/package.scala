/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.tool

import java.util.concurrent._
import org.openmole.tool.logger.JavaLogger

import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success, Try }

package object thread {
  object L extends JavaLogger
  import L._

  implicit def future2Function[A](f: Future[A]): () => A = () => f.get
  implicit def function2Callable[F](f: => F): Callable[F] = new Callable[F] { def call = f }
  implicit def function2Runable(f: => Unit): Runnable = new Runnable { def run = f }
  def thread(f: => Unit) = new Thread { override def run = f }

  def background[F](f: => F)(implicit pool: ThreadPoolExecutor): Future[F] = pool.submit(f)

  def timeout[F](f: => F)(duration: Duration)(implicit pool: ThreadPoolExecutor): F = try {
    val r = pool.submit(f)
    try r.get(duration.toMillis, TimeUnit.MILLISECONDS)
    catch {
      case e: TimeoutException => r.cancel(true); throw e
    }
  }
  catch {
    case e: RejectedExecutionException =>
      Log.logger.log(Log.WARNING, "Execution rejected, operation executed in the caller thread with no timeout", e)
      f
  }

  def withThreadClassLoader[T](classLoader: ClassLoader)(f: => T): T = {
    val threadClassLoader = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(classLoader)
    try f
    finally Thread.currentThread().setContextClassLoader(threadClassLoader)
  }

  def forciblyStop(t: Thread) = {
    t.interrupt()
    if (t.isAlive) t.stop
  }

}
