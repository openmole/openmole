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
import org.openmole.core.tools.script.GroovyProxy
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.tools.FromContext

import scala.util.Random

object LogRange {

  def apply[T](range: Range[T], steps: FromContext[T])(implicit lg: Log[T]) =
    new LogRange[T](range, steps)

  def apply[T](range: Range[T], steps: T)(implicit lg: Log[T]) =
    new LogRange[T](range, FromContext(steps))

  def apply[T](
    min: FromContext[T],
    max: FromContext[T],
    steps: FromContext[T])(implicit integral: Integral[T], log: Log[T]): LogRange[T] =
    LogRange[T](Range[T](min, max), steps)

}

sealed class LogRange[T](val range: Range[T], val steps: FromContext[T])(implicit lg: Log[T]) extends Domain[T] with Finite[T] with Bounds[T] {

  import range._

  override def computeValues(context: Context)(implicit rng: Random): Iterable[T] = {
    val mi: T = lg.log(min(context))
    val ma: T = lg.log(max(context))
    val nbst: T = nbStep(context)

    import integral._

    val st = abs(minus(ma, mi)) / nbst

    var cur = mi

    for (i ← 0 to nbst.toInt) yield {
      val ret = cur
      cur = plus(ret, st)
      lg.exp(ret)
    }
  }

  def nbStep(context: Context): T = steps.from(context)
  def min(context: Context): T = range.min.from(context)
  def max(context: Context): T = range.max.from(context)

}
