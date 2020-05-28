/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.fileservice

import java.io.File
import java.util.concurrent.TimeUnit

import com.google.common.cache._
import org.openmole.core.preference.{ PreferenceLocation, Preference }
import org.openmole.core.threadprovider.{ ThreadProvider, Updater }
import org.openmole.tool.hash._
import org.openmole.core.workspace._
import org.openmole.tool.cache.AssociativeCache
import org.openmole.tool.hash._
import org.openmole.tool.tar._
import org.openmole.tool.file._
import org.openmole.tool.thread._
import squants._
import squants.time.TimeConversions._

import scala.collection.mutable.{ ListBuffer, WeakHashMap }
import scala.ref.WeakReference

object FileService {
  val GCInterval = PreferenceLocation("FileService", "GCInterval", Some(1 minutes))

  val hashCacheSize = PreferenceLocation("FileService", "HashCacheSize", Some(1000))
  val hashCacheTime = PreferenceLocation("FileService", "HashCacheTime", Some(10 minutes))

  val archiveCacheSize = PreferenceLocation("FileService", "ArchiveCacheSize", Some(1000))
  val archiveCacheTime = PreferenceLocation("FileService", "ArchiveCacheTime", Some(10 minutes))

  def apply()(implicit preference: Preference, threadProvider: ThreadProvider) = {
    val fs = new FileService
    start(fs)
    fs
  }

  def start(fileService: FileService)(implicit preference: Preference, threadProvider: ThreadProvider): Unit = {
    fileService.fileDeleter.start(threadProvider)
    Updater.delay(fileService.gc, preference(FileService.GCInterval))
  }
}

object FileServiceCache {
  def apply()(implicit preference: Preference) = new FileServiceCache()
}

class FileServiceCache(implicit preference: Preference) {
  private[fileservice] val hashCache =
    CacheBuilder.newBuilder.maximumSize(preference(FileService.hashCacheSize)).
      expireAfterAccess(preference(FileService.hashCacheTime).millis, TimeUnit.MILLISECONDS).
      build[String, Hash]()

  private[fileservice] val archiveCache =
    CacheBuilder.newBuilder.maximumSize(preference(FileService.archiveCacheSize)).
      expireAfterAccess(preference(FileService.archiveCacheTime).millis, TimeUnit.MILLISECONDS).
      build[String, FileCache]()
}

class FileService(implicit preference: Preference) {

  private[fileservice] val deleteEmpty = ListBuffer[File]()

  def hashNoCache(file: File, hashType: HashType = SHA1)(implicit newFile: TmpDirectory) = {
    if (file.isDirectory) newFile.withTmpFile { archive ⇒
      file.archive(archive, time = false)
      hashFile(archive, hashType)
    }
    else hashFile(file, hashType)
  }

  def hash(file: File)(implicit newFile: TmpDirectory, fileServiceCache: FileServiceCache): Hash = {
    def hash = hashFile(if (file.isDirectory) archiveForDir(file).file else file)
    fileServiceCache.hashCache.get(file.getCanonicalPath, hash)
  }

  def archiveForDir(directory: File)(implicit newFile: TmpDirectory, fileServiceCache: FileServiceCache): FileCache = {
    def archive = {
      val ret = newFile.newFile("archive", ".tar")
      directory.archive(ret, time = false)
      FileCache(ret)(this)
    }

    fileServiceCache.archiveCache.get(directory.getAbsolutePath, archive)
  }

  private val fileDeleter = new FileDeleter(WeakReference(this))
  private val gc = new FileServiceGC(WeakReference(this))
  private val deleters = new WeakHashMap[File, DeleteOnFinalize]

  def deleteWhenGarbageCollected(file: File): File = deleters.synchronized {
    deleters += file → new DeleteOnFinalize(file.getAbsolutePath, fileDeleter)
    file
  }

  def deleteWhenEmpty(directory: File) =
    if (directory.exists() && !directory.delete()) deleteEmpty.synchronized { deleteEmpty += directory }

  def asynchronousRemove(file: File): Boolean = fileDeleter.asynchronousRemove(file)

}

