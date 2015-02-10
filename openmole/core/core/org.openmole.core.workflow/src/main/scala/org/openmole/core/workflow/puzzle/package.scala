/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow

import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.mole._

package puzzle {

  trait PuzzlePackage {

    implicit def puzzleDecoration(puzzle: Puzzle) = new {
      def last = puzzle.lasts.head
    }

    implicit def pieceOfPuzzleToPuzzleDecorator(piece: PuzzlePiece) = piece.toPuzzle

    implicit def capsuleToPuzzlePieceDecorator(capsule: Capsule) = new {
      def toPuzzlePiece = PuzzlePiece(Slot(capsule))
    }

    implicit def slotToPuzzlePieceDecorator(slot: Slot) = new {
      def toPuzzlePiece = PuzzlePiece(slot)
    }

    implicit def capsulePuzzleDecorator(capsule: Capsule) = new {
      def toPuzzle: Puzzle = Puzzle(Slot(capsule), List(capsule))
    }

    implicit def slotDecorator(slot: Slot) = new {
      def toPuzzle = Puzzle(slot, List(slot.capsule))
    }
  }

}

package object puzzle extends PuzzlePackage