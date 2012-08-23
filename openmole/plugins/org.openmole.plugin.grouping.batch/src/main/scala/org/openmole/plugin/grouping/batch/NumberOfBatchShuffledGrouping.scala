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
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service._
import org.openmole.core.implementation.task.Task._

/**
 * Group the mole jobs by distributing them at random among {{{numberOfBatch}}}
 * groups.
 *
 * @param numberOfBatch total number of groups
 */
class NumberOfBatchShuffledGrouping(numberOfBatch: Int) extends IGrouping {

  override def apply(context: IContext, groups: Iterable[(IMoleJobGroup, Iterable[IMoleJob])]): IMoleJobGroup =
    new MoleJobGroup(newRNG(context.valueOrException(openMOLESeed)).nextInt(numberOfBatch))

}
