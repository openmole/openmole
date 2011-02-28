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

package org.openmole.misc.tools.service

object IHash {
  val HEX_CHAR_TABLE = List('0','1','2','3','4','5','6','7','8','9'
                            ,'a','b','c','d','e','f').map{ _.toByte }.toArray
  
  def hexString(raw: Array[Byte]): String = {
    val hex = new Array[Byte](2 * raw.length)
    var index = 0

    for (b <- raw) {
      val v = b & 0xFF
      hex(index) = HEX_CHAR_TABLE(v >>> 4)
      index += 1
      hex(index) = HEX_CHAR_TABLE(v & 0xF)
      index += 1
    }
    new String(hex, "ASCII")
  }
  
  implicit val ordering = new Ordering[IHash] {
    override def compare(left: IHash, right: IHash) = {
      left.toString.compare(right.toString)
    }
  }
}

trait IHash
