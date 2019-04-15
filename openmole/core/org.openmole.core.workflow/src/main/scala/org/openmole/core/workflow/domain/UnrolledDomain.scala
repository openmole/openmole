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

import org.openmole.core.context._
import org.openmole.core.expansion._
import cats.implicits._

object UnrolledDomain {

  /**
   * Transform the unrolled domain into a finite domain by iterating
   * @tparam D
   * @tparam T
   * @return
   */
  implicit def isFinite[D, T: Manifest] =
    new Finite[UnrolledDomain[D, T], Array[T]] with DomainInputs[UnrolledDomain[D, T]] {
      override def computeValues(domain: UnrolledDomain[D, T]): FromContext[collection.Iterable[Array[T]]] =
        domain.discrete.iterator(domain.d).map { d ⇒ Seq(d.toArray).toIterable }

      override def inputs(domain: UnrolledDomain[D, T]): PrototypeSet = domain.inputs.inputs(domain.d)
    }

  def apply[D[_], T: Manifest](domain: D[T])(implicit discrete: Discrete[D[T], T], inputs: DomainInputs[D[T]] = DomainInputs.empty) =
    new UnrolledDomain[D[T], T](domain)

}

/**
 * Discrete domain
 * @param d domain
 */
class UnrolledDomain[D, T: Manifest](val d: D)(implicit val discrete: Discrete[D, T], val inputs: DomainInputs[D])
