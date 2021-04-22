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

package org.openmole.plugin.domain.modifier

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

import cats._
import cats.implicits._

object TakeDomain {

  implicit def isDiscrete[D, T] = new DiscreteFromContextDomain[TakeDomain[D, T], T] with DomainInputs[TakeDomain[D, T]] {
    def inputs(domain: TakeDomain[D, T]) = domain.domainInputs.inputs(domain.domain)
    def iterator(domain: TakeDomain[D, T]) = (domain.discrete.iterator(domain.domain) map2 domain.size)((d, s) ⇒ d.slice(0, s).toIterator)
  }

}

case class TakeDomain[D, +T](domain: D, size: FromContext[Int])(implicit val discrete: DiscreteFromContextDomain[D, T], val domainInputs: DomainInputs[D])
