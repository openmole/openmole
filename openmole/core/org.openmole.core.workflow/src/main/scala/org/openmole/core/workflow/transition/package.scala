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

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.transition._

import org.openmole.core.workflow.puzzle._
import task._
import data._

package transition {

  case class TransitionParameter(
      puzzleParameter: Puzzle,
      conditionParameter: Condition = Condition.True,
      filterParameter: BlockList[String] = BlockList.empty) {
    def when(condition: Condition) = copy(conditionParameter = condition)
    def filter(filter: BlockList[String]) = copy(filterParameter = filter)
    def keep(prototypes: Prototype[_]*) = filter(Keep(prototypes.map(_.name): _*))
  }

  trait TransitionDecorator {
    def from: Puzzle

    def when(condition: Condition) = TransitionParameter(from, condition)
    def filter(filter: BlockList[String]) = TransitionParameter(from, filterParameter = filter)
    def keep(prototypes: Prototype[_]*) = filter(Keep(prototypes.map(_.name): _*))

    def -<(
      to: Puzzle,
      condition: Condition = Condition.True,
      filter: BlockList[String] = BlockList.empty,
      size: Option[FromContext[Int]] = None): Puzzle = {

      val transitions = from.lasts.map {
        c ⇒
          size match {
            case None    ⇒ new ExplorationTransition(c, to.firstSlot, condition, filter)
            case Some(s) ⇒ new EmptyExplorationTransition(c, to.firstSlot, s, condition, filter)
          }
      }

      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, transitions)
    }

    def -<(toHead: Puzzle, toTail: Puzzle*): Puzzle = -<((toHead :: toTail.toList).map(TransitionParameter(_)): _*)

    def -<(parameters: TransitionParameter*): Puzzle = {
      def buildTransitions(parameter: TransitionParameter) =
        from.lasts.map { c ⇒ new ExplorationTransition(c, parameter.puzzleParameter.firstSlot, parameter.conditionParameter, parameter.filterParameter) }
      val transitions = parameters.flatMap { buildTransitions }
      Puzzle.merge(from.firstSlot, parameters.flatMap(_.puzzleParameter.lasts), from :: parameters.map(_.puzzleParameter).toList, transitions)
    }

    def -<-(
      to: Puzzle,
      condition: Condition = Condition.True,
      filter: BlockList[String] = BlockList.empty) = {

      val transitions = from.lasts.map {
        c ⇒ new SlaveTransition(c, to.firstSlot, condition, filter)
      }

      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, transitions)
    }

    def -<-(toHead: Puzzle, toTail: Puzzle*): Puzzle = -<-(toHead :: toTail.toList)

    def -<-(toPuzzles: Seq[Puzzle]) = {
      val transitions = for (f ← from.lasts; l ← toPuzzles) yield new SlaveTransition(f, l.firstSlot)
      Puzzle.merge(from.firstSlot, toPuzzles.flatMap {
        _.lasts
      }, from :: toPuzzles.toList ::: Nil, transitions)
    }

    def >-(
      to: Puzzle,
      condition: Condition = Condition.True,
      filter: BlockList[String] = BlockList.empty,
      trigger: Condition = Condition.False): Puzzle = {
      val transitions = from.lasts.map { c ⇒ new AggregationTransition(c, to.firstSlot, condition, filter, trigger) }
      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, transitions)
    }

    def >-(toHead: Puzzle, toTail: Puzzle*): Puzzle = >-((toHead :: toTail.toList).map(TransitionParameter(_)): _*)

    def >-(parameters: TransitionParameter*): Puzzle = {
      def buildTransitions(parameter: TransitionParameter) =
        from.lasts.map { c ⇒ new AggregationTransition(c, parameter.puzzleParameter.firstSlot, parameter.conditionParameter, parameter.filterParameter) }
      val transitions = parameters.flatMap { buildTransitions }
      Puzzle.merge(from.firstSlot, parameters.flatMap(_.puzzleParameter.lasts), from :: parameters.map(_.puzzleParameter).toList, transitions)
    }

    def >|(
      to: Puzzle,
      trigger: Condition,
      filter: BlockList[String] = BlockList.empty) = {
      val transitions = from.lasts.map { c ⇒ new EndExplorationTransition(c, to.firstSlot, trigger, filter) }
      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, transitions)
    }

    private def buildTransitions(parameter: TransitionParameter) =
      from.lasts.map { c ⇒ new Transition(c, parameter.puzzleParameter.firstSlot, parameter.conditionParameter, parameter.filterParameter) }

    def --(to: Puzzle, condition: Condition = Condition.True, filter: BlockList[String] = BlockList.empty): Puzzle = {
      val transitions = buildTransitions(TransitionParameter(to, condition, filter))
      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, transitions)
    }

    def --(head: Puzzle, tail: Puzzle*): Puzzle = this.--((Seq(head) ++ tail).map(TransitionParameter(_)): _*)

    def --(parameters: TransitionParameter*): Puzzle = {
      val transitions = parameters.flatMap { buildTransitions }
      Puzzle.merge(from.firstSlot, parameters.flatMap(_.puzzleParameter.lasts), from :: parameters.map(_.puzzleParameter).toList, transitions)
    }

    def --=(to: Puzzle, condition: Condition = Condition.True, filter: BlockList[String] = BlockList.empty): Puzzle = {
      val transitions =
        from.lasts.map {
          c ⇒ new Transition(c, Slot(to.first), condition, filter)
        }
      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, transitions)
    }

    def --=(head: Puzzle, tail: Puzzle*): Puzzle = this.--=((Seq(head) ++ tail).map(TransitionParameter(_)): _*)

    def --=(parameters: TransitionParameter*): Puzzle = {
      val puzzles = parameters.map { case TransitionParameter(t, condition, filter) ⇒ this.--=(t, condition, filter) }
      Puzzle.merge(from.firstSlot, puzzles.flatMap(_.lasts), puzzles)
    }

    def oo(to: Puzzle, prototypes: Prototype[_]*): Puzzle = {
      def blockList: BlockList[String] = if (prototypes.isEmpty) BlockList.empty else Keep(prototypes.map(_.name): _*)
      oo(to, filter = blockList)
    }

    def oo(to: Puzzle, filter: BlockList[String] = BlockList.empty): Puzzle = {
      val channels = from.lasts.map {
        c ⇒ new DataChannel(c, to.firstSlot, filter)
      }
      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, dataChannels = channels)
    }

  }

  trait TransitionPackage {
    implicit def taskToSlotConverter(task: Task) = Slot(Capsule(task))
    implicit def transitionToSlotConverter(transition: ITransition) = transition.end
  }
}

package object transition extends TransitionPackage