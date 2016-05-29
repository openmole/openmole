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

import org.openmole.core.tools.io.FromString
import org.openmole.core.workflow.data.{ Context, RandomProvider }
import org.openmole.core.workflow.domain.{ Bounds, Center, Finite }
import org.openmole.core.workflow.tools.FromContext

object Range {

  implicit def isBounded[T] = new Bounds[Range[T], T] with Center[Range[T], T] {
    override def min(domain: Range[T]) = FromContext((context, rng) ⇒ domain.min.from(context)(rng))
    override def max(domain: Range[T]) = FromContext((context, rng) ⇒ domain.max.from(context)(rng))
    override def center(domain: Range[T]) = Range.rangeCenter(domain)
  }

  implicit def rangeWithDefaultStepIsFinite[T](implicit step: DefaultStep[T]) = new Finite[Range[T], T] {
    override def computeValues(domain: Range[T]) =
      FromContext((context, rng) ⇒ StepRange[T](domain, step.step).computeValues(context)(rng))
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

  def rangeCenter[T](r: Range[T]): FromContext[T] = FromContext { (context, rng) ⇒
    import r.ops._
    val mi = r.min.from(context)(rng)
    mi + ((r.max.from(context)(rng) - mi) / fromInt(2))
  }

}

class Range[T](val min: FromContext[T], val max: FromContext[T])(implicit val ops: RangeValue[T])
