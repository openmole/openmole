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

import scala.collection.mutable.WeakHashMap
import scala.ref.WeakReference

object FileService {
  val GCInterval = ConfigurationLocation("FileService", "GCInterval", Some(5 minutes))
  def apply()(implicit preference: Preference, threadProvider: ThreadProvider) = {
    val fs = new FileService
    fs.start
    fs
  }
}

class FileService {

  private[fileservice] val hashCache = new AssociativeCache[String, Hash]
  private[fileservice] val archiveCache = new AssociativeCache[String, FileCache]

  private val fileDeleter = new FileDeleter(WeakReference(this))
  private val gc = new FileServiceGC(WeakReference(this))

  def hash(file: File)(implicit newFile: NewFile): Hash =
    hash(file, if (file.isDirectory) archiveForDir(file).file else file)

  def invalidate(key: Object, file: File) = hashCache.invalidateCache(key, file.getAbsolutePath)

  def archiveForDir(file: File)(implicit newFile: NewFile): FileCache = archiveForDir(file, file)

  def hash(key: Object, file: File)(implicit newFile: NewFile): Hash =
    hashCache.cache(
      key,
      file.getCanonicalPath
    ) { _ ⇒ computeHash(if (file.isDirectory) archiveForDir(key, file).file else file) }

  def archiveForDir(key: Object, directory: File)(implicit newFile: NewFile) =
    archiveCache.cache(key, directory.getAbsolutePath) { _ ⇒
      val ret = newFile.newFile("archive", ".tar")
      directory.archive(ret, time = false)
      FileCache(ret)(this)
    }

  private val deleters = new WeakHashMap[File, DeleteOnFinalize]

  def deleteWhenGarbageCollected(file: File): File = deleters.synchronized {
    deleters += file → new DeleteOnFinalize(file.getAbsolutePath, fileDeleter)
    file
  }

  def assynchonousRemove(file: File): Boolean = fileDeleter.assynchonousRemove(file)

  def start(implicit preference: Preference, threadProvider: ThreadProvider): Unit = {
    fileDeleter.start(threadProvider)
    Updater.delay(gc, preference(FileService.GCInterval))
  }

}

