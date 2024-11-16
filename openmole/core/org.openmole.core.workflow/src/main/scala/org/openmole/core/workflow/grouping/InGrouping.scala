/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.grouping

import org.openmole.core.context.Context
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole._
import org.openmole.tool.random.RandomProvider

object InGrouping {
  def apply(numberOfBatch: Int) = new InGrouping(numberOfBatch)
}

/**
 * Group mole jobs given a fixed number of batch.
 *
 * @param numberOfBatch total number of batch
 */
class InGrouping(numberOfBatch: Int) extends Grouping {

  override def apply(context: Context, groups: Iterable[(MoleJobGroup, Iterable[Job])])(implicit newGroup: NewGroup, randomProvider: RandomProvider): MoleJobGroup = {
    if (groups.size < numberOfBatch) newGroup()
    else groups.minBy { case (_, g) => g.size }._1
  }

}
