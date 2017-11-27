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
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
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
  val GCInterval = ConfigurationLocation("FileService", "GCInterval", Some(1 minutes))

  val hashCacheSize = ConfigurationLocation("FileService", "HashCacheSize", Some(1000))
  val hashCacheTime = ConfigurationLocation("FileService", "HashCacheTime", Some(10 minutes))

  val archiveCacheSize = ConfigurationLocation("FileService", "ArchiveCacheSize", Some(1000))
  val archiveCacheTime = ConfigurationLocation("FileService", "ArchiveCacheTime", Some(10 minutes))

  def apply()(implicit preference: Preference, threadProvider: ThreadProvider) = {
    val fs = new FileService
    fs.start
    fs
  }
}

class FileService(implicit preference: Preference) {

  private[fileservice] val hashCache =
    CacheBuilder.newBuilder.maximumSize(preference(FileService.hashCacheSize)).
      expireAfterAccess(preference(FileService.hashCacheTime).millis, TimeUnit.MILLISECONDS).
      build[String, Hash]()

  private[fileservice] val archiveCache =
    CacheBuilder.newBuilder.maximumSize(preference(FileService.archiveCacheSize)).
      expireAfterAccess(preference(FileService.archiveCacheTime).millis, TimeUnit.MILLISECONDS).
      build[String, FileCache]()

  private[fileservice] val deleteEmpty = ListBuffer[File]()

  def hash(file: File)(implicit newFile: NewFile): Hash = {
    def hash = computeHash(if (file.isDirectory) archiveForDir(file).file else file)
    hashCache.get(file.getCanonicalPath, hash)
  }

  def archiveForDir(directory: File)(implicit newFile: NewFile): FileCache = {
    def archive = {
      val ret = newFile.newFile("archive", ".tar")
      directory.archive(ret, time = false)
      FileCache(ret)(this)
    }

    archiveCache.get(directory.getAbsolutePath, archive)
  }

  private val fileDeleter = new FileDeleter(WeakReference(this))
  private val gc = new FileServiceGC(WeakReference(this))
  private val deleters = new WeakHashMap[File, DeleteOnFinalize]

  def deleteWhenGarbageCollected(file: File): File = deleters.synchronized {
    deleters += file → new DeleteOnFinalize(file.getAbsolutePath, fileDeleter)
    file
  }

  def deleteWhenEmpty(directory: File) =
    if(directory.isEmpty) directory.recursiveDelete
    else deleteEmpty.synchronized { deleteEmpty += directory }

  def asynchronousRemove(file: File): Boolean = fileDeleter.asynchronousRemove(file)

  def start(implicit preference: Preference, threadProvider: ThreadProvider): Unit = {
    fileDeleter.start(threadProvider)
    Updater.delay(gc, preference(FileService.GCInterval))
  }

}

