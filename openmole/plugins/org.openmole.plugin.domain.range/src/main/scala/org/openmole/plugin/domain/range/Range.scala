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

object Range {

  implicit def isBounded[T] = new BoundsFromContext[Range[T], T] with CenterFromContext[Range[T], T] {
    override def min(domain: Range[T]) = domain.min
    override def max(domain: Range[T]) = domain.max
    override def center(domain: Range[T]) = Range.rangeCenter(domain)
  }

  implicit def rangeWithDefaultStepIsDiscrete[T](implicit step: DefaultStep[T]) = new DiscreteFromContext[Range[T], T] {
    override def iterator(domain: Range[T]) = StepRange[T](domain, step.step).iterator
  }

  def apply[T: RangeValue](
    min: FromContext[T],
    max: FromContext[T]
  ): Range[T] = new Range[T](min, max)

  def apply[T: RangeValue](
    min:  FromContext[T],
    max:  FromContext[T],
    step: FromContext[T]
  ): StepRange[T] =
    StepRange[T](Range[T](min, max), step)

  def size[T: RangeValue](
    min:  FromContext[T],
    max:  FromContext[T],
    size: FromContext[Int]
  ): SizeRange[T] =
    SizeRange[T](Range[T](min, max), size)

  def rangeCenter[T](r: Range[T]): FromContext[T] = (r.min, r.max) mapN { (min, max) ⇒
    import r.ops._
    min + ((max - min) / fromInt(2))
  }

}

class Range[T](val min: FromContext[T], val max: FromContext[T])(implicit val ops: RangeValue[T])
