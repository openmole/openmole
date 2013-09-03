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
    step: String = "1")(implicit integral: Integral[T], fs: FromString[T]) = new Range[T](min, max, step)

}

sealed class Range[T](val min: String, val max: String, val step: String = "1")(implicit integral: Integral[T], fs: FromString[T]) extends Domain[T] with Finite[T] with Center[T] with Bounds[T] {

  import integral._
  import fs._

  @transient lazy val minValue = GroovyProxyPool(min)
  @transient lazy val maxValue = GroovyProxyPool(max)
  @transient lazy val stepValue = GroovyProxyPool(step)

  override def computeValues(context: Context): Iterable[T] = {
    val mi = min(context)
    val ma = max(context)
    val s = step(context)

    val size = (ma - mi).abs / s

    (for (i ← 0 to size.toInt) yield { mi + fromInt(i) * s })
  }

  override def center(context: Context): T = {
    val mi = min(context);
    mi + ((max(context) - mi) / fromInt(2))
  }

  def step(context: Context): T = fromString(stepValue(context).toString)
  override def max(context: Context): T = fromString(maxValue(context).toString)
  override def min(context: Context): T = fromString(minValue(context).toString)

}

