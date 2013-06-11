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
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Random

object Puzzle {

  def merge(p1: Puzzle, p2: Puzzle) =
    new Puzzle(
      p1.first,
      p1.lasts,
      p1.transitions.toList ::: p2.transitions.toList,
      p1.dataChannels.toList ::: p2.dataChannels.toList,
      p1.sources.toList ::: p2.sources.toList,
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
      transitions.toList ::: puzzles.flatMap { _.transitions }.toList,
      dataChannels.toList ::: puzzles.flatMap { _.dataChannels }.toList,
      puzzles.flatMap(_.sources),
      puzzles.flatMap(_.hooks),
      puzzles.flatMap { _.selection }.toMap,
      puzzles.flatMap { _.grouping }.toMap)

}

case class Puzzle(
    first: Slot,
    lasts: Iterable[ICapsule],
    transitions: Iterable[ITransition],
    dataChannels: Iterable[IDataChannel],
    sources: Iterable[(ICapsule, ISource)],
    hooks: Iterable[(ICapsule, IHook)],
    selection: Map[ICapsule, EnvironmentSelection],
    grouping: Map[ICapsule, Grouping]) {

  def this(p: Puzzle) =
    this(
      p.first,
      p.lasts,
      p.transitions,
      p.dataChannels,
      p.sources,
      p.hooks,
      p.selection,
      p.grouping)

  def toMole = new Mole(first.capsule, transitions, dataChannels)

  def toPartialExecution = PartialMoleExecution(toMole, sources, hooks, selection, grouping)

  def toPartialExecution(
    sources: Iterable[(ICapsule, ISource)] = Iterable.empty,
    hooks: Iterable[(ICapsule, IHook)] = Iterable.empty,
    selection: Map[ICapsule, EnvironmentSelection] = Map.empty,
    grouping: Map[ICapsule, Grouping] = Map.empty,
    profiler: Profiler = Profiler.empty,
    seed: Long = Workspace.newSeed) =
    PartialMoleExecution(toMole, this.sources ++ sources, this.hooks ++ hooks, this.selection ++ selection, this.grouping ++ grouping, profiler, seed)

  def toExecution: MoleExecution =
    MoleExecution(toMole, sources, hooks, selection, grouping)

  def toExecution(
    sources: Iterable[(ICapsule, ISource)] = Iterable.empty,
    hooks: Iterable[(ICapsule, IHook)] = Iterable.empty,
    selection: Map[ICapsule, EnvironmentSelection] = Map.empty,
    grouping: Map[ICapsule, Grouping] = Map.empty,
    profiler: Profiler = Profiler.empty,
    implicits: Context = Context.empty,
    seed: Long = Workspace.newSeed): MoleExecution =
    MoleExecution(toMole, this.sources ++ sources, this.hooks ++ hooks, this.selection ++ selection, this.grouping ++ grouping, profiler, implicits, seed)

  def +(p: Puzzle) = Puzzle.merge(this, p)

  def slots: Set[Slot] = (first :: transitions.map(_.end).toList).toSet

  override def toString = first.capsule.task.name
}