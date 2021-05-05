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

object SizeRangeDomain {
  implicit def isFinite[T] = new DiscreteFromContextDomain[SizeRangeDomain[T], T] with BoundedFromContextDomain[SizeRangeDomain[T], T] with CenterFromContextDomain[SizeRangeDomain[T], T] {
    override def iterator(domain: SizeRangeDomain[T]) = domain.iterator
    override def max(domain: SizeRangeDomain[T]) = domain.max
    override def min(domain: SizeRangeDomain[T]) = domain.min
    override def center(domain: SizeRangeDomain[T]) = RangeDomain.rangeCenter(domain.range)
  }

  implicit def inputs[T]: RequiredInput[SizeRangeDomain[T]] = domain ⇒ RangeDomain.inputs.apply(domain.range) ++ domain.size.inputs
  implicit def validate[T]: ExpectedValidation[SizeRangeDomain[T]] = domain ⇒ RangeDomain.validate.apply(domain.range) ++ domain.size.validate

  def apply[T: RangeValue](min: FromContext[T], max: FromContext[T], size: FromContext[Int]): SizeRangeDomain[T] =
    apply(RangeDomain(min, max), size)

  def apply[T](range: RangeDomain[T], size: FromContext[Int]): SizeRangeDomain[T] =
    new SizeRangeDomain[T](range, size)

}

class SizeRangeDomain[T](val range: RangeDomain[T], val size: FromContext[Int]) extends SizeStep[T] {
  import range._

  def stepAndSize(minValue: T, maxValue: T) = size.map { size ⇒
    import ops._
    val s = size - 1
    val step = (maxValue - minValue) / fromInt(s)
    (step, s.toInt)
  }

  def min = range.min
  def max = range.max
}
