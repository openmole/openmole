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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object SortedByDomain {

  implicit def isDiscrete[D, T, S] = new DiscreteFromContextDomain[SortedByDomain[D, T, S], T] with DomainInputs[SortedByDomain[D, T, S]] {
    def inputs(domain: SortedByDomain[D, T, S]) = domain.domainInputs.inputs(domain.domain)
    def iterator(domain: SortedByDomain[D, T, S]) = FromContext { p ⇒
      import p._
      import domain.sOrdering
      domain.discrete.iterator(domain.domain).from(context).toSeq.sortBy(domain.s.from(context)).iterator
    }
  }

}

case class SortedByDomain[D, T, S](domain: D, s: FromContext[T ⇒ S])(implicit val discrete: DiscreteFromContextDomain[D, T], val domainInputs: DomainInputs[D], val sOrdering: scala.Ordering[S])

