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

import java.util.concurrent.Executors
import org.openmole.misc.tools.service.Priority

object EventDispatcher {
  
  //TODO maybe change to fixed thread pool some time 
  private val Executor = Executors.newCachedThreadPool
   
  private val objectChangedMap = new ObjectListenerMap
  //private val objectChangedWithArgsMap = new ObjectListenerMap
  
  def registerForObjectChanged[T, L <: IObjectListener[T]](obj: T, priority: Int, listner: L, event: Event[T,L]) = 
    objectChangedMap.register(obj, priority, listner, event)
  
  def unregisterListener[T, L <: IObjectListener[T]](obj: T, listner: L, event: Event[T,L]) =
    objectChangedMap.unregister(obj, listner, event)
   
  def objectChanged[T, L <: IObjectListener[T]](obj: T, event: Event[T,L], args: Array[Object]) = {
    /* --- Listners without args ---*/

    val listeners = objectChangedMap.get(obj, event)
 
    for (listner <- listeners) {
      //Logger.getLogger(classOf[EventDispatcher].getName).fine("Event no arg " + event + " from " + obj.toString + " signaled to " + listner.toString)
      listner.eventOccured(obj, args)
    }
  }
   
  def objectChanged[T, L <: IObjectListener[T]](obj: T, event: Event[T,L]): Unit = objectChanged(obj, event, Array.empty)

}
