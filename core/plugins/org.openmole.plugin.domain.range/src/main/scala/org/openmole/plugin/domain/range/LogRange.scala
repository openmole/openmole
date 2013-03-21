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

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.io.FromString
import org.openmole.misc.tools.script.GroovyProxy

sealed class LogRange[T](val min: String, val max: String, val nbStep: String)(implicit integral: Integral[T], fs: FromString[T], lg: Log[T]) extends Domain[T] with Finite[T] with Bounds[T] {

  //def this(min: Double, max: Double, nbStep: Int) = this(min.toString, max.toString, nbStep.toString)

  override def computeValues(context: Context): Iterable[T] = {
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

  @transient lazy val minProxy = GroovyProxy(min)
  @transient lazy val maxProxy = GroovyProxy(max)
  @transient lazy val nbStepProxy = GroovyProxy(nbStep)

  def nbStep(context: Context): T = fs.fromString(nbStepProxy(context).toString)
  def min(context: Context): T = fs.fromString(minProxy(context).toString)
  def max(context: Context): T = fs.fromString(maxProxy(context).toString)

}
