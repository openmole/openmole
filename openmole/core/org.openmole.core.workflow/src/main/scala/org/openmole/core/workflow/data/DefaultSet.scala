/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.core.workflow.data

import scala.collection.SetLike
import scala.collection.immutable.TreeMap

object DefaultSet {
  val empty = DefaultSet(Iterable.empty)

  def apply(p: Traversable[Default[_]]): DefaultSet =
    new DefaultSet {
      val defaults = p.toIterable
    }

  def apply(p: Default[_]*): DefaultSet = DefaultSet(p)

}

trait DefaultSet extends Set[Default[_]] with SetLike[Default[_], DefaultSet] { self ⇒

  def defaults: Iterable[Default[_]]

  @transient lazy val defaultMap =
    TreeMap.empty[String, Default[_]] ++ defaults.map { p ⇒ (p.prototype.name, p) }

  override def empty = DefaultSet.empty
  override def iterator: Iterator[Default[_]] = defaultMap.values.iterator

  def +(p: Default[_]) = DefaultSet(p :: defaults.toList)

  def -(p: Default[_]) = DefaultSet((defaultMap - p.prototype.name).values.toList)

  def contains(p: Default[_]) = defaultMap.contains(p.prototype.name)

}
