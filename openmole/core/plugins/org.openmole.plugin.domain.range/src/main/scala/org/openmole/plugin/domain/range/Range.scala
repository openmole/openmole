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

import org.openmole.core.model.domain._
import org.openmole.misc.tools.io.FromString
import org.openmole.core.model.data._
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.script.GroovyProxyPool

object Range {

  def apply[T](
    min: String,
    max: String,
    step: String = "1")(implicit integral: Integral[T], fs: FromString[T]) = {
    val _step = step
    new Range[T](min, max) {
      lazy val stepProxy = GroovyProxyPool(_step)

      def stepAndSize(minValue: T, maxValue: T, context: Context) = {
        import integral._
        val step = fs.fromString(stepProxy(context).toString)
        val size = (maxValue - minValue).abs / step
        (step, size.toInt)
      }

    }
  }

  def steps[T](
    min: String,
    max: String,
    nbSteps: String)(implicit integral: Integral[T], fs: FromString[T]) = {
    new Range[T](min, max) {
      lazy val nbStepProxy = GroovyProxyPool(nbSteps)
      def stepAndSize(minValue: T, maxValue: T, context: Context) = {
        import integral._
        val size = fs.fromString(nbStepProxy(context).toString)
        val step = (maxValue - minValue) / size
        (step, size.toInt - 1)
      }
    }
  }

}

abstract sealed class Range[T](val min: String, val max: String)(implicit integral: Integral[T], fs: FromString[T]) extends Domain[T] with Finite[T] with Center[T] with Bounds[T] {

  import integral._
  import fs._

  def stepAndSize(maxValue: T, minValue: T, context: Context): (T, Int)

  @transient lazy val minValue = GroovyProxyPool(min)
  @transient lazy val maxValue = GroovyProxyPool(max)

  override def computeValues(context: Context): Iterable[T] = {
    val mi = min(context)
    val ma = max(context)
    val (step, size) = stepAndSize(mi, ma, context)
    for (i ← 0 to size) yield { mi + fromInt(i) * step }
  }

  override def center(context: Context): T = {
    val mi = min(context)
    mi + ((max(context) - mi) / fromInt(2))
  }

  override def max(context: Context): T = fromString(maxValue(context).toString)
  override def min(context: Context): T = fromString(minValue(context).toString)

}

