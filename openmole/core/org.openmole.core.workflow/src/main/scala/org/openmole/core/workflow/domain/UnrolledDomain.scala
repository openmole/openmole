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

object UnrolledDomain {
  implicit def isDiscrete[T: Manifest, D] =
    new Finite[Array[T], UnrolledDomain[T, D]] {
      override def computeValues(domain: UnrolledDomain[T, D]) =
        domain.computeValues(domain)
    }

  def apply[T: Manifest, D](d: D)(implicit discrete: Discrete[T, D]) = new UnrolledDomain[T, D](d)
}

class UnrolledDomain[T: Manifest, D](d: D)(implicit val discrete: Discrete[T, D]) {
  def computeValues(domain: UnrolledDomain[T, D]) = domain.discrete.iterator(d).map(d ⇒ Seq(d.toArray))
}
