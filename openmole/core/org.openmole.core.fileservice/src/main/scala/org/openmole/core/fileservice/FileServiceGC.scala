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

import org.openmole.core.threadprovider.IUpdatable
import scala.ref.WeakReference

class FileServiceGC(fileService: WeakReference[FileService]) extends IUpdatable {

  override def update(): Boolean =
    fileService.get match {
      case Some(fileService) ⇒
        fileService.deleteEmpty.synchronized {
          def deleteEmpty(files: Vector[File]): Vector[File] = {
            val (deletedDirectories, nonEmptyDirectories) = files.filter(_.exists()).partition(_.delete())
            if (!deletedDirectories.isEmpty) deleteEmpty(nonEmptyDirectories) else nonEmptyDirectories
          }

          val nonEmpty = deleteEmpty(fileService.deleteEmpty.toVector)

          fileService.deleteEmpty.clear()
          fileService.deleteEmpty ++= nonEmpty
        }
        true
      case None ⇒ false
    }
}
