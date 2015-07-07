/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.core.workflow.domain

import org.openmole.core.workflow.data.{ RandomProvider, Context }

object UnrolledDomain {
  def apply[T: Manifest](domain: Domain[T] with Discrete[T]) = new UnrolledDomain[T](domain)
}

class UnrolledDomain[T: Manifest](val domain: Domain[T] with Discrete[T]) extends Domain[Array[T]] with Finite[Array[T]] {
  override def computeValues(context: Context)(implicit rng: RandomProvider): Iterable[Array[T]] =
    Seq(domain.iterator(context).toArray)
}
