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
import java.util.regex.Pattern

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
import scala.util.Try

object StorageService extends JavaLogger {
  val DirRegenerate = ConfigurationLocation("StorageService", "DirRegenerate", Some(1 hours))
  val TmpDirRemoval = ConfigurationLocation("StorageService", "TmpDirRemoval", Some(30 days))
  val persistent = "persistent/"
  val tmp = "tmp/"

  implicit def replicationStorage[S](implicit token: AccessToken, services: BatchEnvironment.Services): ReplicationStorage[StorageService[S]] = new ReplicationStorage[StorageService[S]] {
    override def backgroundRmFile(storage: StorageService[S], path: String): Unit = StorageService.backgroundRmFile(storage, path)
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
  )(implicit storageInterface: StorageInterface[S], threadProvider: ThreadProvider, preference: Preference, replicaCatalog: ReplicaCatalog) = {
    val usageControl = UsageControl(concurrency)

    import cats.implicits._

    val baseDirCache = Lazy(createBasePath(s, root, isConnectionError))
    def baseDir(accessToken: AccessToken) = baseDirCache()

    val persistentDirCache = baseDirCache.map { baseDir ⇒
      val dir = storageInterface.child(s, baseDir, persistent)
      if (!storageInterface.exists(s, dir)) storageInterface.makeDir(s, dir)
      cleanPersistent(s, dir, id)
      dir
    }

    def persistentDir(accessToken: AccessToken) = persistentDirCache()

    val tmpDirCache = baseDirCache.map { baseDir ⇒
      val dir = storageInterface.child(s, baseDir, tmp)
      if (!storageInterface.exists(s, dir)) storageInterface.makeDir(s, dir)
      cleanTmp(s, dir)
      dir
    }
    def tmpDir(token: AccessToken) = tmpDirCache()

    new StorageService[S](s, baseDir, persistentDir, tmpDir, id, environment, remoteStorage, usageControl)
  }

  def timedUniqName = org.openmole.tool.file.uniqName(System.currentTimeMillis.toString, ".rep", separator = "_")

  lazy val replicationPattern = Pattern.compile("(\\p{XDigit}*)_.*")
  def extractTimeFromName(name: String) = {
    val matcher = replicationPattern.matcher(name)
    if (!matcher.matches) None
    else Try(matcher.group(1).toLong).toOption
  }

  def cleanTmp[S](s: S, tmpDirectory: String)(implicit storageInterface: StorageInterface[S], preference: Preference) = {
    val entries = storageInterface.list(s, tmpDirectory)
    val removalDate = System.currentTimeMillis - preference(TmpDirRemoval).toMillis

    def remove(name: String) = extractTimeFromName(name).map(_ < removalDate).getOrElse(true)

    for {
      entry ← entries
      if remove(entry.name)
    } {
      val path = storageInterface.child(s, tmpDirectory, entry.name)
      if (entry.`type` == FileType.Directory) storageInterface.rmDir(s, path)
      else storageInterface.rmFile(s, path)
    }
  }

  def cleanPersistent[S](s: S, persistentPath: String, storageId: String)(implicit replicaCatalog: ReplicaCatalog, preference: Preference, storageInterface: StorageInterface[S]) = {
    def graceIsOver(name: String) =
      StorageService.extractTimeFromName(name).map {
        _ + preference(ReplicaCatalog.ReplicaGraceTime).toMillis < System.currentTimeMillis
      }.getOrElse(true)

    val names = storageInterface.list(s, persistentPath).map(_.name)
    val inReplica = replicaCatalog.forPaths(names.map { storageInterface.child(s, persistentPath, _) }, Seq(storageId)).map(_.path).toSet

    for {
      name ← names
      if graceIsOver(name)
    } {
      val path = storageInterface.child(s, persistentPath, name)
      if (!inReplica.contains(path)) storageInterface.rmFile(s, path)
    }
  }

  def createBasePath[S](s: S, root: String, isConnectionError: Throwable ⇒ Boolean)(implicit storageInterface: StorageInterface[S], preference: Preference) = {
    def baseDirName = "openmole-" + preference(Preference.uniqueID) + '/'

    def mkRootDir: String = synchronized {
      val paths = Iterator.iterate[Option[String]](Some(root))(p ⇒ p.flatMap(storageInterface.parent(s, _))).takeWhile(_.isDefined).toSeq.reverse.flatten

      paths.tail.foldLeft(paths.head.toString) {
        (path, file) ⇒
          val childPath = storageInterface.child(s, path, storageInterface.name(s, file))
          try storageInterface.makeDir(s, childPath)
          catch {
            case e: Throwable if isConnectionError(e) ⇒ throw e
            case e: Throwable                         ⇒ Log.logger.log(Log.FINE, "Error creating base directory " + root, e)
          }
          childPath
      }
    }

    val rootPath = mkRootDir
    val basePath = storageInterface.child(s, rootPath, baseDirName)
    util.Try(storageInterface.makeDir(s, basePath)) match {
      case util.Success(_) ⇒ basePath
      case util.Failure(e) ⇒
        if (storageInterface.exists(s, basePath)) basePath else throw e
    }
  }

  def backgroundRmFile(storageService: StorageService[_], path: String)(implicit services: BatchEnvironment.Services) = JobManager ! DeleteFile(storageService, path, false)
  def backgroundRmDir(storageService: StorageService[_], path: String)(implicit services: BatchEnvironment.Services) = JobManager ! DeleteFile(storageService, path, true)

}

class StorageService[S](
  s:                       S,
  val baseDirectory:       AccessToken ⇒ String,
  val persistentDirectory: AccessToken ⇒ String,
  val tmpDirectory:        AccessToken ⇒ String,
  val id:                  String,
  val environment:         BatchEnvironment,
  val remoteStorage:       RemoteStorage,
  val usageControl:        UsageControl,
  qualityHysteresis:       Int                  = 100
)(implicit storage: StorageInterface[S]) {

  override def toString: String = id

  lazy val quality = QualityControl(qualityHysteresis)

  def exists(path: String)(implicit token: AccessToken): Boolean = token.access { quality { storage.exists(s, path) } }

  def rmDir(path: String)(implicit token: AccessToken): Unit = token.access { quality { storage.rmDir(s, path) } }
  def rmFile(path: String)(implicit token: AccessToken): Unit = token.access { quality { storage.rmFile(s, path) } }

  def makeDir(path: String)(implicit token: AccessToken): Unit = token.access { quality { storage.makeDir(s, path) } }
  def child(path: String, name: String) = storage.child(s, path, name)

  def upload(src: File, dest: String, options: TransferOptions = TransferOptions.default)(implicit token: AccessToken) = token.access { quality { storage.upload(s, src, dest, options) } }
  def download(src: String, dest: File, options: TransferOptions = TransferOptions.default)(implicit token: AccessToken) = token.access { quality { storage.download(s, src, dest, options) } }

}
