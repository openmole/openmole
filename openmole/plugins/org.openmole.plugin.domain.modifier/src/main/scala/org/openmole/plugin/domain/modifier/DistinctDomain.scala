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

package org.openmole.plugin.domain.modifier

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.domain._

object DistinctDomain {

  implicit def isDiscrete[D, T] =
    new DiscreteFromContext[DistinctDomain[D, T], T] with DomainInputs[DistinctDomain[D, T]] {
      override def iterator(domain: DistinctDomain[D, T]) = FromContext { p ⇒
        import p._
        domain.discrete.iterator(domain.domain).from(context).toSeq.distinct.iterator
      }

      override def inputs(domain: DistinctDomain[D, T]) = domain.inputs.inputs(domain.domain)
    }

}

case class DistinctDomain[D, +T](domain: D)(implicit val discrete: DiscreteFromContext[D, T], val inputs: DomainInputs[D])