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

import org.openmole.misc.tools.script.GroovyProxyPool
import org.openmole.core.model.data._
import org.openmole.core.implementation.tools._

object SizeRange {

  def apply[T](range: Range[T], steps: String) = new SizeRange[T](range, steps)

}

class SizeRange[T](val range: Range[T], steps: String) extends SizeStep[T] with Bounded[T] {
  import range._

  lazy val stepsProxy = GroovyProxyPool(steps)
  def stepAndSize(minValue: T, maxValue: T, context: Context) = {
    import integral._
    val size = fs.fromString(stepsProxy(context).toString) - integral.one
    val step = (maxValue - minValue) / size
    (step, size.toInt)
  }
}
