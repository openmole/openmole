/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain.range

import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.{ BoundsFromContext, CenterFromContext, DiscreteFromContext }
import cats.implicits._

object RangeDomain {

  object ToRangeDomain {
    import org.openmole.tool.collection.DoubleRange
    implicit def doubleRangeIsRange: ToRangeDomain[DoubleRange, Double] = (range: DoubleRange) ⇒ RangeDomain(range.low, range.high)
    implicit def intRangeIsRange: ToRangeDomain[scala.Range, Int] = (range: scala.Range) ⇒ RangeDomain(range.min, range.max)
    implicit def intRangeIsRangeDouble: ToRangeDomain[scala.Range, Double] = (range: scala.Range) ⇒ RangeDomain(range.min.toDouble, range.max.toDouble)
    implicit def rangeDomainIsRange[T]: ToRangeDomain[RangeDomain[T], T] = (r: RangeDomain[T]) ⇒ r
  }

  trait ToRangeDomain[-D, T] {
    def apply(t: D): RangeDomain[T]
  }

  implicit def isBounded[D, T](implicit toRangeDomain: ToRangeDomain[D, T]) = new BoundsFromContext[D, T] with CenterFromContext[D, T] {
    override def min(domain: D) = toRangeDomain(domain).min
    override def max(domain: D) = toRangeDomain(domain).max
    override def center(domain: D) = RangeDomain.rangeCenter(toRangeDomain(domain))
  }

  implicit def rangeWithDefaultStepIsDiscrete[D, T](implicit toRangeDomain: ToRangeDomain[D, T], step: DefaultStep[T]) = new DiscreteFromContext[D, T] {
    override def iterator(domain: D) = StepRangeDomain[T](toRangeDomain(domain), step.step).iterator
  }

  implicit def toDomain[D, T](d: D)(implicit toRangeDomain: ToRangeDomain[D, T]) = toRangeDomain(d)

  def apply[T: RangeValue](
    min: FromContext[T],
    max: FromContext[T]
  ): RangeDomain[T] = new RangeDomain[T](min, max)

  def apply[T: RangeValue](
    min:  FromContext[T],
    max:  FromContext[T],
    step: FromContext[T]
  ): StepRangeDomain[T] =
    StepRangeDomain[T](RangeDomain[T](min, max), step)

  def size[T: RangeValue](
    min:  FromContext[T],
    max:  FromContext[T],
    size: FromContext[Int]
  ): SizeRangeDomain[T] =
    SizeRangeDomain[T](RangeDomain[T](min, max), size)

  def rangeCenter[T](r: RangeDomain[T]): FromContext[T] = (r.min, r.max) mapN { (min, max) ⇒
    import r.ops._
    min + ((max - min) / fromInt(2))
  }

}

class RangeDomain[T](val min: FromContext[T], val max: FromContext[T])(implicit val ops: RangeValue[T])
