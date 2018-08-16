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

package org.openmole.core.event

import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable.{ ListBuffer, WeakHashMap }

object EventDispatcher {
  def apply() = new EventDispatcher
  case class EventListnerKey(obj: Any, listner: Listner[_])
}

class EventDispatcher {

  private val _eventId = new AtomicLong
  private lazy val listenerMap = new WeakHashMap[Any, collection.mutable.Set[Any]]

  def eventId = _eventId.getAndIncrement()

  def listen[T](obj: T)(listener: Listner[T]) = listenerMap.synchronized {
    listenerMap.getOrElseUpdate(obj, collection.mutable.Set()) += listener.asInstanceOf[Listner[Any]]
    EventDispatcher.EventListnerKey(obj, listener)
  }

  def unregister(key: EventDispatcher.EventListnerKey) = listenerMap.synchronized {
    listenerMap.get(key.obj).foreach(_ -= key.listner)
  }

  def trigger[T](obj: T, event: Event[T]) = {
    val listeners = listenerMap.synchronized { listenerMap.get(obj).getOrElse(List.empty) }
    for {
      l ← listeners
    } l.asInstanceOf[Listner[T]].lift(obj, event)
  }

}
