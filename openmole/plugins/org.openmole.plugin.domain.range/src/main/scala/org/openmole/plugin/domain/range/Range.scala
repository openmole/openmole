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
import org.openmole.core.workflow.data.{ RandomProvider, Context }
import org.openmole.core.workflow.domain.{ Center, Bounds }
import org.openmole.core.workflow.tools.FromContext

object Range {

  implicit def isBounded[T] = new Bounds[T, Range[T]] with Center[T, Range[T]] {
    override def min(domain: Range[T], context: Context)(implicit rng: RandomProvider): T = domain.min(context)
    override def max(domain: Range[T], context: Context)(implicit rng: RandomProvider): T = domain.max(context)
    override def center(domain: Range[T], context: Context)(implicit rng: RandomProvider): T = domain.center(context)
  }

  def apply[T](
    min: FromContext[T],
    max: FromContext[T])(implicit integral: Integral[T]) = new Range[T](min, max)

  def apply[T](
    min: FromContext[T],
    max: FromContext[T],
    step: FromContext[T])(implicit integral: Integral[T]): StepRange[T] =
    StepRange[T](Range[T](min, max), step)

  def size[T](
    min: FromContext[T],
    max: FromContext[T],
    size: FromContext[Int])(implicit integral: Integral[T]): SizeRange[T] =
    SizeRange[T](Range[T](min, max), size)

}

class Range[T](val min: FromContext[T], val max: FromContext[T])(implicit val integral: Integral[T]) extends Bounded[T] {
  lazy val range = this
}

