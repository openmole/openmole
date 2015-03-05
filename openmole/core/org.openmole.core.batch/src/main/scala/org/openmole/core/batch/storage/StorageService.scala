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
import java.nio.file.Paths
import java.util.concurrent.{ Callable, TimeUnit }
import com.google.common.cache.CacheBuilder
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.refresh._
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.filedeleter.FileDeleter
import org.openmole.core.serializer._
import org.openmole.core.tools.service.Logger
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import fr.iscpif.gridscale.storage.FileType
import java.io._
import scala.slick.driver.H2Driver.simple._

object StorageService extends Logger {

  val DirRegenerate = new ConfigurationLocation("StorageService", "DirRegenerate")
  Workspace += (DirRegenerate, "P1D")

}

import StorageService.Log._

trait StorageService extends BatchService with Storage {

  def url: URI
  val id: String
  def remoteStorage: RemoteStorage

  def persistentDir(implicit token: AccessToken, session: Session): String
  def tmpDir(implicit token: AccessToken): String

  @transient lazy val directoryCache =
    CacheBuilder.
      newBuilder().
      expireAfterWrite(Workspace.preferenceAsDuration(StorageService.DirRegenerate).toMillis, TimeUnit.MILLISECONDS).
      build[String, String]()

  protected implicit def callable[T](f: () ⇒ T): Callable[T] = new Callable[T]() { def call() = f() }

  def clean(implicit token: AccessToken, session: Session) = {
    ReplicaCatalog.onStorage(this).delete
    super.rmDir(baseDir)
    directoryCache.invalidateAll
  }

  @transient lazy val serializedRemoteStorage = {
    val file = Workspace.newFile("remoteStorage", ".xml")
    FileDeleter.deleteWhenGarbageCollected(file)
    SerialiserService.serialiseAndArchiveFiles(remoteStorage, file)
    file
  }

  def baseDir(implicit token: AccessToken): String =
    directoryCache.get(
      "baseDir",
      () ⇒ createBasePath
    )

  protected def createBasePath(implicit token: AccessToken) = {
    val rootPath = mkRootDir
    val basePath = child(rootPath, baseDirName)
    if (!exists(basePath)) makeDir(basePath)
    basePath
  }

  protected def mkRootDir(implicit token: AccessToken): String = synchronized {
    val paths = Iterator.iterate(Paths.get(root))(_.getParent).takeWhile(_ != null).toSeq.reverse

    paths.tail.foldLeft(paths.head.toString) {
      (path, file) ⇒
        val childPath = child(path, file.getFileName.toString)
        try makeDir(childPath)
        catch {
          case e: Throwable ⇒ logger.log(FINEST, "Error creating base directory " + root + e)
        }
        childPath
    }
  }

  override def toString: String = id

  def exists(path: String)(implicit token: AccessToken): Boolean = token.synchronized { super.exists(path) }
  def listNames(path: String)(implicit token: AccessToken): Seq[String] = token.synchronized { super.listNames(path) }
  def list(path: String)(implicit token: AccessToken): Seq[(String, FileType)] = token.synchronized { super.list(path) }
  def makeDir(path: String)(implicit token: AccessToken): Unit = token.synchronized { super.makeDir(path) }
  def rmDir(path: String)(implicit token: AccessToken): Unit = token.synchronized { super.rmDir(path) }
  def rmFile(path: String)(implicit token: AccessToken): Unit = token.synchronized { super.rmFile(path) }
  def mv(from: String, to: String)(implicit token: AccessToken) = token.synchronized { super.mv(from, to) }
  def openInputStream(path: String)(implicit token: AccessToken): InputStream = token.synchronized { super.openInputStream(path) }
  def openOutputStream(path: String)(implicit token: AccessToken): OutputStream = token.synchronized { super.openOutputStream(path) }

  def upload(src: File, dest: String)(implicit token: AccessToken) = token.synchronized { super.upload(src, dest) }
  def uploadGZ(src: File, dest: String)(implicit token: AccessToken) = token.synchronized { super.uploadGZ(src, dest) }
  def download(src: String, dest: File)(implicit token: AccessToken) = token.synchronized { super.download(src, dest) }
  def downloadGZ(src: String, dest: File)(implicit token: AccessToken) = token.synchronized { super.downloadGZ(src, dest) }

  def baseDirName = Workspace.preference(Workspace.uniqueID) + '/'

  def backgroundRmFile(path: String) = BatchEnvironment.jobManager ! DeleteFile(this, path, false)
  def backgroundRmDir(path: String) = BatchEnvironment.jobManager ! DeleteFile(this, path, true)

}
