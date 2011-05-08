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

package org.openmole.misc.eventdispatcher

import scala.collection.immutable.SortedMap
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ListBuffer

class SortedListners[T] extends Iterable[T] {
  
  var listners: SortedMap[Int, List[T]] = TreeMap.empty[Int, List[T]](new Ordering[Int] {
      def compare(a: Int, b: Int) = (a - b)
    })
 
  def register(priority: Int, listner: T) = synchronized {
    listners += priority -> (listner +: listners.getOrElse(priority, Nil))
  }

  override def iterator: Iterator[T] = {  
    new Iterator[T] {
      val it = listners.valuesIterator
      var curSetIt = if(it.hasNext) it.next.iterator else Iterator.empty
      
      override def hasNext = {it.hasNext || curSetIt.hasNext}
        
      override def next = {
        if(!curSetIt.hasNext) curSetIt = it.next.iterator
        curSetIt.next
      }
    }
  }
  
}
