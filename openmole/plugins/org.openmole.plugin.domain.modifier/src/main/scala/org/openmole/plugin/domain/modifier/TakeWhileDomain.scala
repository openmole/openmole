package org.openmole.plugin.domain.modifier

/*
 * Copyright (C) 2021 Romain Reuillon
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

import org.openmole.core.context.PrototypeSet
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._

import cats._
import cats.implicits._

object TakeWhileDomain {

  implicit def isFinite[D, T] = new DiscreteFromContext[TakeWhileDomain[D, T], T] with DomainInputs[TakeWhileDomain[D, T]] {
    override def iterator(domain: TakeWhileDomain[D, T]) = domain.iterator
    override def inputs(domain: TakeWhileDomain[D, T]): PrototypeSet = domain.inputs
  }

}

case class TakeWhileDomain[D, T](domain: D, predicate: FromContext[T ⇒ Boolean])(implicit discrete: DiscreteFromContext[D, T], domainInputs: DomainInputs[D]) {
  def inputs = domainInputs.inputs(domain)
  def iterator =
    (discrete.iterator(domain) map2 predicate)((d, p) ⇒ d.takeWhile(p))
}
