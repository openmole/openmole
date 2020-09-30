/*
 * Copyright (C) 19/12/12 Romain Reuillon
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

package org.openmole.plugin.domain.modifier

import org.openmole.core.context.PrototypeSet
import org.openmole.core.workflow.domain._

import cats.implicits._

object SortedByDomain {

  implicit def isFinite[D, T, S] = new FiniteFromContext[SortedByDomain[D, T, S], T] with DomainInputs[SortedByDomain[D, T, S]] {
    override def computeValues(domain: SortedByDomain[D, T, S]) = domain.computeValues()
    override def inputs(domain: SortedByDomain[D, T, S]): PrototypeSet = domain.inputs
  }

}

case class SortedByDomain[D, T, S: scala.Ordering](domain: D, s: T ⇒ S)(implicit finite: FiniteFromContext[D, T], domainInputs: DomainInputs[D]) {
  def inputs = domainInputs.inputs(domain)
  def computeValues() =
    for {
      f ← finite.computeValues(domain)
    } yield f.toList.sortBy(s)
}

