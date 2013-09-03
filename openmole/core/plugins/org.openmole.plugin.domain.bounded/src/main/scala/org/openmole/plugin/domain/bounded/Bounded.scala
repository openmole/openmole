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

package org.openmole.plugin.domain.bounded

import org.openmole.core.model.domain._
import org.openmole.core.model.data._
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.io._
import org.openmole.misc.tools.script._

object Bounded {

  def apply[T](min: String, max: String)(implicit fromString: FromString[T]) =
    new Bounded[T](min, max)

}

sealed class Bounded[T](val min: String, val max: String)(implicit fromString: FromString[T]) extends Domain[T] with Bounds[T] {
  @transient lazy val minValue = GroovyProxyPool(min)
  @transient lazy val maxValue = GroovyProxyPool(max)

  def min(context: Context) = fromString.fromString(minValue(context).toString)
  def max(context: Context) = fromString.fromString(maxValue(context).toString)
}
