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

package org.openmole.core.model.sampling

import org.openmole.core.model.domain._
import org.openmole.core.model.data._

object Factor {

  def apply[T, D <: Domain[T]](prototype: Prototype[T], domain: D) = {
    val (_domain, _prototype) = (domain, prototype)
    new Factor[T, D] {
      val domain = _domain
      val prototype = _prototype
    }
  }

}

trait Factor[T, +D <: Domain[T]] {
  def domain: D
  def prototype: Prototype[T]
}
