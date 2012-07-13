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

package org.openmole.misc.fileservice

import com.ice.tar.TarOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.logging.Logger
import org.openmole.misc.tools.cache.AssociativeCache
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.tools.io.TarArchiver._
import org.openmole.misc.tools.service.IHash
import org.openmole.misc.filecache.FileCacheDeleteOnFinalize
import org.openmole.misc.filecache.IFileCache
import org.openmole.misc.hashservice.HashService
import org.openmole.misc.updater.Updater
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace

object FileService {
  val GCInterval = new ConfigurationLocation("FileService", "GCInterval")
  Workspace += (GCInterval, "PT5M")

  class CachedArchiveForDir(file: File, val lastModified: Long) extends FileCacheDeleteOnFinalize(file)

  private[fileservice] val hashCache = new AssociativeCache[String, IHash]
  private[fileservice] val archiveCache = new AssociativeCache[String, CachedArchiveForDir]

  Updater.delay(new FileServiceGC, Workspace.preferenceAsDurationInMs(FileService.GCInterval))

  def hash(file: File): IHash =
    if (file.isDirectory) hash(archiveForDir(file).file(false), file)
    else hash(file, file)

  def archiveForDir(file: File): IFileCache = archiveForDir(file, file)

  def hash(key: Object, file: File): IHash = hashCache.cache(key, file.getAbsolutePath, HashService.computeHash(file))

  def archiveForDir(key: Object, file: File): IFileCache = {

    //invalidateDirCacheIfModified(file, cacheLenght)

    archiveCache.cache(key, file.getAbsolutePath, {
      val ret = Workspace.newFile("archive", ".tar");
      val os = new TarOutputStream(new FileOutputStream(ret))
      try os.createDirArchiveWithRelativePathNoVariableContent(file)
      finally os.close

      new CachedArchiveForDir(ret, file.lastModification)
    })
  }

  private[fileservice] def invalidateDirCacheIfModified(file: File, cacheLenght: Object) =
    archiveCache.cached(cacheLenght, file.getAbsolutePath) match {
      case None ⇒
      case Some(cached) ⇒
        if (cached.lastModified < file.lastModification) {
          archiveCache.invalidateCache(cacheLenght, file.getAbsolutePath)
        }
    }

}
