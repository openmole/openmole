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

package org.openmole.core.batch.storage

import java.net.URI
import java.nio.file._
import java.util.concurrent.{ Callable, TimeUnit }
import com.google.common.cache.CacheBuilder
import org.openmole.core.tools.cache._
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.refresh._
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.filedeleter.FileDeleter
import org.openmole.core.serializer._
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import fr.iscpif.gridscale.storage.{ ListEntry, FileType }
import java.io._
import org.openmole.tool.logger.Logger

object StorageService extends Logger {

  val DirRegenerate = new ConfigurationLocation("StorageService", "DirRegenerate")
  Workspace += (DirRegenerate, "P1D")

}

import StorageService.Log._

trait StorageService extends BatchService with Storage {

  def url: URI
  val id: String
  val remoteStorage: RemoteStorage

  def persistentDir(implicit token: AccessToken): String
  def tmpDir(implicit token: AccessToken): String

  @transient private lazy val _directoryCache =
    CacheBuilder.
      newBuilder().
      expireAfterWrite(Workspace.preferenceAsDuration(StorageService.DirRegenerate).toMillis, TimeUnit.MILLISECONDS).
      build[String, String]()

  @transient private lazy val _serializedRemoteStorage = {
    val file = Workspace.newFile("remoteStorage", ".xml")
    FileDeleter.deleteWhenGarbageCollected(file)
    SerialiserService.serialiseAndArchiveFiles(remoteStorage, file)
    file
  }

  def serializedRemoteStorage = synchronized { _serializedRemoteStorage }
  def directoryCache = synchronized { _directoryCache }

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
          case e: Throwable ⇒ logger.log(FINE, "Error creating base directory " + root, e)
        }
        childPath
    }
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

  def baseDirName = Workspace.preference(Workspace.uniqueID) + '/'

  def backgroundRmFile(path: String) = BatchEnvironment.jobManager ! DeleteFile(this, path, false)
  def backgroundRmDir(path: String) = BatchEnvironment.jobManager ! DeleteFile(this, path, true)

}
