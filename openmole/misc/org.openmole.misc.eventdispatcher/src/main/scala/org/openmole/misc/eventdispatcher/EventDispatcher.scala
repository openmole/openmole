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
   
  private val objectChangedMap = new ObjectListenerMap[IObjectListener[AnyRef]]
  private val objectChangedWithArgsMap = new ObjectListenerMap[IObjectListenerWithArgs[AnyRef]]
  
  def registerForObjectChanged[T](obj: T, priority: Int, listner: IObjectListener[T], event: String) = 
    objectChangedMap.register(obj.asInstanceOf[AnyRef], priority, listner.asInstanceOf[IObjectListener[AnyRef]], event)
  
  def registerForObjectChanged[T](obj: T, priority: Int, listner: IObjectListenerWithArgs[T], event: String) =
    objectChangedWithArgsMap.register(obj.asInstanceOf[AnyRef], priority, listner.asInstanceOf[IObjectListenerWithArgs[AnyRef]], event)

  def unregisterListener[T](obj: T, listner: IObjectListener[T], event: String) =
    objectChangedMap.unregister(obj.asInstanceOf[AnyRef], listner.asInstanceOf[IObjectListener[AnyRef]], event)
   
  def unregisterListener[T](obj: T, listner: IObjectListenerWithArgs[T], event: String) =
    objectChangedWithArgsMap.unregister(obj.asInstanceOf[AnyRef], listner.asInstanceOf[IObjectListenerWithArgs[AnyRef]], event)
  
  def objectChanged[T](obj: T, event: String, args: Array[Object]) = {
    /* --- Listners without args ---*/

    val listeners = objectChangedMap.get(obj.asInstanceOf[AnyRef], event)
 
    for (listner <- listeners) {
      //Logger.getLogger(classOf[EventDispatcher].getName).fine("Event no arg " + event + " from " + obj.toString + " signaled to " + listner.toString)
      listner.eventOccured(obj.asInstanceOf[AnyRef])
    }
 
    /* --- Listners with args ---*/

    val listenersWithArgs = objectChangedWithArgsMap.get(obj.asInstanceOf[AnyRef], event)

    for (listner <- listenersWithArgs) {
      //Logger.getLogger(classOf[EventDispatcher].getName).fine("Event no arg " + event + " from " + obj.toString + " signaled to " + listner.toString)
      listner.eventOccured(obj.asInstanceOf[AnyRef], args)
    }
  }
   
  def objectChanged[T](obj: T, event: String): Unit = objectChanged(obj.asInstanceOf[AnyRef], event, Array.empty)

}
