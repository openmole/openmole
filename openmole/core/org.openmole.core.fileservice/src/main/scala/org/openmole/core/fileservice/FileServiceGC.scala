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
import org.openmole.tool.file._
import org.openmole.core.updater.IUpdatable

class FileServiceGC extends IUpdatable {
  override def update: Boolean = {
    FileService.archiveCache.cacheMaps.synchronized {
      def invalidate =
        for {
          execution ← FileService.archiveCache.cacheMaps
          file ← execution._2
          if (!new File(file._1).exists)
        } yield (execution._1, file._1)

      invalidate.foreach(Function.tupled(FileService.archiveCache.invalidateCache))
    }

    FileService.hashCache.cacheMaps.synchronized {
      def invalidate =
        for {
          execution ← FileService.hashCache.cacheMaps
          file ← execution._2
          if !new File(file._1).exists
        } yield (execution._1, file._1)

      invalidate.foreach(Function.tupled(FileService.hashCache.invalidateCache))
    }

    FileDeleter.cleanDirectories.synchronized {
      def delete =
        for {
          directory ← FileDeleter.cleanDirectories
          if directory.isDirectoryEmpty
        } yield directory

      delete.foreach { dir ⇒ dir.delete; FileDeleter.cleanDirectories.remove(dir) }
    }

    true
  }
}
