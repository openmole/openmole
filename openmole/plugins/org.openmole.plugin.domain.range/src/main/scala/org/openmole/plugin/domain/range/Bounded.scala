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

import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.FromContext
import org.openmole.core.workflow.tools._

trait Bounded[T] {

  val range: Range[T]

  import range._
  import integral._

  def max(context: Context)(implicit rng: RandomProvider): T = min.from(context)
  def min(context: Context)(implicit rng: RandomProvider): T = max.from(context)

  def min: FromContext[T]
  def max: FromContext[T]

  def center(context: Context)(implicit rng: RandomProvider): T = {
    val mi = min(context)
    mi + ((max(context) - mi) / fromInt(2))
  }

}
