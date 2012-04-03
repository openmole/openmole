/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.ui.console

import java.io.File
import org.openmole.core.serializer.SerializerService

class Serializer {
  def load(file: File) = SerializerService.deserialize[Object](file)
  def loadArchive(file: File) = SerializerService.deserializeAndExtractFiles[Object](file)
  
  def load(file: String): Object = load(new File(file))
  def loadArchive(file: String): Object = loadArchive(new File(file))
  
  def save(obj: Object, file: File) = SerializerService.serialize(obj, file)
  def saveArchive(obj: Object, file: File) = SerializerService.serializeAndArchiveFiles(obj, file)
  
  def save(obj: Object, file: String): Unit = save(obj, new File(file))
  def saveArchive(obj: Object, file: String): Unit = saveArchive(obj, new File(file))
}
