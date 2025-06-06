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

package org.openmole.core.serializer.file

import com.thoughtworks.xstream.converters.extended.FileConverter
import java.io.File

import org.openmole.core.serializer.PluginAndFilesListing



class FileConverterNotifier(fileUsed: File => Unit) extends FileConverter:

  override def toString(obj: Object): String =
    val file = obj.asInstanceOf[File]
    fileUsed(file)
    file.getPath

  override def canConvert(`type`: Class[?]): Boolean = classOf[File].isAssignableFrom(`type`)



