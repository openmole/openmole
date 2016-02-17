/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.puzzle

import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.mole._
import org.openmole.core.workspace.Workspace
import org.openmole.core.workflow.execution._
import scalaz._
import Scalaz._

object ToPuzzle {

  implicit val puzzleToPuzzle = new ToPuzzle[Puzzle] {
    def toPuzzle(p: Puzzle) = p
  }

  implicit val puzzlePieceToPuzzle = new ToPuzzle[PuzzlePiece] {
    def toPuzzle(p: PuzzlePiece) = p.buildPuzzle
  }

  implicit val puzzleContainerToPuzzle = new ToPuzzle[PuzzleContainer] {
    def toPuzzle(p: PuzzleContainer) = p.buildPuzzle
  }

  implicit val slotToPuzzle = new ToPuzzle[Slot] {
    def toPuzzle(p: Slot) = p.toPuzzle
  }

  implicit val capsuleToPuzzle = new ToPuzzle[Capsule] {
    def toPuzzle(p: Capsule) = p.toPuzzle
  }

  implicit val taskToPuzzle = new ToPuzzle[Task] {
    def toPuzzle(p: Task) = Capsule(p).toPuzzle
  }

  implicit def bothToPuzzleIsToPuzzle[P: ToPuzzle] = new ToPuzzle[\&/[P, _]] {
    def toPuzzle(p: \&/[P, _]) = implicitly[ToPuzzle[P]].toPuzzle(p.a.get)
  }

}

trait ToPuzzle[-T] {
  def toPuzzle(t: T): Puzzle
}

object Puzzle {

  implicit def toPuzzle[P: ToPuzzle](p: P): Puzzle = implicitly[ToPuzzle[P]].toPuzzle(p)

  def merge[P1: ToPuzzle, P2: ToPuzzle](p1: P1, p2: P2) =
    new Puzzle(
      p1.firstSlot,
      p1.lasts,
      p1.transitions.toList ::: p2.transitions.toList,
      p1.dataChannels.toList ::: p2.dataChannels.toList,
      p1.sources.toList ::: p2.sources.toList,
      p1.hooks.toList ::: p2.hooks.toList,
      p1.environments ++ p2.environments,
      p1.grouping ++ p2.grouping)

  def merge(
    first: Slot,
    lasts: Iterable[Capsule],
    puzzles: Iterable[Puzzle],
    transitions: Iterable[ITransition] = Iterable.empty,
    dataChannels: Iterable[DataChannel] = Iterable.empty) =
    new Puzzle(
      first,
      lasts,
      transitions.toList ::: puzzles.flatMap { _.transitions }.toList,
      dataChannels.toList ::: puzzles.flatMap { _.dataChannels }.toList,
      puzzles.flatMap(_.sources),
      puzzles.flatMap(_.hooks),
      puzzles.flatMap { _.environments }.toMap,
      puzzles.flatMap { _.grouping }.toMap)

}

object PuzzlePiece {
  implicit def slotToPuzzlePieceConverter(slot: Slot) = slot.toPuzzlePiece
  implicit def capsuleToPuzzlePieceConverter(capsule: Capsule) = capsule.toPuzzlePiece
  implicit def taskToPuzzlePieceConverter(task: Task) = Capsule(task).toPuzzlePiece
}

case class PuzzlePiece(
    slot: Slot,
    sources: Iterable[Source] = Iterable.empty,
    hooks: Iterable[Hook] = Iterable.empty,
    environment: Option[Environment] = None,
    grouping: Option[Grouping] = None) {
  def capsule = slot.capsule

  def buildPuzzle: Puzzle =
    Puzzle(
      slot,
      Seq(capsule),
      Iterable.empty,
      Iterable.empty,
      sources.map(capsule -> _),
      hooks.map(capsule -> _),
      Map() ++ environment.map(capsule -> _),
      Map() ++ grouping.map(capsule -> _)
    )

}

case class Puzzle(
    firstSlot: Slot,
    lasts: Iterable[Capsule] = Iterable.empty,
    transitions: Iterable[ITransition] = Iterable.empty,
    dataChannels: Iterable[DataChannel] = Iterable.empty,
    sources: Iterable[(Capsule, Source)] = Iterable.empty,
    hooks: Iterable[(Capsule, Hook)] = Iterable.empty,
    environments: Map[Capsule, Environment] = Map.empty,
    grouping: Map[Capsule, Grouping] = Map.empty) {

  def this(p: Puzzle) =
    this(
      p.firstSlot,
      p.lasts,
      p.transitions,
      p.dataChannels,
      p.sources,
      p.hooks,
      p.environments,
      p.grouping)

  def toMole = new Mole(firstSlot.capsule, transitions, dataChannels)

  def toExecution: MoleExecution =
    MoleExecution(toMole, sources, hooks, environments, grouping)

  def toExecution(
    implicits: Context = Context.empty,
    seed: Long = Workspace.newSeed,
    executionContext: ExecutionContext = ExecutionContext.local,
    defaultEnvironment: LocalEnvironment = LocalEnvironment()): MoleExecution =
    MoleExecution(
      toMole,
      sources,
      hooks,
      environments,
      grouping,
      implicits,
      seed,
      defaultEnvironment)(executionContext)

  def slots: Set[Slot] = (firstSlot :: transitions.map(_.end).toList).toSet

  def first = firstSlot.capsule

  def buildPuzzle = this

}