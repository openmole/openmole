/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.core.implementation.domain

import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IFiniteDomain

class SampledDomain[+T](val domain: IDomain[T], val size: Int) extends IFiniteDomain[T] {

  override def computeValues(global: IContext, context: IContext): Iterable[T] = {
    val it = domain.iterator(global, context)
    for(i <- 0 until math.min(size, it.size)) yield it.next
  }

}
