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

object ByGrouping {

  def apply(numberOfMoleJobs: Int) = new ByGrouping(numberOfMoleJobs)

}

/**
 * Group mole jobs by group of numberOfMoleJobs.
 *
 * @param numberOfMoleJobs size of each batch
 */
class ByGrouping(numberOfMoleJobs: Int) extends Grouping {

  override def apply(context: Context, groups: Iterable[(MoleJobGroup, Iterable[Job])])(implicit newGroup: NewGroup, randomProvider: RandomProvider): MoleJobGroup = {
    groups.find { case (_, g) => g.size < numberOfMoleJobs } match {
      case Some((mg, _)) => mg
      case None          => newGroup()
    }
  }

  override def complete(jobs: Iterable[Job]) = jobs.size >= numberOfMoleJobs
}
