/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.misc.hashservice


import org.openmole.misc.tools.service.IHash

class SHA1Hash(val content: Array[Byte]) extends IHash {

  override def toString: String = IHash.hexString(content)

  override def hashCode: Int = content.deep.hashCode

  override def equals(obj: Any): Boolean = {
    if (obj == null) return false
    if (getClass != obj.asInstanceOf[AnyRef].getClass) return false
    val other = obj.asInstanceOf[SHA1Hash]
    content.deep == other.content.deep
  }

}
