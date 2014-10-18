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

import org.openmole.plugin.hook.file._
import org.openmole.core.implementation.data._

object SaveABCHook {

  def apply(puzzle: ABCPuzzle, dir: String): AppendToCSVFileHook.Builder = apply(puzzle, dir, "/abc{" + puzzle.iteration.name + "}.csv")

  def apply(puzzle: ABCPuzzle, dir: String, name: String): AppendToCSVFileHook.Builder = {
    val builder = new AppendToCSVFileHook.Builder(dir + "/" + name)
    builder.add(puzzle.iteration)
    puzzle.algorithm.priorPrototypes.foreach(p ⇒ builder.add(p.toArray))
    puzzle.algorithm.targetPrototypes.foreach { o ⇒ builder.add(o.toArray) }
    builder
  }
}
