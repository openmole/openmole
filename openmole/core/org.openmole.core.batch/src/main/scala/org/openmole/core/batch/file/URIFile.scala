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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.file

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.FileInputStream
import java.net.URI
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
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
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.tools.io.FileUtil
import org.openmole.core.batch.internal.Activator
import org.openmole.core.batch.environment.IAccessToken
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.commons.tools.io.Network._

import org.openmole.core.batch.control.IUsageControl._
import org.openmole.core.batch.control.BatchServiceDescription
import org.openmole.core.batch.control.BatchStorageDescription
import org.openmole.core.batch.control.IQualityControl._
import scala.collection.JavaConversions._

object URIFile {
  
  val Timeout = new ConfigurationLocation("URIFile", "Timeout")
  val BufferSize = new ConfigurationLocation("URIFile", "BufferSize")
  val CopyTimeout = new ConfigurationLocation("URIFile", "CopyTimeout")

  Activator.getWorkspace += (Timeout, "PT2M")
  Activator.getWorkspace += (BufferSize, "8192")
  Activator.getWorkspace += (CopyTimeout, "PT2M")
        
  def child(url: URL, child: String): URL = {
    if (url.toString().endsWith("/") || child.charAt(0) == '/') fromLocation(url.toString() + child)
    else fromLocation(url.toString() + '/' + child)
  }
    
  def fromLocation(location: String): URL = URLFactory.createURL(location) 
  def fromLocation(location: URI): URL = fromLocation(location.toString)
    
  def copy(src: IURIFile, srcToken: IAccessToken, dest: IURIFile): Unit = {
    val srcDescrption = src.storageDescription
    val destDescrption = dest.storageDescription

    val same = sameRessource(srcDescrption, destDescrption)

    if(same) copy(src, dest, srcToken, srcToken)
    else withToken(destDescrption,copy(src, dest, srcToken,_))
  }

  def copy(src: IURIFile, dest: IURIFile, destToken: IAccessToken): Unit = {
    val srcDescrption = src.storageDescription
    val destDescrption = dest.storageDescription

    val same = sameRessource(srcDescrption, destDescrption)

    if(same) copy(src, dest, destToken, destToken)
    else withToken(srcDescrption, copy(src, dest, _, destToken))
  }

  def copy(src: File, dest: IURIFile): Unit =  withToken(dest.storageDescription, copy(src, dest,_))

  def copy(src: File, dest: IURIFile, token: IAccessToken): Unit =  {
    val is = new FileInputStream(src)
    try {
      val os = dest.openOutputStream(token)
      try {
        withFailureControl(dest.storageDescription, FileUtil.copy(is, os, Activator.getWorkspace.preferenceAsInt(BufferSize), Activator.getWorkspace.preferenceAsDurationInMs(CopyTimeout)))
      } finally {
        os.close
      }
    } finally {
      is.close
    }
  }

  def copy(src: IURIFile, dest: IURIFile): Unit = {
    val srcDescrption = src.storageDescription
    val destDescrption = dest.storageDescription

    val same = sameRessource(srcDescrption, destDescrption)
        withToken(srcDescrption,
          srcToken =>
          if(!same) withToken(destDescrption, copy(src, dest, srcToken, _))
          else copy(src, dest, srcToken, srcToken)
        )
  }

  private def copy(src: IURIFile, dest: IURIFile, srcToken: IAccessToken, destToken: IAccessToken): Unit = {
    val srcDesc = src.storageDescription
    val destDesc = dest.storageDescription
    val same = sameRessource(srcDesc, destDesc)

    val is = src.openInputStream(srcToken)
    try {
      val os = dest.openOutputStream(destToken)

      try {
        withFailureControl(srcDesc,
          if(!same) withFailureControl(destDesc,FileUtil.copy(is, os, Activator.getWorkspace.preferenceAsInt(BufferSize), Activator.getWorkspace.preferenceAsDurationInMs(CopyTimeout)))
          else FileUtil.copy(is, os, Activator.getWorkspace.preferenceAsInt(BufferSize), Activator.getWorkspace.preferenceAsDurationInMs(CopyTimeout)) 
        )              
      } finally {
        os.close
      }
    } finally {
      is.close
    }
  }

  private def sameRessource(srcDescrption: BatchServiceDescription, destDescrption: BatchServiceDescription) = srcDescrption.equals(destDescrption);
}

class URIFile(val location: String) extends IURIFile {
  
  import URIFile._
 
  private def this(location: URL) = this(location.toString)
 
  def this(file: File) = this(file.getCanonicalFile.toURI.toString)

  def this(uriFile: IURIFile, childVal: String) = this(URIFile.child(URIFile.fromLocation(uriFile.location), childVal))

  def this(location: URI) =  this(if (location.getScheme == null) new File(location.getPath).toURI.toString else location.toString)

  def this(file: IURIFile) = this(file.location)
    
  private def withToken[A](a: (IAccessToken) => A): A = org.openmole.core.batch.control.IUsageControl.withToken(storageDescription,a)
  private def withFailureControl[A](a: => A): A = org.openmole.core.batch.control.IQualityControl.withFailureControl(storageDescription,a)

  private def trycatch[A](f: => A): A = {
    try {
      f
    } catch {
      case (e: IOException) => throw e
      case e => throw new IOException(location, e)
    }
  }
  
  private def trycatch[A](f: => A, t: Task[_,_]): A = {
    try {
      f
    } catch {
      case (e: TimeoutException) =>
        t.cancel(true)
        throw new IOException(location, e)
      case (e: IOException) => throw e
      case e => throw new IOException(location, e)
    }
  }
  
  private def fetchEntry: NSEntry = trycatch {
    val task = NSFactory.createNSEntry(TaskMode.ASYNC, Activator.getJSAGASessionService.getSession, SAGAURL)
    trycatch(task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS) , task)
  }
    

  private def fetchEntryAsDirectory: NSDirectory = trycatch {
    val task = NSFactory.createNSDirectory(TaskMode.ASYNC, Activator.getJSAGASessionService.getSession, SAGAURL)
    trycatch(task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
  }

  protected def close(entry: NSEntry) = trycatch {
    val task = entry.close(TaskMode.ASYNC)
    trycatch(task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
  }

  protected def SAGAURL: URL = trycatch(fromLocation(location))
  
  
  /*-------------------- is a directory ---------------------------*/
  override def isDirectory: Boolean = withToken(isDirectory(_))

  override def isDirectory(token: IAccessToken): Boolean = trycatch {
    val entry = fetchEntry
    try {
      isDirectory(entry)
    } finally {
      close(entry)
    }
  }

  private def isDirectory(entry: NSEntry): Boolean = trycatch {
    val task = entry.isDir(TaskMode.ASYNC)
    trycatch(task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task).booleanValue
  }

  override def URLRepresentsADirectory: Boolean = trycatch {location.toString.endsWith("/")}

  /*--------------------- mkdir ---------------------------*/
  override def mkdir(name: String): IURIFile = withToken(mkdir(name, _))

  override def mkdir(name: String, token: IAccessToken): IURIFile = {
    val dir = fetchEntryAsDirectory
    try trycatch {
      val cname =  if (name.endsWith("/")) {
        name
      } else {
        name + '/'
      }

      val dest = URIFile.child(SAGAURL, cname)
      val task = dir.makeDir(TaskMode.ASYNC, dest);
            
      trycatch(task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task)
      new URIFile(this, name)
    } finally {
      close(dir)
    }
  }
  
  override def mkdirIfNotExist(name: String): IURIFile = withToken(mkdirIfNotExist(name, _))

  override def mkdirIfNotExist(name: String, token: IAccessToken): IURIFile = {
    try {
      mkdir(name, token)
    } catch {
      case (e: IOException) =>
        withFailureControl {
          val childVal = child(name)
          if (!childVal.isDirectory(token)) throw new IOException("Could not create dir " + location, e);
          childVal
        }
    }
  }

  /* ------------------- new file in dir -------------------------*/
  override def newFileInDir(prefix: String, sufix: String): IURIFile =  new URIFile(this, prefix + UUID.randomUUID.toString + sufix);

  /*-------------------------- exist -------------------------*/
  override def exist(name: String): Boolean = withToken(exist(name, _))

  override def exist(name: String, token: IAccessToken): Boolean = {
    val dir = fetchEntryAsDirectory

    try trycatch {
      val dest = URLFactory.createURL(name)
      val task = dir.exists(TaskMode.ASYNC, dest)
    
      trycatch(task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS), task).booleanValue
    
    } finally {
      close(dir)
    }
  }

  override def openInputStream: InputStream = withToken(openInputStream(_))

  override def openInputStream(token: IAccessToken): InputStream = trycatch {

    val task = FileFactory.createFileInputStream(TaskMode.ASYNC, Activator.getJSAGASessionService.getSession, SAGAURL);

    trycatch(
      withFailureControl {
        val ret = task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS)
        new JSAGAInputStream(ret)
      }, task)
  }

  override def openOutputStream: OutputStream = withToken(openOutputStream(_))

  override def openOutputStream(token: IAccessToken): OutputStream = trycatch {

    val task = FileFactory.createFileOutputStream(TaskMode.ASYNC, Activator.getJSAGASessionService.getSession, SAGAURL, false)
    trycatch(
      withFailureControl {
        val ret = task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS)
        new JSAGAOutputStream(ret)
      }, task)
  }

  override def cache: File = withToken(cache(_))

  override def cache(token: IAccessToken): File = trycatch(synchronized {
      val cacheTmp = Activator.getWorkspace().newFile("file", "cache")
      this.copy(new URIFile(cacheTmp), token)
      cacheTmp
    })

  private def isLocal: Boolean = {
    val url = SAGAURL
    url.getHost == null || url.getScheme == null || (url.getScheme != null && url.getScheme.compareToIgnoreCase("file") == 0) || IsLocalHost(url.getHost)
  }
 
  override def copy(dest: IURIFile) = URIFile.copy(this, dest)
  override def copy(dest: IURIFile, srcToken: IAccessToken) = URIFile.copy(this, srcToken, dest)


  /* -------------------- remove -------------------------------*/
  override def remove(recursive: Boolean) = remove(true, recursive);
  override def remove(recursive: Boolean, token: IAccessToken) = remove(true, recursive, token);
  override def remove(timeOut: Boolean, recursive: Boolean) = withToken(remove(timeOut, recursive,_))

  override def remove(timeOut: Boolean, recursive: Boolean, token: IAccessToken) = trycatch {
    val entry = fetchEntry
    try {

      val task = if (recursive /*&& directory*/) entry.remove(TaskMode.ASYNC, Flags.RECURSIVE.getValue) 
      else entry.remove(TaskMode.ASYNC)

      trycatch(
        if (timeOut) {
          task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS)
        } else {
          task.get
        }
        , task)
    } finally {
      close(entry)
    }
  }

  override def list: Iterable[String] = withToken(list(_))

  override def list(token: IAccessToken): Iterable[String] = trycatch {

    val dir = fetchEntryAsDirectory
    try {
      val task = dir.list(TaskMode.ASYNC)

      trycatch (task.get(Activator.getWorkspace.preferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS).map{_.toString}, task)
    } finally {
      close(dir);
    }
  }

  override def child(childVal: String): URIFile =  new URIFile(this, childVal)

  override def storageDescription: BatchServiceDescription =  new BatchStorageDescription(location)
  override def URI: URI = new URI(location)
  override def toString: String = location

  override def equals(obj: Any): Boolean = {
    if (obj == null) return false
    if (getClass != obj.asInstanceOf[AnyRef].getClass) return false
  
    val other = obj.asInstanceOf[URIFile]
    if (this.location != other.location && (this.location == null || !this.location.equals(other.location))) return false
    true
  }

  override def hashCode: Int = {
    var hash = 3
    97 * hash + (if(location != null) location.hashCode else 0)
  }
}
