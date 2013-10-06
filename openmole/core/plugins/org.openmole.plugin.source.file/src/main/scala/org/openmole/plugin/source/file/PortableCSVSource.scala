/*
 * Copyright (C) 03/10/13 Romain Reuillon
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

package org.openmole.plugin.source.file

import java.io.File
import org.openmole.plugin.source.file.CSVSource.CSVSourceBuilder

object PortableCSVSource {

  def apply(_file: File) =
    new CSVSourceBuilder { builder ⇒
      def toSource = new PortableCSVSource with Built {
        val file = _file
      }
    }

}

abstract class PortableCSVSource extends CSVSource {
  val file: File
  def path = file.getPath
}
