/*
 * Copyright (C) 19/12/12 Romain Reuillon
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

package org.openmole.plugin.domain.modifier

import org.openmole.core.model.domain._
import org.openmole.core.model.data._

object SlidingDomain {

  def apply[T](domain: Domain[T] with Discrete[T], size: Int, step: Int = 1)(implicit m: Manifest[T]) =
    new SlidingDomain[T](domain, size, step)

}

class SlidingDomain[T](val domain: Domain[T] with Discrete[T], val size: Int, val step: Int = 1)(implicit m: Manifest[T]) extends Domain[Array[T]] with Discrete[Array[T]] {

  override def inputs = domain.inputs

  override def iterator(context: Context): Iterator[Array[T]] =
    domain.iterator(context).sliding(size, step).map(_.toArray)

}
