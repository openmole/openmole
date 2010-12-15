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

package org.openmole.misc.fileservice.internal

import java.io.File
import java.util.logging.Logger
import org.openmole.commons.tools.cache.AssociativeCache
import org.openmole.commons.tools.io.FileOutputStream
import org.openmole.commons.tools.io.FileUtil
import org.openmole.commons.tools.io.TarArchiver
import org.openmole.commons.tools.service.IHash
import org.openmole.misc.filecache.FileCacheDeleteOnFinalize
import org.openmole.misc.filecache.IFileCache
import org.openmole.misc.fileservice.IFileService

class FileService extends IFileService {

  class CachedArchiveForDir(file: File, val lastModified: Long) extends FileCacheDeleteOnFinalize(file)
  class HashWithLastModified(val hash: IHash, val lastModified: Long)
    
  val hashCach = new AssociativeCache[String, HashWithLastModified]
  val archiveCache = new AssociativeCache[String, CachedArchiveForDir]

  override def hash(file: File): IHash = hash(file, file)

  override def archiveForDir(file: File): IFileCache = archiveForDir(file, file)
    
  override def hash(file: File, cacheLength: Object): IHash = {
    invalidateHashCacheIfModified(file, cacheLength)
    hashCach.cache(cacheLength, file.getAbsolutePath, new HashWithLastModified(Activator.getHashService.computeHash(file), file.lastModified)).hash
  }

  private def invalidateHashCacheIfModified(file: File, cacheLength: Object) = {
    val hashWithLastModified = hashCach.cached(cacheLength, file.getAbsolutePath) match {
      case None =>
      case Some(hash) => 
        if (hash.lastModified < FileUtil.lastModification(file)) {
          Logger.getLogger(classOf[FileService].getName).info("Invalidate cache " + file.getAbsolutePath)
          hashCach.invalidateCache(cacheLength, file.getAbsolutePath)
        }
    }
  }

  override def archiveForDir(file: File, cacheLenght: Object): IFileCache = {

    invalidateDirCacheIfModified(file, cacheLenght)

    return archiveCache.cache(cacheLenght, file.getAbsolutePath, {
        val ret = Activator.getWorkspace.newFile("archive", ".tar");
        val os = new FileOutputStream(ret)

        try {
          TarArchiver.createDirArchiveWithRelativePathNoVariableContent(file, os)
        } finally {
          os.close
        }

        new CachedArchiveForDir(ret, FileUtil.lastModification(file))
      })
  }

  private def invalidateDirCacheIfModified(file: File, cacheLenght: Object) = {
    archiveCache.cached(cacheLenght, file.getAbsolutePath) match {
      case None =>
      case Some(cached) => 
        if (cached.lastModified < FileUtil.lastModification(file)) {
          archiveCache.invalidateCache(cacheLenght, file.getAbsolutePath)
        }
    }
  }
}
