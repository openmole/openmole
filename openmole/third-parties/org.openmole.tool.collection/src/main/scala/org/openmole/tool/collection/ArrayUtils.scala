/*
 * Copyright (C) 13/02/13 Romain Reuillon
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

package org.openmole.tool.collection

object ArrayUtils {

  def unsecureBuild(t: Manifest[?], values: Any*) = {
    val res = t.newArray(values.size)
    values.zipWithIndex.foreach {
      case (v, i) ⇒ java.lang.reflect.Array.set(res, i, v)
    }
    res
  }

  def unsecureConcat(t: Manifest[?], a1: Array[?], a2: Array[?]): Array[?] = {
    val res = t.newArray(a1.size + a2.size)
    (a1 ++ a2).zipWithIndex.foreach {
      case (v, i) ⇒ java.lang.reflect.Array.set(res, i, v)
    }
    res
  }

}
