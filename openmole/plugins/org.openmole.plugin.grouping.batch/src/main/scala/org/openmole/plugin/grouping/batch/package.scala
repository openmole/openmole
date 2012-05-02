/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.grouping

import org.openmole.core.implementation.puzzle.Puzzle

package object batch {

  implicit def puzzleBatchGroupingDecorator(puzzle: Puzzle) = new {

    def by(n: Int): Puzzle =
      puzzle.copy(
        grouping = puzzle.grouping + (puzzle.last -> new NumberOfMoleJobsGrouping(n)))

    def in(n: Int): Puzzle =
      puzzle.copy(
        grouping = puzzle.grouping + (puzzle.last -> new NumberOfBatchGrouping(n)))

    def inShuffled(n: Int): Puzzle =
      puzzle.copy(
        grouping = puzzle.grouping + (puzzle.last -> new NumberOfBatchShuffledGrouping(n)))

  }

  implicit def intToNumberOfMoleJobGrouping(n: Int) = new NumberOfMoleJobsGrouping(n)

}