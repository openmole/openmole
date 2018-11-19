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

package org.openmole.plugin.grouping.onvariable

import org.openmole.core.context._
import org.openmole.core.workflow.job._
import org.openmole.core.workflow.mole._
import org.openmole.tool.random.RandomProvider

object OnVariableGrouping {

  def apply(prototypes: Val[_]*) = new OnVariableGrouping(None, prototypes: _*)
  def apply(numberOfMoleJobs: Int, prototypes: Val[_]*) = new OnVariableGrouping(Some(numberOfMoleJobs), prototypes: _*)
}

class OnVariableGrouping(numberOfMoleJobs: Option[Int], prototypes: Val[_]*) extends Grouping {

  def apply(context: Context, groups: Iterable[(MoleJobGroup, Iterable[MoleJob])])(implicit newGroup: NewGroup, randomProvider: RandomProvider): MoleJobGroup =
    new MoleJobGroup(prototypes.flatMap { context.option(_) }.toSeq: _*)

  override def complete(jobs: Iterable[MoleJob]) =
    numberOfMoleJobs map { jobs.size >= _ } getOrElse (false)
}