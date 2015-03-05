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

import org.openmole.core.tools.io.FromString
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.tools.FromContext

object StepRange {
  def apply[T](range: Range[T], step: FromContext[T]) = new StepRange[T](range, step)
}

class StepRange[T](val range: Range[T], steps: FromContext[T]) extends SizeStep[T] with Bounded[T] {
  import range._

  def stepAndSize(minValue: T, maxValue: T, context: Context) = {
    import integral._
    val step = steps.from(context)
    val size = (maxValue - minValue).abs / step
    (step, size.toInt)
  }

  def min = range.min
  def max = range.max
}
