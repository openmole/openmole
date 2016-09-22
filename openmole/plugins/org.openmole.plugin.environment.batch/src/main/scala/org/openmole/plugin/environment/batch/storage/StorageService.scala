/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.batch.storage

import java.io._
import java.net.{ SocketTimeoutException, URI }
import java.util.concurrent.{ Callable, TimeUnit, TimeoutException }

import com.google.common.cache.CacheBuilder
import fr.iscpif.gridscale.storage._
import org.openmole.core.communication.storage._
import org.openmole.core.fileservice.FileDeleter
import org.openmole.core.serializer._
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.openmole.plugin.environment.batch.control._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.refresh._
import org.openmole.plugin.environment.batch.replication.ReplicaCatalog
import org.openmole.tool.cache._
import org.openmole.tool.logger.Logger

import scala.concurrent.duration._

object StorageService extends Logger {

  val DirRegenerate = ConfigurationLocation("StorageService", "DirRegenerate", Some(1 hour))
  Workspace setDefault DirRegenerate

  val TmpDirRemoval = ConfigurationLocation("StorageService", "TmpDirRemoval", Some(30 days))
  Workspace setDefault TmpDirRemoval

  val persistent = "persistent/"
  val tmp = "tmp/"

}

import org.openmole.plugin.environment.batch.storage.StorageService.Log._
import org.openmole.plugin.environment.batch.storage.StorageService._

trait StorageService extends BatchService with Storage {

  def url: URI
  def id: String

  val remoteStorage: RemoteStorage

  val _directoryCache = Cache {
    CacheBuilder.
      newBuilder().
      expireAfterWrite(Workspace.preference(StorageService.DirRegenerate).toMillis, TimeUnit.MILLISECONDS).
      build[String, String]()
  }

  val _serializedRemoteStorage = Cache {
    val file = Workspace.newFile("remoteStorage", ".xml")
    FileDeleter.deleteWhenGarbageCollected(file)
    SerialiserService.serialiseAndArchiveFiles(remoteStorage, file)
    file
  }

  def serializedRemoteStorage = _serializedRemoteStorage()
  def directoryCache = _directoryCache()

  protected implicit def callable[T](f: () ⇒ T): Callable[T] = new Callable[T]() { def call() = f() }

  def clean(implicit token: AccessToken) = {
    ReplicaCatalog.deleteReplicas(this)
    rmDir(baseDir)
    directoryCache.invalidateAll
  }

  def baseDir(implicit token: AccessToken): String =
    unwrap { directoryCache.get("baseDir", () ⇒ createBasePath) }

  protected def createBasePath(implicit token: AccessToken) = {
    val rootPath = mkRootDir
    val basePath = child(rootPath, baseDirName)
    if (!exists(basePath)) makeDir(basePath)
    basePath
  }

  protected def mkRootDir(implicit token: AccessToken): String = synchronized {
    val paths = Iterator.iterate[Option[String]](Some(root))(p ⇒ p.flatMap(parent)).takeWhile(_.isDefined).toSeq.reverse.flatten

    paths.tail.foldLeft(paths.head.toString) {
      (path, file) ⇒
        val childPath = child(path, name(file))
        try makeDir(childPath)
        catch {
          case e: SocketTimeoutException ⇒ throw e
          case e: TimeoutException       ⇒ throw e
          case e: Throwable              ⇒ logger.log(FINE, "Error creating base directory " + root, e)
        }
        childPath
    }
  }

  def persistentDir(implicit token: AccessToken): String =
    unwrap { directoryCache.get("persistentDir", () ⇒ createPersistentDir) }

  private def createPersistentDir(implicit token: AccessToken) = {
    val persistentPath = child(baseDir, persistent)
    if (!exists(persistentPath)) makeDir(persistentPath)

    def graceIsOver(name: String) =
      ReplicaCatalog.timeOfPersistent(name).map {
        _ + Workspace.preference(ReplicaCatalog.ReplicaGraceTime).toMillis < System.currentTimeMillis
      }.getOrElse(true)

    val names = listNames(persistentPath)
    val inReplica = ReplicaCatalog.forPaths(names.map { child(persistentPath, _) }).map(_.path).toSet

    for {
      name ← names
      if graceIsOver(name)
    } {
      val path = child(persistentPath, name)
      if (!inReplica.contains(path)) backgroundRmFile(path)
    }
    persistentPath
  }

  def tmpDir(implicit token: AccessToken) =
    unwrap { directoryCache.get("tmpDir", () ⇒ createTmpDir) }

  private def createTmpDir(implicit token: AccessToken) = {
    val time = System.currentTimeMillis

    val tmpNoTime = child(baseDir, tmp)
    if (!exists(tmpNoTime)) makeDir(tmpNoTime)

    val removalDate = System.currentTimeMillis - Workspace.preference(TmpDirRemoval).toMillis

    for (entry ← list(tmpNoTime)) {
      val childPath = child(tmpNoTime, entry.name)

      def rmDir =
        try {
          val timeOfDir = (if (entry.name.endsWith("/")) entry.name.substring(0, entry.name.length - 1) else entry.name).toLong
          if (timeOfDir < removalDate) backgroundRmDir(childPath)
        }
        catch {
          case (ex: NumberFormatException) ⇒ backgroundRmDir(childPath)
        }

      entry.`type` match {
        case DirectoryType ⇒ rmDir
        case FileType      ⇒ backgroundRmFile(childPath)
        case LinkType      ⇒ backgroundRmFile(childPath)
        case UnknownType ⇒
          try rmDir
          catch {
            case e: Throwable ⇒ backgroundRmFile(childPath)
          }
      }
    }

    val tmpTimePath = child(tmpNoTime, time.toString)
    if (!exists(tmpTimePath)) makeDir(tmpTimePath)
    tmpTimePath
  }

  override def toString: String = id

  def exists(path: String)(implicit token: AccessToken): Boolean = token.access { _exists(path) }
  def listNames(path: String)(implicit token: AccessToken): Seq[String] = token.access { _listNames(path) }
  def list(path: String)(implicit token: AccessToken): Seq[ListEntry] = token.access { _list(path) }
  def makeDir(path: String)(implicit token: AccessToken): Unit = token.access { _makeDir(path) }
  def rmDir(path: String)(implicit token: AccessToken): Unit = token.access { _rmDir(path) }
  def rmFile(path: String)(implicit token: AccessToken): Unit = token.access { _rmFile(path) }
  def mv(from: String, to: String)(implicit token: AccessToken) = token.access { _mv(from, to) }
  def downloadStream(path: String, options: TransferOptions = TransferOptions.default)(implicit token: AccessToken): InputStream = token.access { _downloadStream(path, options) }
  def uploadStream(is: InputStream, path: String, options: TransferOptions = TransferOptions.default)(implicit token: AccessToken): Unit = token.access { _uploadStream(is, path, options) }

  def parent(path: String): Option[String] = _parent(path)
  def name(path: String) = _name(path)

  def upload(src: File, dest: String, options: TransferOptions = TransferOptions.default)(implicit token: AccessToken) = token.access { _upload(src, dest, options) }
  def download(src: String, dest: File, options: TransferOptions = TransferOptions.default)(implicit token: AccessToken) = token.access { _download(src, dest, options) }

  def baseDirName = "openmole-" + Workspace.preference(Workspace.uniqueIDLocation) + '/'

  def backgroundRmFile(path: String) = BatchEnvironment.jobManager ! DeleteFile(this, path, false)
  def backgroundRmDir(path: String) = BatchEnvironment.jobManager ! DeleteFile(this, path, true)

}
