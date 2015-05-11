/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.core.workflow

import java.io.PrintStream

package object execution {

  def display(stream: PrintStream, label: String, content: String) =
    if (!content.isEmpty) {
      stream.synchronized {
        val fullLength = 40
        val dashes = fullLength - label.size / 2
        val header = ("-" * dashes) + label + ("-" * dashes)
        val footer = "-" * header.size
        stream.println(header)
        stream.print(content)
        stream.println(footer)
      }
    }

}
