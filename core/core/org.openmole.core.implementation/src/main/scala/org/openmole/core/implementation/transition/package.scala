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

package org.openmole.core.implementation

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.tools._
import org.openmole.core.model.transition._

import puzzle._
import task._
import data._

package object transition {

  implicit def transitionsPuzzleDecorator(from: Puzzle) = new TransitionDecorator(from)
  implicit def transitionsCapsuleDecorator(from: ICapsule) = new TransitionDecorator(from.toPuzzle)
  implicit def transitionsTaskDecorator(from: ITask) = new TransitionDecorator(from.toCapsule.toPuzzle)
  implicit def transitionsTaskBuilderDecorator(from: TaskBuilder) = new TransitionDecorator(from.toTask.toCapsule.toPuzzle)
  implicit def transitionsSlotDecorator(slot: Slot) = new TransitionDecorator(slot.toPuzzle)

  implicit def taskToSlotConverter(task: ITask) = Slot(Capsule(task))
  implicit def transitionToSlotConverter(transition: ITransition) = transition.end
  implicit def conditionStringConverter(condition: String) = Condition(condition)

  class TransitionDecorator(from: Puzzle) {

    def -<(
      to: Puzzle,
      condition: ICondition = ICondition.True,
      filter: Filter[String] = Filter.empty,
      size: Option[String] = None) = {

      val transitions = from.lasts.map {
        c ⇒
          size match {
            case None ⇒ new ExplorationTransition(c, to.first, condition, filter)
            case Some(s) ⇒ new EmptyExplorationTransition(c, to.first, s, condition, filter)
          }
      }

      Puzzle.merge(from.first, to.lasts, from :: to :: Nil, transitions)
    }

    def -<(toHead: Puzzle, toTail: Puzzle*) = {
      val toPuzzles = (toHead :: toTail.toList)
      val transitions = for (f ← from.lasts; l ← toPuzzles) yield new ExplorationTransition(f, l.first)
      Puzzle.merge(from.first, toPuzzles.flatMap { _.lasts }, from :: toPuzzles ::: Nil, transitions)
    }

    def -<-(
      to: Puzzle,
      condition: ICondition = ICondition.True,
      filter: Filter[String] = Filter.empty) = {

      val transitions = from.lasts.map {
        c ⇒ new SlaveTransition(c, to.first, condition, filter)
      }

      Puzzle.merge(from.first, to.lasts, from :: to :: Nil, transitions)
    }

    def -<-(toHead: Puzzle, toTail: Puzzle*) = {
      val toPuzzles = (toHead :: toTail.toList)
      val transitions = for (f ← from.lasts; l ← toPuzzles) yield new SlaveTransition(f, l.first)
      Puzzle.merge(from.first, toPuzzles.flatMap { _.lasts }, from :: toPuzzles ::: Nil, transitions)
    }

    def >-(
      to: Puzzle,
      condition: ICondition = ICondition.True,
      filter: Filter[String] = Filter.empty,
      trigger: ICondition = ICondition.False) = {
      val transitions = from.lasts.map { c ⇒ new AggregationTransition(c, to.first, condition, filter, trigger) }
      Puzzle.merge(from.first, to.lasts, from :: to :: Nil, transitions)
    }

    //def >-(ch: ICapsule, ct: Puzzle*): Puzzle = >-(ch.toPuzzle, ct: _*)

    def >-(toHead: Puzzle, toTail: Puzzle*) = {
      val toPuzzles = (toHead :: toTail.toList)
      val transitions = for (f ← from.lasts; l ← toPuzzles) yield new AggregationTransition(f, l.first)
      Puzzle.merge(from.first, toPuzzles.flatMap { _.lasts }, from :: toPuzzles ::: Nil, transitions)
    }

    def >|(
      to: Puzzle,
      trigger: ICondition,
      filter: Filter[String] = Filter.empty) = {
      val transitions = from.lasts.map { c ⇒ new EndExplorationTransition(c, to.first, trigger, filter) }
      Puzzle.merge(from.first, to.lasts, from :: to :: Nil, transitions)
    }

    def --(to: Puzzle, condition: ICondition = ICondition.True, filter: Filter[String] = Filter.empty) = {
      val transitions = from.lasts.map {
        c ⇒ new Transition(c, to.first, condition, filter)
      }
      Puzzle.merge(from.first, to.lasts, from :: to :: Nil, transitions)
    }

    def --(toHead: Puzzle, toTail: Puzzle*) = {
      val toPuzzles = (toHead :: toTail.toList)
      val transitions = for (f ← from.lasts; l ← toPuzzles) yield new Transition(f, l.first)
      Puzzle.merge(from.first, toPuzzles.flatMap { _.lasts }, from :: toPuzzles ::: Nil, transitions)
    }

    def oo(to: Puzzle, filter: Filter[String] = Filter.empty) = {
      val channels = from.lasts.map {
        c ⇒ new DataChannel(c, to.first, filter)
      }
      Puzzle.merge(from.first, to.lasts, from :: to :: Nil, dataChannels = channels)
    }

  }

}
