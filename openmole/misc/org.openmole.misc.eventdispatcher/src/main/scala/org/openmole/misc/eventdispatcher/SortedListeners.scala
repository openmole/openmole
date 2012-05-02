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

class SortedListeners[T] extends Iterable[T] {

  class Listners(listners: List[(Int, T)]) {

    def iterator = sorted.iterator
    lazy val sorted = listners.sortBy(_._1).map(_._2).reverse

    def isEmpty = listners.isEmpty
    def -(elt: T) = new Listners(listners.filter(_._2 != elt))
    def +(priority: Int, elt: T) = new Listners(priority -> elt :: listners.filter(_._2 != elt))

  }

  var listeners = new Listners(List.empty)

  def register(priority: Int, listener: T) = {
    listeners += (priority, listener)
  }

  override def isEmpty = listeners.isEmpty

  def -=(listener: T) = {
    listeners -= listener
  }

  override def iterator = listeners.iterator
}
