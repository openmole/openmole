/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.domain.range

import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._

import cats._
import cats.implicits._
import cats.syntax._

object LogRange {

  implicit def isDiscrete[T] =
    new DiscreteFromContext[LogRange[T], T] with CenterFromContext[LogRange[T], T] with BoundsFromContext[LogRange[T], T] {
      override def iterator(domain: LogRange[T]) = domain.iterator
      override def center(domain: LogRange[T]) = Range.rangeCenter(domain.range)
      override def max(domain: LogRange[T]) = domain.max
      override def min(domain: LogRange[T]) = domain.min
    }

  def apply[T: Log](range: Range[T], steps: FromContext[Int]) =
    new LogRange[T](range, steps)

  def apply[T: RangeValue: Log](
    min:   FromContext[T],
    max:   FromContext[T],
    steps: FromContext[Int]
  ): LogRange[T] =
    LogRange[T](Range[T](min, max), steps)

}

sealed class LogRange[T](val range: Range[T], val steps: FromContext[Int])(implicit lg: Log[T]) {

  import range._

  def iterator = (min, max, steps) mapN { (min, max, steps) ⇒
    val logMin: T = lg.log(min)
    val logMax: T = lg.log(max)

    import ops._

    val logStep = (logMax - logMin) / (fromInt(steps - 1))
    Iterator.iterate(logMin)(_ + logStep).map(lg.exp).take(steps)
  }

  def max = range.max
  def min = range.min

}
