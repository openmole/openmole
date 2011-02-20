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

package org.openmole.commons.aspect.eventdispatcher

import scala.collection.mutable.WeakHashMap

class ClassListenerMap[T] {

  val listnerMap = new WeakHashMap[Class[_], SortedListners[T]]

  def getOrCreateListners(c: Class[_]): SortedListners[T] = {
    listnerMap.synchronized  {
      listnerMap.get(c) match {
        case Some(listners) => listners
        case None => 
          val listners = new SortedListners[T]
          listnerMap(c)
          listners
      }
    }
  }
	
  def get(c: Class[_]): Iterable[T] = {
    listnerMap.synchronized  {
      listnerMap.getOrElse(c, Iterable.empty)
    }
  }

  def register(c: Class[_], priority: Int, listner: T) = {
    val listners = getOrCreateListners(c)

    listners.synchronized {
      listners.register(priority, listner)
    }
  }

}
