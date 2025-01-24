/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.core.outputmanager

import java.io.{ OutputStream, PrintStream }

import org.openmole.tool.types.TypeTool

import scala.collection.mutable
import org.openmole.tool.stream._

object OutputManager {

  val systemOutput = System.out
  val systemError = System.err

  lazy val outputDispatcher = new PrintStream(OutputManager.dispatchOutput)
  lazy val errorDispatcher = new PrintStream(OutputManager.dispatchError)

  def install = 
    redirectSystemOutput(outputDispatcher)
    redirectSystemError(errorDispatcher)

  def uninstall = 
    redirectSystemOutput(systemOutput)
    redirectSystemError(systemError)

  private lazy val output = mutable.WeakHashMap[ThreadGroup, PrintStream]()
  private lazy val error = mutable.WeakHashMap[ThreadGroup, PrintStream]()

  private def findRedirect(group: ThreadGroup, map: mutable.Map[ThreadGroup, PrintStream]): Option[PrintStream] =
    def parentThreadGroups(group: ThreadGroup) =
      Iterator.iterate(group)(_.getParent).takeWhile(_ != null)

    parentThreadGroups(group).map(map.get).find(_.isDefined).map(_.get)

  private def redirectedOutput[T](f: OutputStream ⇒ T) = output.synchronized {
    findRedirect(Thread.currentThread().getThreadGroup, output) match
      case None                               ⇒ f(systemOutput)
      case Some(os) if os == outputDispatcher ⇒ f(systemOutput)
      case Some(os)                           ⇒ f(os)
  }

  private def redirectedError[T](f: OutputStream ⇒ T) = error.synchronized {
    findRedirect(Thread.currentThread().getThreadGroup, error) match 
      case None                              ⇒ f(systemError)
      case Some(os) if os == errorDispatcher ⇒ f(systemError)
      case Some(os)                          ⇒ f(os)
  }

  class RedirectedOutput extends OutputStream:
    override def write(i: Int): Unit = redirectedOutput(_.write(i))
    override def flush(): Unit = redirectedOutput(_.flush())

  class RedirectError extends OutputStream:
    override def write(i: Int): Unit = redirectedError(_.write(i))
    override def flush(): Unit = redirectedError(_.flush())

  lazy val dispatchOutput = new RedirectedOutput
  lazy val dispatchError = new RedirectError

  def redirectSystemOutput(ps: PrintStream) = 
    System.setOut(ps)
    TypeTool.call(Console, "setOutDirect", Seq(classOf[PrintStream]), Seq(ps))

  def redirectSystemError(ps: PrintStream) = 
    System.setErr(ps)
    TypeTool.call(Console, "setErrDirect", Seq(classOf[PrintStream]), Seq(ps))

  private def unregister(thread: ThreadGroup) = 
    output.synchronized { output.remove(thread) }
    error.synchronized { error.remove(thread) }

  private def redirectOutput(thread: ThreadGroup, stream: PrintStream) = output.synchronized {
    output.put(thread, stream)
  }

  private def redirectError(thread: ThreadGroup, stream: PrintStream) = error.synchronized {
    error.put(thread, stream)
  }

  case class Outputs(output: String, error: String)

  def withStringOutput[T](f: ⇒ T): (T, Outputs) = {
    val o = StringPrintStream()
    val e = StringPrintStream()
    redirectOutput(o, e)

    val res =
      try f
      finally OutputManager.unregister(Thread.currentThread.getThreadGroup)

    (res, Outputs(o.toString, e.toString))
  }

  def withStreamOutputs[T](out: PrintStream, err: PrintStream)(f: ⇒ T): T = {
    redirectOutput(out, err)
    val res =
      try f
      finally OutputManager.unregister(Thread.currentThread.getThreadGroup)
    res
  }

  private def redirectOutput(output: PrintStream, error: PrintStream): Unit = {
    OutputManager.redirectOutput(Thread.currentThread.getThreadGroup, output)
    OutputManager.redirectError(Thread.currentThread.getThreadGroup, error)
  }

}
