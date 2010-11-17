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

package org.openmole.commons.tools.service

import scala.util.Random
import scala.collection.mutable.IndexedSeq

object RNG extends Random {
  
  /*def shuffle[T](a: Array[T]) = {
    for (i <- 1 until a.size reverse) {
      val j = nextInt (i + 1)
      val t = a(i)
      a(i) = a(j)
      a(j) = t
    }
    a
  }*/
  
  def shuffle[T](a: IndexedSeq[T]) = {
    for (i <- 1 until a.size reverse) {
      val j = nextInt (i + 1)
      val t = a(i)
      a(i) = a(j)
      a(j) = t
    }
    a
  }
}
