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
import java.util.concurrent.{ Callable, TimeUnit, TimeoutException }

import com.google.common.cache.CacheBuilder
import gridscale._
import org.openmole.core.communication.storage._
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.replication.{ ReplicaCatalog, ReplicationStorage }
import org.openmole.core.serializer._
import org.openmole.core.threadprovider.{ ThreadProvider, Updater }
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.refresh._
import org.openmole.tool.cache._
import org.openmole.tool.logger.JavaLogger
import squants.time.TimeConversions._

import scala.ref.WeakReference

object StorageService extends JavaLogger {
  val DirRegenerate = ConfigurationLocation("StorageService", "DirRegenerate", Some(1 hours))
  val TmpDirRemoval = ConfigurationLocation("StorageService", "TmpDirRemoval", Some(30 days))
  val persistent = "persistent/"
  val tmp = "tmp/"

  def startGC(storage: StorageService[_])(implicit threadProvider: ThreadProvider, preference: Preference) =
    Updater.delay(new StoragesGC(WeakReference(storage)), preference(BatchEnvironment.StoragesGCUpdateInterval))

  implicit def replicationStorage[S](implicit token: AccessToken): ReplicationStorage[StorageService[S]] = new ReplicationStorage[StorageService[S]] {
    override def backgroundRmFile(storage: StorageService[S], path: String): Unit = storage.backgroundRmFile(path)
    override def exists(storage: StorageService[S], path: String): Boolean = storage.exists(path)
    override def id(storage: StorageService[S]): String = storage.id
  }

  def apply[S](
    s:                 S,
    root:              String,
    id:                String,
    environment:       BatchEnvironment,
    remoteStorage:     RemoteStorage,
    concurrency:       Int,
    isConnectionError: Throwable ⇒ Boolean
  )(implicit storageInterface: StorageInterface[S], threadProvider: ThreadProvider, preference: Preference) = {
    val usageControl = UsageControl(concurrency)
    val storage = new StorageService[S](s, root, id, environment, remoteStorage, usageControl, isConnectionError)
    startGC(storage)
    storage
  }
}

import org.openmole.plugin.environment.batch.storage.StorageService.Log._
import org.openmole.plugin.environment.batch.storage.StorageService._

class StorageService[S](
  s:                 S,
  val root:          String,
  val id:            String,
  val environment:   BatchEnvironment,
  val remoteStorage: RemoteStorage,
  val usageControl:  UsageControl,
  isConnectionError: Throwable ⇒ Boolean,
  qualityHysteresis: Int                 = 100
)(implicit storage: StorageInterface[S]) {

  import environment.services
  import environment.services._

  val _directoryCache = Cache {
    CacheBuilder.
      newBuilder().
      expireAfterWrite(preference(StorageService.DirRegenerate).millis, TimeUnit.MILLISECONDS).
      build[String, String]()
  }

  val _serializedRemoteStorage = Cache {
    val file = newFile.newFile("remoteStorage", ".xml")
    fileService.deleteWhenGarbageCollected(file)
    serializerService.serialiseAndArchiveFiles(remoteStorage, file)
    file
  }

  def serializedRemoteStorage = _serializedRemoteStorage()
  def directoryCache = _directoryCache()

  protected implicit def callable[T](f: () ⇒ T): Callable[T] = new Callable[T]() { def call() = f() }

  def clean(implicit token: AccessToken) = {
    replicaCatalog.deleteReplicas(this)
    rmDir(baseDir)
    directoryCache.invalidateAll
  }

  def baseDir(implicit token: AccessToken): String =
    unwrap { directoryCache.get("baseDir", () ⇒ createBasePath) }

  protected def createBasePath(implicit token: AccessToken) = {
    val rootPath = mkRootDir
    val basePath = storage.child(s, rootPath, baseDirName)
    util.Try(makeDir(basePath)) match {
      case util.Success(_) ⇒ basePath
      case util.Failure(e) ⇒
        if (exists(basePath)) basePath else throw e
    }
  }

  protected def mkRootDir(implicit token: AccessToken): String = synchronized {
    val paths = Iterator.iterate[Option[String]](Some(root))(p ⇒ p.flatMap(parent)).takeWhile(_.isDefined).toSeq.reverse.flatten

    paths.tail.foldLeft(paths.head.toString) {
      (path, file) ⇒
        val childPath = storage.child(s, path, storage.name(s, file))
        try makeDir(childPath)
        catch {
          case e: Throwable if isConnectionError(e) ⇒ throw e
          case e: Throwable                         ⇒ logger.log(FINE, "Error creating base directory " + root, e)
        }
        childPath
    }
  }

  def persistentDir(implicit token: AccessToken): String =
    unwrap { directoryCache.get("persistentDir", () ⇒ createPersistentDir) }

  private def createPersistentDir(implicit token: AccessToken) = {
    val persistentPath = storage.child(s, baseDir, persistent)
    if (!exists(persistentPath)) makeDir(persistentPath)

    def graceIsOver(name: String) =
      replicaCatalog.timeOfPersistent(name).map {
        _ + preference(ReplicaCatalog.ReplicaGraceTime).toMillis < System.currentTimeMillis
      }.getOrElse(true)

    val names = listNames(persistentPath)
    val inReplica = replicaCatalog.forPaths(names.map { storage.child(s, persistentPath, _) }, Seq(this.id)).map(_.path).toSet

    for {
      name ← names
      if graceIsOver(name)
    } {
      val path = storage.child(s, persistentPath, name)
      if (!inReplica.contains(path)) backgroundRmFile(path)
    }

    persistentPath
  }

  def tmpDir(implicit token: AccessToken) =
    unwrap { directoryCache.get("tmpDir", () ⇒ createTmpDir) }

  private def createTmpDir(implicit token: AccessToken) = {
    val time = System.currentTimeMillis

    val tmpNoTime = storage.child(s, baseDir, tmp)
    if (!exists(tmpNoTime)) makeDir(tmpNoTime)

    val removalDate = System.currentTimeMillis - preference(TmpDirRemoval).toMillis

    for (entry ← list(tmpNoTime)) {
      val childPath = storage.child(s, tmpNoTime, entry.name)

      def rmDir =
        try {
          val timeOfDir = (if (entry.name.endsWith("/")) entry.name.substring(0, entry.name.length - 1) else entry.name).toLong
          if (timeOfDir < removalDate) backgroundRmDir(childPath)
        }
        catch {
          case (ex: NumberFormatException) ⇒ backgroundRmDir(childPath)
        }

      entry.`type` match {
        case FileType.Directory ⇒ rmDir
        case FileType.File      ⇒ backgroundRmFile(childPath)
        case FileType.Link      ⇒ backgroundRmFile(childPath)
        case FileType.Unknown ⇒
          try rmDir
          catch {
            case e: Throwable ⇒ backgroundRmFile(childPath)
          }
      }
    }

    val tmpTimePath = storage.child(s, tmpNoTime, time.toString)
    util.Try(makeDir(tmpTimePath)) match {
      case util.Success(_) ⇒ tmpTimePath
      case util.Failure(e) ⇒
        if (exists(tmpTimePath)) tmpTimePath else throw e
    }
  }

  override def toString: String = id

  lazy val quality = QualityControl(qualityHysteresis)

  def exists(path: String)(implicit token: AccessToken): Boolean = token.access { quality { storage.exists(s, path) } }
  def listNames(path: String)(implicit token: AccessToken): Seq[String] = token.access { quality { storage.list(s, path).map(_.name) } }
  def list(path: String)(implicit token: AccessToken): Seq[ListEntry] = token.access { quality { storage.list(s, path) } }
  def makeDir(path: String)(implicit token: AccessToken): Unit = token.access { quality { storage.makeDir(s, path) } }
  def rmDir(path: String)(implicit token: AccessToken): Unit = token.access { quality { storage.rmDir(s, path) } }
  def rmFile(path: String)(implicit token: AccessToken): Unit = token.access { quality { storage.rmFile(s, path) } }
  def mv(from: String, to: String)(implicit token: AccessToken) = token.access { quality { storage.mv(s, from, to) } }

  def parent(path: String): Option[String] = storage.parent(s, path)
  def name(path: String) = storage.name(s, path)
  def child(path: String, name: String) = storage.child(s, path, name)

  def upload(src: File, dest: String, options: TransferOptions = TransferOptions.default)(implicit token: AccessToken) = token.access { quality { storage.upload(s, src, dest, options) } }
  def download(src: String, dest: File, options: TransferOptions = TransferOptions.default)(implicit token: AccessToken) = token.access { quality { storage.download(s, src, dest, options) } }

  def baseDirName = "openmole-" + preference(Preference.uniqueID) + '/'

  def backgroundRmFile(path: String) = JobManager ! DeleteFile(this, path, false)
  def backgroundRmDir(path: String) = JobManager ! DeleteFile(this, path, true)

}
