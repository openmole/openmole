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

package org.openmole.commons.aspect.eventdispatcher.internal

import scala.collection.immutable.TreeMap
import scala.collection.mutable.WeakHashMap


class ObjectListenerMap[T] {
    
  val listnerTypeMap = new WeakHashMap[Object, TreeMap[String, SortedListners[T]]]
  
  private def getOrCreateListners(obj: Object, event: String):  SortedListners[T] = {
    listnerTypeMap.synchronized {
      listnerTypeMap.get(obj) match { 
        case Some(listners) => 
          listners.get(event) match {
            case Some(sortedListners) => 
              sortedListners
            case None => 
              val sortedListners = new SortedListners[T]
              val added = listners + ((event, sortedListners))
              listnerTypeMap += ((obj, added))
              sortedListners
          }
        case None => 
          val sortedListners = new SortedListners[T]
          listnerTypeMap += ((obj, TreeMap[String, SortedListners[T]]((event, sortedListners))))
          sortedListners
      } 
    }
  }

  def get(obj: Object, event: String): Iterable[T] = {
    listnerTypeMap.get(obj) match {
      case None => Iterable.empty
      case Some(map) => map.get(event) match {
          case None => Iterable.empty
          case Some(listners) => listners
        }
    }
  }

  def register(obj: Object, priority: Int, listner: T, event: String) = {
    val sortedListners = getOrCreateListners(obj, event)
    sortedListners.synchronized {
      sortedListners.register(priority, listner)
    }
  }
}
