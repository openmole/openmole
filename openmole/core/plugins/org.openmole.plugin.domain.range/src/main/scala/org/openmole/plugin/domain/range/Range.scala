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

import org.openmole.core.implementation.tools.FromContext
import org.openmole.misc.tools.io.FromString

object Range {

  def apply[T](
    min: FromContext[T],
    max: FromContext[T])(implicit integral: Integral[T]) = new Range[T](min, max)

  def apply[T](
    min: FromContext[T],
    max: FromContext[T],
    step: FromContext[T])(implicit integral: Integral[T]): StepRange[T] =
    StepRange[T](Range[T](min, max), step)

  def steps[T](
    min: FromContext[T],
    max: FromContext[T],
    steps: FromContext[T])(implicit integral: Integral[T], fs: FromString[T]): SizeRange[T] =
    SizeRange[T](Range[T](min, max), steps)

}

class Range[T](val min: FromContext[T], val max: FromContext[T])(implicit val integral: Integral[T]) extends Bounded[T] {
  lazy val range = this
}

