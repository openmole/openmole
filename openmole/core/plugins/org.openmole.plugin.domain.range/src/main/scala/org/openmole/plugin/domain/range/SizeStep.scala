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

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.implementation.tools._

trait SizeStep[T] extends Domain[T] with Finite[T] with Center[T] with Bounds[T] {

  val range: Range[T]

  import range._
  import integral._

  def stepAndSize(maxValue: T, minValue: T, context: Context): (T, Int)

  override def computeValues(context: Context): Iterable[T] = {
    val mi = min(context)
    val ma = max(context)
    val (step, size) = stepAndSize(mi, ma, context)
    for (i ← 0 to size) yield { mi + fromInt(i) * step }
  }

}
