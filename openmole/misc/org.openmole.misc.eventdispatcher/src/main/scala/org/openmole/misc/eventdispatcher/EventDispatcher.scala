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

import java.util.concurrent.Executors
import org.openmole.misc.tools.service.Priority

object EventDispatcher {

  private val triggerListenerMap = new ObjectListenerMap
  //private val objectChangedWithArgsMap = new ObjectListenerMap

  def listen[T, L <: EventListener[T], E <: Event[T]](obj: T, listner: L, event: Class[E]) =
    triggerListenerMap.register(obj, Priority.NORMAL, listner, event)

  def listen[T, L <: EventListener[T], E <: Event[T]](obj: T, priority: Int, listner: L, event: Class[E]) =
    triggerListenerMap.register(obj, priority, listner, event)

  def unlisten[T, L <: EventListener[T], E <: Event[T]](obj: T, listner: L, event: Class[E]) =
    triggerListenerMap.unregister(obj, listner, event)

  def trigger[T](obj: T, event: Event[T]) = {
    /* --- Listners without args ---*/
    val listeners = triggerListenerMap.get(obj, event.getClass.asInstanceOf[Class[Event[T]]])

    for (listener ← listeners) listener.triggered(obj, event)
  }

}
