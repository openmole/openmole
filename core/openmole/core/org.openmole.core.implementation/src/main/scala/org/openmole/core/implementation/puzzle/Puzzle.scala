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

package org.openmole.core.implementation.puzzle

import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.transition._
import org.openmole.core.implementation.mole._

object Puzzle {

  def merge(p1: Puzzle, p2: Puzzle) =
    new Puzzle(
      p1.first,
      p1.lasts,
      p1.transitions.toList ::: p2.transitions.toList,
      p1.dataChannels.toList ::: p2.dataChannels.toList,
      p1.hooks.toList ::: p2.hooks.toList,
      p1.selection ++ p2.selection,
      p1.grouping ++ p2.grouping)

  def merge(
    first: Slot,
    lasts: Iterable[ICapsule],
    puzzles: Iterable[Puzzle],
    transitions: Iterable[ITransition] = Iterable.empty,
    dataChannels: Iterable[IDataChannel] = Iterable.empty) =
    new Puzzle(
      first,
      lasts,
      puzzles.flatMap { _.transitions }.toList ::: transitions.toList,
      puzzles.flatMap { _.dataChannels }.toList ::: dataChannels.toList,
      puzzles.flatMap(_.hooks),
      puzzles.flatMap { _.selection }.toMap,
      puzzles.flatMap { _.grouping }.toMap)

}

case class Puzzle(
    val first: Slot,
    val lasts: Iterable[ICapsule],
    val transitions: Iterable[ITransition],
    val dataChannels: Iterable[IDataChannel],
    val hooks: Iterable[(ICapsule, Hook)],
    val selection: Map[ICapsule, EnvironmentSelection],
    val grouping: Map[ICapsule, Grouping]) {

  def this(p: Puzzle) =
    this(
      p.first,
      p.lasts,
      p.transitions,
      p.dataChannels,
      p.hooks,
      p.selection,
      p.grouping)

  def toMole = new Mole(first.capsule, transitions, dataChannels)
  def toExecution =
    new MoleExecution(toMole, hooks, selection, grouping)

  def toExecution(profiler: Profiler) =
    new MoleExecution(toMole, hooks, selection, grouping, profiler)

  def +(p: Puzzle) = Puzzle.merge(this, p)
}