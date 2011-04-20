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

package org.openmole.misc.tools.obj


object OrderedTuple2Id {
  implicit def orderedTuple2IdOrdering[T1 <% Ordered[T1], T2 <% Ordered[T2], O <: OrderedTuple2Id[T1, T2]] = new Ordering[O] {
    override def compare(n1: O, n2: O) = {
      val cmpId = n1._1.compare(n2._1)
      if(cmpId != 0) cmpId
      else n1._2.compare(n2._2)
    }
  }
}


class OrderedTuple2Id[+T1 <% Ordered[T1], +T2 <% Ordered[T2]](val _1: T1, val _2: T2) extends Id {
  def id = (_1, _2)
}
