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

import org.openmole.core.tools.io.FromString
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.tools.FromContext

import scala.util.Random

object LogRange {

  implicit def isFinite[T] =
    new Finite[LogRange[T], T] with Center[LogRange[T], T] with Bounds[LogRange[T], T] {
      override def computeValues(domain: LogRange[T]) = FromContext.apply((context, rng) ⇒ domain.computeValues(context)(rng))
      override def center(domain: LogRange[T]) = FromContext.apply((context, rng) ⇒ domain.center(context)(rng))
      override def max(domain: LogRange[T]) = FromContext.apply((context, rng) ⇒ domain.max(context)(rng))
      override def min(domain: LogRange[T]) = FromContext.apply((context, rng) ⇒ domain.min(context)(rng))
    }

  def apply[T: Log](range: Range[T], steps: FromContext[Int]) =
    new LogRange[T](range, steps)

  def apply[T: Fractional: Log](
    min:   FromContext[T],
    max:   FromContext[T],
    steps: FromContext[Int]
  ): LogRange[T] =
    LogRange[T](Range[T](min, max), steps)

}

sealed class LogRange[T](val range: Range[T], val steps: FromContext[Int])(implicit lg: Log[T]) extends Bounded[T] {

  import range._

  def computeValues(context: Context)(implicit rng: RandomProvider): Iterable[T] = {
    val logMin: T = lg.log(min(context))
    val logMax: T = lg.log(max(context))
    val nbSteps = steps.from(context)

    import fractional._

    val logStep = (logMax - logMin) / (fromInt(nbSteps - 1))
    Iterator.iterate(logMin)(_ + logStep).map(lg.exp).take(nbSteps).toVector
  }

  def max = range.max
  def min = range.min

}
