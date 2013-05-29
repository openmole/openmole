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

package org.openmole.core.serializer.converter

import java.io.File
import org.openmole.core.serializer.structure.FileInfo

class DeserializerWithFileInjectionFromPathHash extends Deserializer with Factory.XStreamPool {

  var files: PartialFunction[FileInfo, File] = null
  registerConverter(new FilePathHashInjecter(this, reflectionConverter))
  def clean = files = null

  def matchingFile(file: FileInfo) = files(file)

}
