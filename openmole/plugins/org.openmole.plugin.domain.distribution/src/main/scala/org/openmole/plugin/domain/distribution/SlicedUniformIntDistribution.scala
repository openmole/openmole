/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.domain.distribution

import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IFiniteDomain

class SlicedUniformIntDistribution(seed: Option[Long], size: Int, topBound: Option[Int]= None) extends IFiniteDomain[Int]{
    
  def this(seed: Long, size: Int) = this(Some(seed), size,None)
  def this(seed: Long, size: Int, b: Int) = this(Some(seed), size,Some(b))
  def this(size: Int) = this(None, size,None)
  def this(size: Int, b: Int) = this(None, size,Some(b))
 
  @transient lazy val domain = new UniformIntDistribution(seed, topBound)

  override def computeValues(context: IContext) = domain.iterator(context).take(size).toIterable
}
