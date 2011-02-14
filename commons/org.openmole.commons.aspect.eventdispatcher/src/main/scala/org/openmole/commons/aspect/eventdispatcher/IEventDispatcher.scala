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

trait IEventDispatcher {
    def registerForObjectChangedAsynchronous[T](obj: T, listner: IObjectListener[T] , event: String)
    def registerForObjectChangedSynchronous[T](obj: T, priority: Int, listner: IObjectListener[T], event: String)

    def registerForObjectChangedAsynchronous[T](obj: T, listner: IObjectListenerWithArgs[T], event: String)
    def registerForObjectChangedSynchronous[T](obj: T, priority: Int, listner: IObjectListenerWithArgs[T], event: String)

    def registerForObjectConstructedSynchronous[T](c: Class[T], priority: Int, listner: IObjectListener[T])
    def registerForObjectConstructedAsynchronous[T](c: Class[T], listner: IObjectListener[T])
    
    def objectChanged[T](obj: T, event: String)
    def objectChanged[T](obj: T, event: String, args: Array[Object])
    def objectConstructed(obj: Object)
}
