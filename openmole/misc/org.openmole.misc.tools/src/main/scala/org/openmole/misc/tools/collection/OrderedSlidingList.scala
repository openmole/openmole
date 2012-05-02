/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.misc.tools.collection

import java.util.Collections
import java.util.LinkedList
import scala.collection.JavaConversions._

class OrderedSlidingList[T](size: Int)(implicit o: Ordering[T]) extends Iterable[T] {
  val averages = Collections.synchronizedList(new LinkedList[T])

  def iterator: Iterator[T] = averages.iterator

  def +=(sample: T) = averages.synchronized {
    import o._
    if (averages.size >= size) averages.remove(0)

    val it = averages.listIterator(averages.size)
    var inserted = false

    while (it.hasPrevious && !inserted) {
      val elt = it.previous
      if (elt <= sample) {
        it.add(sample)
        inserted = true
      }
    }

    if (!inserted) it.add(sample)
  }
}
