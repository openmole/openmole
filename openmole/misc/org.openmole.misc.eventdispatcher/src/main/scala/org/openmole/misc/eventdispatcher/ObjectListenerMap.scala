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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.eventdispatcher

import scala.collection.mutable.HashMap
import scala.collection.mutable.WeakHashMap

class ObjectListenerMap {
    
  
  val listnerTypeMap = new WeakHashMap[Any, HashMap[Event[Any, IObjectListener[Any]], SortedListeners[Any]]] //forSome {type T; type L <: IObjectListener[T]} = WeakHashMap.empty
  
  private def getOrCreateListeners[T, L <: IObjectListener[T]](obj: T, event: Event[T, L]): SortedListeners[L] = 
    listnerTypeMap.getOrElseUpdate(obj, HashMap.empty).getOrElseUpdate(event.asInstanceOf[Event[Any, IObjectListener[Any]]], new SortedListeners[Any]).asInstanceOf[SortedListeners[L]]
  
  def get[T, L <: IObjectListener[T]](obj: T, event: Event[T, L]): Iterable[L] = synchronized {
    listnerTypeMap.getOrElse(obj, HashMap.empty).getOrElse(event.asInstanceOf[Event[Any, IObjectListener[Any]]], Iterable.empty).asInstanceOf[Iterable[L]]
  }

  def unregister[T, L <: IObjectListener[T]](obj: T, listener: L, event: Event[T, L]) = synchronized {
    val map = listnerTypeMap.getOrElse(obj, HashMap.empty).asInstanceOf[HashMap[Event[T, L], SortedListeners[L]]]
    
    map.get(event) match {
      case Some(listeners) => 
        listeners -= listener
        if(listeners.isEmpty) map -= event
        if(map.isEmpty) listnerTypeMap -= obj
      case None =>
    }
    
  }
  
  def register[T, L <: IObjectListener[T]](obj: T, priority: Int, listener: L, event: Event[T, L]) = synchronized {
    getOrCreateListeners(obj, event).register(priority, listener)
  }
  
}
