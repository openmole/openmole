/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.tool.collection

import scala.concurrent.stm._

class OrderedSlidingList[T](size: () => Int)(implicit o: Ordering[T]) {

  private val _values = Ref(List[T]())

  def values = _values.single()

  def +=(e: T) = atomic { implicit ctx =>
    import o._

    def insertValue(reversedHead: List[T], tail: List[T]): List[T] = {
      tail match {
        case Nil                    => (e :: reversedHead).reverse
        case l @ (h :: _) if h >= e => reversedHead.reverse ::: (e :: tail)
        case h :: t                 => insertValue(h :: reversedHead, t)
      }
    }

    _values() = insertValue(List.empty, _values())
    if (_values().size >= size()) _values() = _values().drop(1)
  }

  def clear() = atomic { implicit ctx =>
    val res = _values()
    _values() = List.empty
    res
  }
}
