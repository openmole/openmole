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
import cats._
import cats.implicits._

object SlidingDomain {

  implicit def isDiscrete[D, T]: DiscreteFromContextDomain[SlidingDomain[D, T], Array[T]] = domain ⇒ domain.iterator
  implicit def inputs[D, T](implicit domainInputs: DomainInput[D]): DomainInput[SlidingDomain[D, T]] = domain ⇒ domainInputs(domain.domain) ++ domain.size.inputs ++ domain.step.inputs
  implicit def validate[D, T](implicit validate: DomainValidation[D]): DomainValidation[SlidingDomain[D, T]] = domain ⇒ validate(domain.domain) ++ domain.size.validate ++ domain.size.validate

}

case class SlidingDomain[D, T: Manifest](domain: D, size: FromContext[Int], step: FromContext[Int] = 1)(implicit discrete: DiscreteFromContextDomain[D, T]) {

  // FIXME convoluted expression, might be simplified
  def iterator =
    ((discrete.iterator(domain) map2 size)((a, b) ⇒ (a, b)) map2 step) {
      case ((it, si), st) ⇒ it.sliding(si, st).map(_.toArray)
    }

}
