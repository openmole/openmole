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

  import org.openmole.core.event.EventDispatcher
  import org.openmole.core.preference.Preference
  import org.openmole.core.threadprovider.ThreadProvider
  import org.openmole.core.workflow.execution.{ Environment, EnvironmentProvider }
  import org.openmole.core.workspace.NewFile
  import org.openmole.tool.random.Seeder

  trait PuzzlePackage {
    implicit def toPuzzle[P: ToPuzzle](p: P): Puzzle = implicitly[ToPuzzle[P]].toPuzzle(p)

    class PuzzlePieceDecorator(puzzle: PuzzlePiece) extends HookDecorator[PuzzlePiece] with EnvironmentDecorator[PuzzlePiece] with SourceDecorator[PuzzlePiece] with TransitionDecorator {
      def from = puzzle.buildPuzzle

      def on(env: EnvironmentProvider) =
        puzzle.copy(environment = Some(env))

      def hook(hooks: Hook*) =
        puzzle.copy(hooks = puzzle.hooks.toList ::: hooks.toList)

      def source(sources: Source*) =
        puzzle.copy(sources = puzzle.sources.toList ::: sources.toList)

      def by(strategy: Grouping) =
        puzzle.copy(grouping = Some(strategy))
    }

    implicit def capsuleToPuzzlePieceDecorator(capsule: Capsule) = new {
      def toPuzzlePiece = PuzzlePiece(Slot(capsule))
    }

    implicit def slotToPuzzlePieceDecorator(slot: Slot) = new {
      def toPuzzlePiece = PuzzlePiece(slot)
    }

    implicit def puzzlePuzzlePieceDecoration(puzzle: PuzzlePiece) = new PuzzlePieceDecorator(puzzle)
    implicit def capsulePuzzlePieceDecoration(capsule: Capsule) = new PuzzlePieceDecorator(capsule.toPuzzlePiece)
    implicit def slotPuzzlePieceDecoration(slot: Slot) = new PuzzlePieceDecorator(slot.toPuzzlePiece)
    implicit def taskPuzzlePieceDecoration(task: Task): PuzzlePieceDecorator = new PuzzlePieceDecorator(Capsule(task).toPuzzlePiece)
    implicit def puzzlePieceMoleExecutionConverter(puzzle: PuzzlePiece)(implicit moleServices: MoleServices) = puzzle.buildPuzzle.toExecution
    implicit def puzzlePieceMoleConverter(puzzle: PuzzlePiece) = puzzle.buildPuzzle.toMole
    implicit def pieceOfPuzzleToPuzzleDecorator(piece: PuzzlePiece) = piece.buildPuzzle

    implicit def capsulePuzzleDecorator(capsule: Capsule) = new {
      def toPuzzle: Puzzle = Puzzle(Slot(capsule), List(capsule))
    }

    implicit def slotDecorator(slot: Slot) = new {
      def toPuzzle = Puzzle(slot, List(slot.capsule))
    }

    implicit def puzzleDecoratorConverter[P: ToPuzzle](p: P): PuzzleDecorator[P] = new PuzzleDecorator(p)

    class PuzzleDecorator[P: ToPuzzle](val puzzle: P) extends TransitionDecorator {
      def from = puzzle
      def last = puzzle.lasts.head

      def &[P2: ToPuzzle](p2: P2): Puzzle = Puzzle.merge[P, P2](puzzle, p2)

      def hook(hooks: Hook*): Puzzle = {
        def pieces = puzzle.lasts.map(_ hook (hooks: _*))
        pieces.foldLeft(puzzle: Puzzle)((puzzle, piece) ⇒ puzzle & piece)
      }

      def source(sources: Source*): Puzzle = {
        def piece: PuzzlePiece = puzzle.first.source(sources: _*)
        puzzle & piece
      }
    }

    implicit def puzzleContainerDecoration(pc: PuzzleContainer) = new PuzzleDecorator(pc.buildPuzzle)
    implicit def puzzleMoleExecutionConverter(puzzle: Puzzle)(implicit moleServices: MoleServices) = puzzle.toExecution
    implicit def puzzleMoleConverter(puzzle: Puzzle) = puzzle.toMole
    implicit def puzzleContainerMoleExecutionConverter(puzzle: PuzzleContainer)(implicit moleServices: MoleServices) = puzzle.buildPuzzle.toExecution
    implicit def puzzleContainerMoleConverter(puzzle: PuzzleContainer) = puzzle.buildPuzzle.toMole

    implicit def capsuleToMoleExecutionConverter(capsule: Capsule)(implicit moleServices: MoleServices): MoleExecution = capsule.toPuzzle.toExecution
    implicit def taskToMoleExecutionConverter(task: Task)(implicit moleServices: MoleServices): MoleExecution = Capsule(task).toPuzzle.toExecution
    implicit def moleToMoleExecutionConverter(mole: Mole)(implicit moleServices: MoleServices) = MoleExecution(mole)

  }

}

package object puzzle