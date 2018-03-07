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

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._

package puzzle {

  trait PuzzlePackage {

    implicit def toPuzzlePiece[P: ToPuzzlePiece](p: P): PuzzlePiece = implicitly[ToPuzzlePiece[P]].apply(p)

    // implicit def toPuzzlePieceDecorator[P: ToPuzzlePiece](p: P): PuzzlePiece = implicitly[ToPuzzlePiece[P]].apply(p)
    implicit def taskToPuzzlePieceDecorator(p: Task): PuzzlePiece = implicitly[ToPuzzlePiece[Task]].apply(p)
    implicit def capsuleToPuzzlePieceDecorator(p: Capsule): PuzzlePiece = implicitly[ToPuzzlePiece[Capsule]].apply(p)
    implicit def slotToPuzzlePieceDecorator(p: Slot): PuzzlePiece = implicitly[ToPuzzlePiece[Slot]].apply(p)

    class PuzzleDecorator[P: ToPuzzle](val puzzle: P) extends TransitionDecorator {
      val from = puzzle
      val last = puzzle.lasts.head

      def &[P2: ToPuzzle](p2: P2): Puzzle = Puzzle.merge[P, P2](puzzle, p2)

      def hook(hooks: Hook*): Puzzle = {
        val p: Puzzle = puzzle
        def hooked =
          for {
            c ← p.lasts
            h ← hooks
          } yield c -> h

        Puzzle.hooks.modify(_ ++ hooked)(p)
      }

      def source(sources: Source*): Puzzle = {
        val p: Puzzle = puzzle
        def sourced =
          for {
            c ← p.lasts
            s ← sources
          } yield c -> s

        Puzzle.sources.modify(_ ++ sourced)(p)
      }
    }

    implicit def toPuzzle[P: ToPuzzle](p: P): Puzzle = implicitly[ToPuzzle[P]].toPuzzle(p)
    implicit def toPuzzleDecorator[P: ToPuzzle](p: P): PuzzleDecorator[P] = new PuzzleDecorator(p)
    implicit def toPuzzleToMoleExecution[T: ToPuzzle](t: T)(implicit moleServices: MoleServices) = implicitly[ToPuzzle[T]].toPuzzle(t).toExecution

  }

}

package object puzzle