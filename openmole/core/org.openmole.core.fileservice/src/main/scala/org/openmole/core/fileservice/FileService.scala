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
import org.openmole.core.tools.cache.AssociativeCache
import org.openmole.tool.hash._
import org.openmole.core.updater.Updater
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.tool.hash.Hash
import org.openmole.tool.tar._
import concurrent.duration._

object FileService {
  val GCInterval = ConfigurationLocation("FileService", "GCInterval", Some(5 minutes))
  Workspace setDefault GCInterval

  private[fileservice] val hashCache = new AssociativeCache[String, Hash]
  private[fileservice] val archiveCache = new AssociativeCache[String, FileCache]

  Updater.delay(new FileServiceGC, Workspace.preference(FileService.GCInterval))

  def hash(file: File): Hash =
    hash(file, if (file.isDirectory) archiveForDir(file).file else file)

  def invalidate(key: Object, file: File) = hashCache.invalidateCache(key, file.getAbsolutePath)

  def archiveForDir(file: File): FileCache = archiveForDir(file, file)

  def hash(key: Object, file: File): Hash =
    hashCache.cache(
      key,
      file.getCanonicalPath,
      computeHash(if (file.isDirectory) archiveForDir(key, file).file else file)
    )

  def archiveForDir(key: Object, directory: File) = {
    archiveCache.cache(key, directory.getAbsolutePath, {
      val ret = Workspace.newFile("archive", ".tar")
      directory.archive(ret, time = false)
      FileCache(ret)
    })
  }

}

