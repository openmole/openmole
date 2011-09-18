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

import java.util.Random
import org.openmole.core.model.data.IContext
import org.openmole.core.model.domain.IFiniteDomain

class SlicedUniformIntDistribution(generator: Random, size: Int, topBound: Option[Int]= None) extends IFiniteDomain[Int]{
    
  def this(seed: Long, size: Int) = this(new Random(seed), size,None)
  def this(seed: Long, size: Int, b: Int) = this(new Random(seed), size,Some(b))
  
  lazy val domain = new UniformIntDistribution(generator, topBound)

  override def computeValues(context: IContext) = domain.iterator(context).slice(0, size).toIterable
}
