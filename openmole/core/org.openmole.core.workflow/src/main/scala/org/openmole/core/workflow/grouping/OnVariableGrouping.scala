/*
 *  Copyright (C) 2010 Romain Reuillon
 *  Copyright (C) 2015 Jonathan Passerat-Palmbach
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.grouping

import org.openmole.core.context._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole._
import org.openmole.tool.random.RandomProvider

object OnVariableGrouping {

  def apply(prototypes: Val[?]*) = new OnVariableGrouping(None, prototypes *)
  def apply(numberOfMoleJobs: Int, prototypes: Val[?]*) = new OnVariableGrouping(Some(numberOfMoleJobs), prototypes *)
}

class OnVariableGrouping(numberOfMoleJobs: Option[Int], prototypes: Val[?]*) extends Grouping {

  def apply(context: Context, groups: Iterable[(MoleJobGroup, Iterable[Job])])(implicit newGroup: NewGroup, randomProvider: RandomProvider): MoleJobGroup =
    new MoleJobGroup(prototypes.flatMap { context.option(_) }.toSeq *)

  override def complete(jobs: Iterable[Job]) =
    numberOfMoleJobs map { jobs.size >= _ } getOrElse (false)
}