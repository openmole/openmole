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

import java.util.concurrent.Executors

import org.openmole.commons.tools.service.Priority
import java.util.logging.Logger
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher
import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.commons.aspect.eventdispatcher.IObjectListenerWithArgs

object EventDispatcher {
  
  //TODO maybe change to fixed thread pool some time 
  val Executor = Executors.newCachedThreadPool
  
}

class EventDispatcher extends IEventDispatcher {

  import EventDispatcher._
  
  val asynchronousObjectChangedMap = new ObjectListenerMap[IObjectListener[AnyRef]]
  val synchronousObjectChangedMap = new ObjectListenerMap[IObjectListener[AnyRef]]
  val asynchronousObjectChangedWithArgsMap = new ObjectListenerMap[IObjectListenerWithArgs[AnyRef]]
  val synchronousObjectChangedWithArgsMap = new ObjectListenerMap[IObjectListenerWithArgs[AnyRef]]
  val asynchronousObjectConstructedMap = new ClassListenerMap[IObjectListener[AnyRef]]
  val synchronousObjectConstructedMap = new ClassListenerMap[IObjectListener[AnyRef]]

  
  override def registerForObjectChangedAsynchronous[T](obj: T, listner: IObjectListener[T] , event: String) = {
    asynchronousObjectChangedMap.register(obj.asInstanceOf[AnyRef], Priority.NORMAL, listner.asInstanceOf[IObjectListener[AnyRef]], event)
  }
    
  override def registerForObjectChangedSynchronous[T](obj: T, priority: Int, listner: IObjectListener[T], event: String) = {  
    synchronousObjectChangedMap.register(obj.asInstanceOf[AnyRef], priority, listner.asInstanceOf[IObjectListener[AnyRef]], event)
    //Logger.getLogger(classOf[EventDispatcher].getName).fine("All listners " + synchronousObjectChangedMap.toString)  
  }
  
  
  override def registerForObjectChangedAsynchronous[T](obj: T, listner: IObjectListenerWithArgs[T] , event: String) = {
    asynchronousObjectChangedWithArgsMap.register(obj.asInstanceOf[AnyRef], Priority.NORMAL, listner.asInstanceOf[IObjectListenerWithArgs[AnyRef]], event)
  }
  
  override def registerForObjectChangedSynchronous[T](obj: T, priority: Int, listner: IObjectListenerWithArgs[T], event: String) = {
    synchronousObjectChangedWithArgsMap.register(obj.asInstanceOf[AnyRef], priority, listner.asInstanceOf[IObjectListenerWithArgs[AnyRef]], event)
  }
    
  override def registerForObjectConstructedSynchronous[T](c: Class[T], priority: Int, listner: IObjectListener[T]) = {
    synchronousObjectConstructedMap.register(c, priority, listner.asInstanceOf[IObjectListener[AnyRef]])
  }  
  
  override def registerForObjectConstructedAsynchronous[T](c: Class[T], listner: IObjectListener[T]) = {
    asynchronousObjectConstructedMap.register(c, Priority.NORMAL, listner.asInstanceOf[IObjectListener[AnyRef]])
  }
  
  
  override def objectChanged[T](obj: T, event: String, args: Array[Object]) = {
    //Logger.getLogger(classOf[EventDispatcher].getName).fine("Signal event " + event + " signaled from " + obj.toString)
    val objectChangedWithTypeAsynchronouslisteners = asynchronousObjectChangedMap.get(obj.asInstanceOf[AnyRef], event)
    val objectChangedWithTypeAsynchronouslistenersWithArgs = asynchronousObjectChangedWithArgsMap.get(obj.asInstanceOf[AnyRef], event)

    //Avoid creating threads if no listners
    if (objectChangedWithTypeAsynchronouslisteners.iterator.hasNext) {

      Executor.submit(new Runnable {

          override def run = {

            /* --- Listners without args ---*/

            objectChangedWithTypeAsynchronouslisteners.synchronized  {
              for (listner <- objectChangedWithTypeAsynchronouslisteners) {
                listner.eventOccured(obj.asInstanceOf[AnyRef])
              }
            }

            /* --- Listners with args ---*/

            objectChangedWithTypeAsynchronouslistenersWithArgs.synchronized {
              for (listner <- objectChangedWithTypeAsynchronouslistenersWithArgs) {
                listner.eventOccured(obj.asInstanceOf[AnyRef], args)
              }
            }
          }
        })
    }
    
    /* --- Listners without args ---*/

    val listeners = synchronousObjectChangedMap.get(obj.asInstanceOf[AnyRef], event)
     
    listeners.synchronized  {
      for (listner <- listeners) {
        //Logger.getLogger(classOf[EventDispatcher].getName).fine("Event no arg " + event + " from " + obj.toString + " signaled to " + listner.toString)
        listner.eventOccured(obj.asInstanceOf[AnyRef])
      }
    }

    /* --- Listners with args ---*/

    val listenersWithArgs = synchronousObjectChangedWithArgsMap.get(obj.asInstanceOf[AnyRef], event)

    listenersWithArgs.synchronized {
      for (listner <- listenersWithArgs) {
        //Logger.getLogger(classOf[EventDispatcher].getName).fine("Event no arg " + event + " from " + obj.toString + " signaled to " + listner.toString)
        listner.eventOccured(obj.asInstanceOf[AnyRef], args)
      }
    }
    
  }
   
  override def objectChanged[T](obj: T, event: String) = objectChanged(obj.asInstanceOf[AnyRef], event, Array.empty)

  override def objectConstructed(obj: Object) = {

    val c = obj.getClass
    val asynchronousObjectConstructedListners = asynchronousObjectConstructedMap.get(c)


    //Dont create thread if no assynchronous listner are registred
    if (asynchronousObjectConstructedListners.iterator.hasNext) {
      Executor.submit(new Runnable() {

          override def run = {
            asynchronousObjectConstructedListners.synchronized  {
              for (listner <- asynchronousObjectConstructedListners) {
                listner.eventOccured(obj)
              }
            }
          }
        })
    }

    val listeners = synchronousObjectConstructedMap.get(c)

    listeners.synchronized {
      for (listner <- listeners) {
        listner.eventOccured(obj)
      }
    }
  }

}
