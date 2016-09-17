/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.method.abc

import org.openmole.core.workflow.tools._
import org.openmole.plugin.hook.file._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.dsl._

object SaveABCHook {

  def apply(puzzle: ABCPuzzle, dir: FromContext[File]) = {
    val fileName = dir / ExpandedString("abc${" + puzzle.iteration.name + "}.csv")
    val prototypes = Seq(puzzle.iteration) ++ puzzle.algorithm.priorPrototypes.map(_.toArray) ++ puzzle.algorithm.targetPrototypes.map(_.toArray)
    AppendToCSVFileHook(fileName, prototypes: _*)
  }

}
