/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.groupingstrategy.batch

import org.openmole.core.implementation.mole.MoleJobGroup
import org.openmole.core.model.data.IContext
import org.openmole.core.model.mole.IGroupingStrategy
import org.openmole.core.model.mole.IMoleJobGroup
import scala.util.Random

/**
 * Group the mole jobs by distributing them at random among {{{numberOfBatch}}}
 * groups. A seed could be provided for the random number generator. If no seed
 * is provided the empty contructor of scala.util.Random is called.
 * 
 * @param numberOfBatch total number of groups
 * @param seed the seed for the pseudo-random numbers generator
 */
class NumberOfRandomizedBatchGroupingStrategy(numberOfBatch: Int, seed: Option[Int]) extends IGroupingStrategy {
  
  /**
   *  Constructor providing None for the seed option.
   *  
   *  @param numberOfBatch total number of groups
   */
  def this(numberOfBatch: Int) = this(numberOfBatch, None)
  
  /**
   *  Constructor providing Some(seed) for the seed option.
   *  
   * @param numberOfBatch total number of groups
   * @param seed the seed for the pseudo-random numbers generator
   */
  def this(numberOfBatch: Int, seed: Int) = this(numberOfBatch, Some(seed))
  
  private val random = seed match {
    case None => new Random
    case Some(seed) => new Random(seed)
  }

  override def group(context: IContext) = (new MoleJobGroup(Array[Any](random.nextInt(numberOfBatch))), false)

}
