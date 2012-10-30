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

package org.openmole.plugin.grouping.batch

import org.openmole.core.implementation.mole._
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.mole._

/**
 * Group mole jobs given a fixed number of batch.
 *
 * @param numberOfBatch total number of batch
 */
class BatchGrouping(numberOfBatch: Int) extends Grouping {

  override def apply(context: Context, groups: Iterable[(IMoleJobGroup, Iterable[IMoleJob])]): IMoleJobGroup = {
    if (groups.size < numberOfBatch) MoleJobGroup()
    else groups.minBy { case (_, g) ⇒ g.size }._1
  }

}
