/*
 * Copyright (C) 2010 Romain Reuillon
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

import scala.collection.mutable.OpenHashMap
import scala.collection.mutable.WeakHashMap

class ObjectListenerMap {

  val listnerTypeMap = new WeakHashMap[Any, OpenHashMap[Class[Event[Any]], SortedListeners[Any]]] //forSome {type T; type L <: IObjectListener[T]} = WeakHashMap.empty

  private def getOrCreateListeners[T, E <: Event[T]](obj: T, event: Class[E]): SortedListeners[EventListener[T]] =
    listnerTypeMap.getOrElseUpdate(obj, new OpenHashMap(1)).getOrElseUpdate(event.asInstanceOf[Class[Event[Any]]], new SortedListeners[Any]).asInstanceOf[SortedListeners[EventListener[T]]]

  def get[T, E <: Event[T]](obj: T, event: Class[E]): Iterable[EventListener[T]] = synchronized {
    listnerTypeMap.getOrElse(obj, new OpenHashMap(1)).getOrElse(event.asInstanceOf[Class[Event[Any]]], Iterable.empty).asInstanceOf[Iterable[EventListener[T]]]
  }

  def unregister[T, E <: Event[T], L <: EventListener[T]](obj: T, listener: L, event: Class[E]) = synchronized {
    val map = listnerTypeMap.getOrElse(obj, OpenHashMap.empty).asInstanceOf[OpenHashMap[Class[E], SortedListeners[L]]]

    map.get(event) match {
      case Some(listeners) ⇒
        listeners -= listener
        if (listeners.isEmpty) map -= event
        if (map.isEmpty) listnerTypeMap -= obj
      case None ⇒
    }

  }

  def register[T, L <: EventListener[T], E <: Event[T]](obj: T, priority: Int, listener: L, event: Class[E]) = synchronized {
    getOrCreateListeners(obj, event).register(priority, listener)
  }

}
