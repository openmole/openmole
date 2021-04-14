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

package org.openmole.plugin.domain.collection

import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._
import cats.implicits._

object ListDomain {
  implicit def isFinite[T] = new DiscreteFromContext[ListDomain[T], T] {
    override def iterator(domain: ListDomain[T]) = domain.values.toList.sequence.map(_.iterator)
  }

  def apply[T](values: FromContext[T]*) = new ListDomain[T](values: _*)
}

sealed class ListDomain[T](val values: FromContext[T]*)
