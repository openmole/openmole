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

package org.openmole.core.implementation.tools

import org.openmole.core.model.tools._

object Filter {

  class Filter[T](ts: Set[T]) extends IFilter[T] {
    def apply(t: T) = ts.contains(t)
  }

  class FilterNot[T](ts: Set[T]) extends IFilter[T] {
    def apply(t: T) = !ts.contains(t)
  }

  def apply[T](ts: T*) = new Filter(ts.toSet)
  def not[T](ts: T*) = new FilterNot(ts.toSet)
  def empty[T] =
    new IFilter[T] {
      def apply(t: T) = false
    }

}

