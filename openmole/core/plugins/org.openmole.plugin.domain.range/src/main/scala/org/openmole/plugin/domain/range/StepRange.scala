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

import org.openmole.misc.tools.script._
import org.openmole.core.model.data._
import org.openmole.core.implementation.tools._

object StepRange {

  def apply[T](
    range: Range[T],
    step: String) = new StepRange(range, step)

}

class StepRange[T](val range: Range[T], step: String) extends SizeStep[T] with Bounded[T] {
  import range._

  lazy val stepProxy = GroovyProxyPool(step)

  def stepAndSize(minValue: T, maxValue: T, context: Context) = {
    import integral._
    val step = fs.fromString(stepProxy(context).toString)
    val size = (maxValue - minValue).abs / step
    (step, size.toInt)
  }

}
