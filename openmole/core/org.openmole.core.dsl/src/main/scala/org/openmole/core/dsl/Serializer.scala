/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.dsl

import org.openmole.core.dsl.extension.FileService
import org.openmole.core.serializer.SerializerService
import org.openmole.core.workspace.TmpDirectory

trait Serializer:
  def load(file: File)(implicit serialiserService: SerializerService) = serialiserService.deserialize[Object](file)
  def loadArchive(file: File)(implicit newFile: TmpDirectory, serialiserService: SerializerService, fileService: FileService) = serialiserService.deserializeAndExtractFiles[Object](file, deleteFilesOnGC = true, gz = true)

  def load(file: String)(implicit serialiserService: SerializerService): Object = load(new File(file))
  def loadArchive(file: String)(implicit newFile: TmpDirectory, serialiserService: SerializerService, fileService: FileService): Object = loadArchive(new File(file))

  def save(obj: Object, file: File)(implicit serialiserService: SerializerService) = serialiserService.serialize(obj, file)
  def saveArchive(obj: Object, file: File)(implicit newFile: TmpDirectory, serialiserService: SerializerService) = serialiserService.serializeAndArchiveFiles(obj, file, gz = true)

  def save(obj: Object, file: String)(implicit serialiserService: SerializerService): Unit = save(obj, new File(file))
  def saveArchive(obj: Object, file: String)(implicit newFile: TmpDirectory, serialiserService: SerializerService): Unit = saveArchive(obj, new File(file))
