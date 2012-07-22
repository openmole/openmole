/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.file

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.RoundRobinRouter
import akka.routing.SmallestMailboxRouter
import com.typesafe.config.ConfigFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.{ FileInputStream, FileOutputStream }
import java.net.URI
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.ogf.saga.error.DoesNotExistException
import org.ogf.saga.error.TimeoutException
import org.ogf.saga.file.FileFactory
import org.ogf.saga.namespace.NSDirectory
import org.ogf.saga.namespace.NSEntry
import org.ogf.saga.namespace.NSFactory
import org.ogf.saga.task.Task
import org.ogf.saga.task.TaskMode
import org.ogf.saga.url.URL
import org.ogf.saga.url.URLFactory
import org.ogf.saga.namespace.Flags
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.control.AccessToken
import org.openmole.misc.tools.obj.Id
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.tools.io.Network._
import org.openmole.core.batch.control.StorageControl._
import org.openmole.core.batch.control.UsageControl._
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.control.QualityControl._
import org.openmole.core.batch.authentication.JSAGASessionService
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._

object URIFile extends Logger {

  import IURIFile._

  val Timeout = new ConfigurationLocation("URIFile", "Timeout")
  val BufferSize = new ConfigurationLocation("URIFile", "BufferSize")
  val CopyTimeout = new ConfigurationLocation("URIFile", "CopyTimeout")
  val CleanerWorkers = new ConfigurationLocation("URIFile", "CleanerWorkers")

  Workspace += (Timeout, "PT2M")
  Workspace += (BufferSize, "32768")
  Workspace += (CopyTimeout, "PT1M")
  Workspace += (CleanerWorkers, "5")

  val system = ActorSystem("URIFile", ConfigFactory.parseString(
    """
akka {
  daemonic="on"
  actor {
    default-dispatcher {
            
      executor = "fork-join-executor"
      type = Dispatcher
      
      fork-join-executor {
        parallelism-min = """ + Workspace.preference(CleanerWorkers) + """
        parallelism-max = """ + Workspace.preference(CleanerWorkers) + """
      }
    }
  }
}
"""))

  val cleaners = system.actorOf(Props(new CleanerActor).withRouter(RoundRobinRouter(Workspace.preferenceAsInt(CleanerWorkers))), name = "cleaner")

  case class Clean(file: IURIFile)

  def clean(file: IURIFile) = cleaners ! Clean(file)

  def child(url: URL, child: String): URL = {
    if (url.toString().endsWith("/") || child.charAt(0) == '/') fromLocation(url.toString() + child)
    else fromLocation(url.toString() + '/' + child)
  }

  def fromLocation(location: String): URL = URLFactory.createURL(location)
  def fromLocation(location: URI): URL = fromLocation(location.toString)

  def copy(src: IURIFile, dest: File): Unit = withToken(src.storageDescription, copy(src, _, dest))

  def copy(src: IURIFile, srcToken: AccessToken, dest: File): Unit = srcToken.synchronized {
    val os = new FileOutputStream(dest)
    try {
      val is = src.openInputStream(srcToken)
      try {
        withFailureControl(src.storageDescription,
          is.copy(os, Workspace.preferenceAsInt(BufferSize), Workspace.preferenceAsDurationInMs(CopyTimeout)))
      } finally is.close
    } finally os.close
  }

  def copy(src: File, dest: IURIFile): Unit = withToken(dest.storageDescription, copy(src, dest, _))

  def copy(src: File, dest: IURIFile, token: AccessToken): Unit = token.synchronized {
    val is = new FileInputStream(src)
    try {
      val os = dest.openOutputStream(token)
      try withFailureControl(dest.storageDescription,
        is.copy(os, Workspace.preferenceAsInt(BufferSize),
          Workspace.preferenceAsDurationInMs(CopyTimeout)))
      finally os.close
    } finally is.close
  }

  private def sameRessource(srcDescrption: ServiceDescription, destDescrption: ServiceDescription) = srcDescrption.equals(destDescrption);
}

class URIFile(val location: String) extends IURIFile with Id {

  import URIFile._

  private def this(location: URL) = this(location.toString)

  def this(file: File) = this(file.getCanonicalFile.toURI.toString)

  def this(uriFile: IURIFile, childVal: String) = this(URIFile.child(URIFile.fromLocation(uriFile.location), childVal))

  def this(location: URI) = this(if (location.getScheme == null) new File(location.getPath).toURI.toString else location.toString)

  def this(file: IURIFile) = this(file.location)

  private def withToken[A](a: (AccessToken) ⇒ A): A = org.openmole.core.batch.control.UsageControl.withToken(storageDescription, a)
  private def withFailureControl[A](a: ⇒ A, isFailure: Throwable ⇒ Boolean): A = org.openmole.core.batch.control.StorageControl.withFailureControl(storageDescription, a, isFailure)
  private def withFailureControl[A](a: ⇒ A): A = org.openmole.core.batch.control.StorageControl.withFailureControl(storageDescription, a)

  private def trycatch[A](f: ⇒ A): A = {
    try f
    catch {
      case (e: IOException) ⇒ throw e
      case e ⇒ throw new IOException(location, e)
    }
  }

  private def trycatch[A](f: ⇒ A, t: Task[_, _]): A = {
    try f
    catch {
      case (e: TimeoutException) ⇒
        t.cancel(true)
        throw new IOException(location, e)
      case (e: IOException) ⇒ throw e
      case e: ExecutionException ⇒ throw e.getCause
      case e ⇒ throw new IOException(location, e)
    }
  }

  private def fetchEntry(token: AccessToken): NSEntry = trycatch {
    val task = NSFactory.createNSEntry(TaskMode.ASYNC, JSAGASessionService.session(location), SAGAURL)
    trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
  }

  private def fetchEntryAsDirectory(token: AccessToken): NSDirectory = trycatch {
    val task = NSFactory.createNSDirectory(TaskMode.ASYNC, JSAGASessionService.session(location), SAGAURL)
    trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
  }

  protected def close(entry: NSEntry) = trycatch {
    val task = entry.close(TaskMode.ASYNC)
    trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
  }

  protected def SAGAURL: URL = trycatch(fromLocation(location))

  private def parentLocationAndName(token: AccessToken) = {
    val n = name
    val indice = location.lastIndexOfSlice(n)
    val parent = location.slice(0, indice)
    (parent, n)
  }

  override def name = new File(SAGAURL.getPath).getName

  /*-------------------- is a directory ---------------------------*/
  override def isDirectory: Boolean = withToken(isDirectory(_))

  override def isDirectory(token: AccessToken): Boolean = trycatch {
    val entry = fetchEntry(token)
    try isDirectory(entry)
    finally close(entry)
  }

  private def isDirectory(entry: NSEntry): Boolean = trycatch {
    val task = entry.isDir(TaskMode.ASYNC)
    trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task).booleanValue
  }

  override def URLRepresentsADirectory: Boolean = trycatch { location.toString.endsWith("/") }

  /*--------------------- mkdir ---------------------------*/
  override def mkdir(name: String): IURIFile = withToken(mkdir(name, _))

  override def mkdir(name: String, token: AccessToken): IURIFile = token.synchronized {
    withFailureControl {
      val dir = fetchEntryAsDirectory(token)
      try trycatch {
        val cname = if (name.endsWith("/")) name else name + '/'

        val dest = URIFile.child(SAGAURL, cname)
        val task = dir.makeDir(TaskMode.ASYNC, dest)

        trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)

        new URIFile(this, name)
      } finally close(dir)
    }
  }

  override def mkdirIfNotExist(name: String): IURIFile = withToken(mkdirIfNotExist(name, _))

  override def mkdirIfNotExist(name: String, token: AccessToken): IURIFile = token.synchronized {
    try mkdir(name, token)
    catch {
      case (e: IOException) ⇒
        withFailureControl {
          val cname = if (name.endsWith("/")) name else name + '/'
          val childVal = child(cname)
          if (!childVal.isDirectory(token)) throw new IOException("Could not create dir " + location, e);
          childVal
        }
    }
  }

  /* ------------------- new file in dir -------------------------*/
  override def newFileInDir(prefix: String, sufix: String): IURIFile = new URIFile(this, prefix + "_" + UUID.randomUUID.toString + sufix)

  /*-------------------------- exist -------------------------*/
  override def exist(name: String): Boolean = withToken(exist(name, _))

  override def exist(name: String, token: AccessToken): Boolean = token.synchronized {
    val dir = fetchEntryAsDirectory(token)

    try trycatch {
      val dest = URLFactory.createURL(name)
      val task = dir.exists(TaskMode.ASYNC, dest)

      trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task).booleanValue

    } finally close(dir)
  }

  override def exists: Boolean = withToken(exists(_))

  override def exists(token: AccessToken): Boolean = token.synchronized {
    try {
      val (parent, name) = parentLocationAndName(token)
      new URIFile(parent).exist(name, token)
    } catch {
      case e: IOException ⇒
        e.getCause match {
          case _: DoesNotExistException ⇒ false
          case _ ⇒ throw e
        }
    }
  }

  override def openInputStream: InputStream = withToken(openInputStream(_))

  override def openInputStream(token: AccessToken): InputStream = token.synchronized {
    trycatch {
      val task = FileFactory.createFileInputStream(TaskMode.ASYNC, JSAGASessionService.session(location), SAGAURL);

      trycatch(
        withFailureControl({
          val ret = task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS)
          new JSAGAInputStream(ret)
        }, { e: Throwable ⇒ !classOf[DoesNotExistException].isAssignableFrom(e.getCause.getClass) }), task)
    }
  }

  override def openOutputStream: OutputStream = withToken(openOutputStream(_))

  override def openOutputStream(token: AccessToken): OutputStream = token.synchronized {
    trycatch {
      val task = FileFactory.createFileOutputStream(TaskMode.ASYNC, JSAGASessionService.session(location), SAGAURL, false)
      trycatch(
        withFailureControl {
          val ret = task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS)
          new JSAGAOutputStream(ret)
        }, task)
    }
  }

  override def cache: File = withToken(cache(_))

  override def cache(token: AccessToken): File = token.synchronized {
    trycatch(synchronized {
      val cacheTmp = Workspace.newFile("file", "cache")
      try this.copy(cacheTmp, token)
      catch {
        case e ⇒
          cacheTmp.delete
          throw e
      }
      cacheTmp
    })
  }

  private def isLocal: Boolean = {
    val url = SAGAURL
    url.getHost == null || url.getScheme == null || (url.getScheme != null && url.getScheme.compareToIgnoreCase("file") == 0) || isLocalHost(url.getHost)
  }

  override def copy(dest: File) = URIFile.copy(this, dest)
  override def copy(dest: File, srcToken: AccessToken) = URIFile.copy(this, srcToken, dest)

  /* -------------------- remove -------------------------------*/
  private def removeTask(entry: NSEntry) =
    if (URLRepresentsADirectory) entry.remove(TaskMode.ASYNC, Flags.RECURSIVE.getValue)
    else entry.remove(TaskMode.ASYNC)

  override def remove = withToken(remove)

  override def remove(token: AccessToken) = trycatch {
    val entry = fetchEntry(token)
    try {
      val task = removeTask(entry)
      trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
    } finally close(entry)
  }

  override def list: Iterable[String] = withToken(list(_))

  override def list(token: AccessToken): Iterable[String] = trycatch {
    val dir = fetchEntryAsDirectory(token)
    try {
      val task = dir.list(TaskMode.ASYNC)
      trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS).map { _.toString }, task)
    } finally close(dir)
  }

  override def modificationTime(name: String): Long = withToken(modificationTime(name, _))

  override def modificationTime(name: String, token: AccessToken): Long = trycatch {
    val dir = fetchEntryAsDirectory(token)
    try {
      val dest = URLFactory.createURL(name)
      val task = dir.getMTime(TaskMode.ASYNC, dest)
      trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
    } finally close(dir)
  }

  override def touch = withToken(touch(_))

  override def touch(token: AccessToken) = trycatch {
    openOutputStream(token).close
  }
  override def modificationTime: Long = withToken(modificationTime(_))

  override def modificationTime(token: AccessToken): Long = token.synchronized {
    trycatch {
      val entry = fetchEntry(token)
      try {
        val task = entry.getMTime(TaskMode.ASYNC)
        trycatch(task.get(Workspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
      } finally close(entry)
    }
  }

  override def child(childVal: String): URIFile = new URIFile(this, childVal)

  override def storageDescription = new ServiceDescription(new URI(location))
  override def URI: URI = new URI(location)
  override def path = URI.getPath

  override def toString = location
  override def id = location

}
