/*
 * Copyright (C) 24/10/13 Romain Reuillon
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

package org.openmole.plugin.domain.range

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import cats.implicits._
import org.openmole.core.workflow.domain

object SizeRangeDomain {

  implicit def isDiscrete[T]: DiscreteFromContextDomain[SizeRangeDomain[T], T] = domain ⇒
    Domain(
      domain.iterator,
      domain.inputs,
      domain.validate
    )

  implicit def isBounded[T]: BoundedFromContextDomain[SizeRangeDomain[T], T] = domain ⇒
    Domain(
      (domain.min, domain.max),
      domain.inputs,
      domain.validate
    )

  implicit def hasCenter[T]: DomainCenterFromContext[SizeRangeDomain[T], T] = domain ⇒ RangeDomain.rangeCenter(domain.range)

  def apply[T: RangeValue](min: FromContext[T], max: FromContext[T], size: FromContext[Int]): SizeRangeDomain[T] =
    apply(RangeDomain(min, max), size)

  def apply[T](range: RangeDomain[T], size: FromContext[Int]): SizeRangeDomain[T] =
    new SizeRangeDomain[T](range, size)

}

class SizeRangeDomain[T](val range: RangeDomain[T], val size: FromContext[Int]):
  import range.*

  def iterator = SizeStep.iterator(range, stepAndSize)

  def stepAndSize(minValue: T, maxValue: T) = size.map { size ⇒
    import ops.*
    val s = size - 1
    val step = (maxValue - minValue) / fromInt(s)
    (step, s)
  }

  def min = range.min
  def max = range.max

  def inputs = range.inputs ++ size.inputs
  def validate = range.validate ++ size.validate

