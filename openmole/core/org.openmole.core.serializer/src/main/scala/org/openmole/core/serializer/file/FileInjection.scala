/*
 * Copyright (C) 04/10/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.serializer.file

import com.thoughtworks.xstream.XStream
import org.openmole.core.exception.InternalProcessingError

import java.io.{File, InputStream}

class FileInjection(xStream: XStream):
  var injectedFiles: Map[String, File] = Map.empty

  xStream.registerConverter(new FileConverterInjecter(this))

  def getMatchingFile(file: String): File =
    injectedFiles.getOrElse(file, throw InternalProcessingError(s"Replacement for file $file not found among $injectedFiles"))

  def fromXML[T](is: InputStream, files: Map[String, File]): T =
    injectedFiles = files
    try
      xStream.fromXML(is).asInstanceOf[T]
    finally
      injectedFiles = null


