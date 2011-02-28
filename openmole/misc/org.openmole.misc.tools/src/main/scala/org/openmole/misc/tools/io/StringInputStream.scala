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

package org.openmole.misc.tools.io

import java.io.InputStream

class StringInputStream(val s: String) extends InputStream {

  private var strOffset  = 0
  private var charOffset = 0
  private var _available = s.length * 2
  
  override def available = _available

  override def read: Int = {
    if (available == 0) return -1
        
    _available -= 1
    val c = s.charAt(strOffset)

    if (charOffset == 0) {
      charOffset = 1
      return (c & 0x0000ff00) >> 8
    } else {
      charOffset = 0
      strOffset += 1
      return c & 0x000000ff
    }
  }

}
