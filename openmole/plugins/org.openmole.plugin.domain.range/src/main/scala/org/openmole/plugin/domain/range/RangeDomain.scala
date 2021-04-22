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
import org.openmole.core.workflow.domain.{ BoundedFromContextDomain, CenterFromContextDomain, DiscreteFromContextDomain }
import cats.implicits._

object RangeDomain {

  implicit def isBounded[T] = new BoundedFromContextDomain[RangeDomain[T], T] with CenterFromContextDomain[RangeDomain[T], T] {
    override def min(domain: RangeDomain[T]) = domain.min
    override def max(domain: RangeDomain[T]) = domain.max
    override def center(domain: RangeDomain[T]) = RangeDomain.rangeCenter(domain)
  }

  implicit def rangeWithDefaultStepIsDiscrete[D, T](implicit toRangeDomain: IsRangeDomain[D, T], step: DefaultStep[T]) = new DiscreteFromContextDomain[D, T] {
    override def iterator(domain: D) = StepRangeDomain[T](toRangeDomain(domain), step.step).iterator
  }

  implicit def toDomain[D, T](d: D)(implicit toRangeDomain: IsRangeDomain[D, T]) = toRangeDomain(d)

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
