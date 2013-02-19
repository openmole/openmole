/*
 * Copyright (C) 28/11/12 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.transition

trait IFilter[T] extends (T ⇒ Boolean)

trait Filter[T] extends IFilter[T] {
  def filtered: Set[T]
  def apply(t: T) = filtered.contains(t)
}

trait Keep[T] extends IFilter[T] {
  def kept: Set[T]
  def apply(t: T) = !kept.contains(t)
}

class EmptyFilter[T] extends IFilter[T] {
  def apply(t: T) = false
}

object Filter {

  def apply[T](ts: T*) = new Filter[T] {
    @transient lazy val filtered = ts.toSet
  }

  def empty[T] = new EmptyFilter[T]

}

object Keep {

  def apply[T](ts: T*) = new Keep[T] {
    @transient lazy val kept = ts.toSet
  }

}