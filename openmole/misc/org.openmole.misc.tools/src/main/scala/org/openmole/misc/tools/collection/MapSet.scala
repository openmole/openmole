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

package org.openmole.misc.tools.collection

import scala.collection._

class MapSet[A, B](
    val sets: Map[A, Set[B]] = Map[A, Set[B]]()) extends Map[A, B] with MapLike[A, B, MapSet[A, B]] {
  def get(key: A) = sets.getOrElse(key, Set[B]()).headOption
  def iterator = new Iterator[(A, B)] {
    private val seti = sets.iterator
    private var thiskey: Option[A] = None
    private var singles: Iterator[B] = Nil.iterator
    private def readyNext {
      while (seti.hasNext && !singles.hasNext) {
        val kv = seti.next
        thiskey = Some(kv._1)
        singles = kv._2.iterator
      }
    }
    def hasNext = {
      if (singles.hasNext) true
      else {
        readyNext
        singles.hasNext
      }
    }
    def next = {
      if (singles.hasNext) (thiskey.get, singles.next)
      else {
        readyNext
        (thiskey.get, singles.next)
      }
    }
  }
  def +[B1 >: B](kv: (A, B1)): MapSet[A, B] = {
    val value: B = kv._2.asInstanceOf[B]
    new MapSet(sets + ((kv._1, sets.getOrElse(kv._1, Set[B]()) + value)))
  }
  def -(key: A): MapSet[A, B] = new MapSet(sets - key)
  def -(kv: (A, B)): MapSet[A, B] = {
    val got = sets.get(kv._1)
    if (got.isEmpty || !got.get.contains(kv._2)) this
    else new MapSet(sets + ((kv._1, got.get - kv._2)))
  }
  override def empty = new MapSet(Map[A, Set[B]]())
}

