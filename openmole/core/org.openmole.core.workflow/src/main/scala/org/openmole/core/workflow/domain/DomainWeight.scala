package org.openmole.core.workflow.domain

/*
 * Copyright (C) 2025 Romain Reuillon
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


import org.openmole.core.keyword.*
import scala.annotation.implicitNotFound

trait LowPriorityDomainWeightImplicit:
  given one[D]: DomainWeight[D, Double] = DomainWeight[D, Double](_ => 1.0)

object DomainWeight extends LowPriorityDomainWeightImplicit:
  def apply[D, T](f: D => T) =
    new DomainWeight[D, T]:
      def apply(d: D) = f(d)

  given [D, T]: DomainWeight[Weight[D, T], T] = DomainWeight(_.weight)
  given [K, D, T](using inner: InnerDomain[K, D], b: DomainWeight[D, T]): DomainWeight[K, T] = DomainWeight(d => b(inner(d)))


@implicitNotFound("${D} is not a domain with a defined weight of type ${T}")
trait DomainWeight[-D, +T] :
  def apply(domain: D): T