/*
 * Copyright (C) 2011 romain
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

import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._

import cats._
import cats.implicits._

object GroupDomain {

  implicit def isDiscrete[D, T: Manifest] = new DiscreteFromContext[GroupDomain[D, T], Array[T]] with DomainInputs[GroupDomain[D, T]] {
    override def iterator(domain: GroupDomain[D, T]) = {
      import domain._
      (discrete.iterator(d) map2 size)((it, s) ⇒ it.grouped(s) map (_.toArray))
    }

    override def inputs(domain: GroupDomain[D, T]) = domain.inputs.inputs(domain.d)
  }

}

case class GroupDomain[D, T: Manifest](d: D, size: FromContext[Int])(implicit val discrete: DiscreteFromContext[D, T], val inputs: DomainInputs[D])
