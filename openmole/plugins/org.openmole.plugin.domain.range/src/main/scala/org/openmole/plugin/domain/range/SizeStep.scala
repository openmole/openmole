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

import org.openmole.core.expansion._

trait SizeStep[T] {

  val range: RangeDomain[T]

  import range.ops._

  def stepAndSize(maxValue: T, minValue: T): FromContext[(T, Int)]

  def iterator: FromContext[Iterator[T]] =
    FromContext { p ⇒
      import p._
      val mi: T = range.min.from(context)
      val ma: T = range.max.from(context)
      val (step, size) = stepAndSize(mi, ma).from(context)
      (0 to size).iterator.map { i ⇒ mi + (fromInt(i) * step) }
    } withValidate { inputs ⇒
      range.min.validate(inputs) ++ range.max.validate(inputs)
    }

}
