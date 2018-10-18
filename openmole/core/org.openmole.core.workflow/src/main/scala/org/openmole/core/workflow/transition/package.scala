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
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._

package transition {

  import org.openmole.core.context.Val
  import org.openmole.core.expansion.{ Condition, FromContext }

  trait TransitionDecorator {
    val from: Puzzle

    def when(condition: Condition) = TransitionParameter(from, condition)
    def filter(filter: BlockList) = TransitionParameter(from, filterParameter = filter)
    def keep(prototypes: Val[_]*) = filter(Keep(prototypes: _*))
    def block(prototypes: Val[_]*) = filter(Block(prototypes: _*))

    def -<(
      to:        Puzzle,
      condition: Condition                = Condition.True,
      filter:    BlockList                = BlockList.empty,
      size:      Option[FromContext[Int]] = None
    ): Puzzle = {

      val transitions = from.lasts.map {
        c ⇒
          size match {
            case None    ⇒ new ExplorationTransition(c, to.firstSlot, condition, filter)
            case Some(s) ⇒ new EmptyExplorationTransition(c, to.firstSlot, s, condition, filter)
          }
      }

      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, transitions)
    }

    def -<[T: ToTransitionParameter](ts: T*): Puzzle = {
      val parameters = ts.map(implicitly[ToTransitionParameter[T]].apply)
      def buildTransitions(parameter: TransitionParameter) =
        from.lasts.map { c ⇒ new ExplorationTransition(c, parameter.puzzleParameter.firstSlot, parameter.conditionParameter, parameter.filterParameter) }
      val transitions = parameters.flatMap { buildTransitions }
      Puzzle.merge(from.firstSlot, parameters.flatMap(_.puzzleParameter.lasts), from :: parameters.map(_.puzzleParameter).toList, transitions)
    }

    def -<-(toPuzzles: Puzzle*) = {
      val transitions = for (f ← from.lasts; l ← toPuzzles) yield new SlaveTransition(f, l.firstSlot)
      Puzzle.merge(from.firstSlot, toPuzzles.flatMap {
        _.lasts
      }, from :: toPuzzles.toList ::: Nil, transitions)
    }

    def >-[T: ToTransitionParameter](ts: T*): Puzzle = {
      val parameters = ts.map(implicitly[ToTransitionParameter[T]].apply)
      def buildTransitions(parameter: TransitionParameter) =
        from.lasts.map { c ⇒ new AggregationTransition(c, parameter.puzzleParameter.firstSlot, parameter.conditionParameter, parameter.filterParameter) }
      val transitions = parameters.flatMap { buildTransitions }
      Puzzle.merge(from.firstSlot, parameters.flatMap(_.puzzleParameter.lasts), from :: parameters.map(_.puzzleParameter).toList, transitions)
    }

    def >|[T: ToTransitionParameter](t: T) = {
      val parameter = implicitly[ToTransitionParameter[T]].apply(t)
      val trigger = parameter.conditionParameter
      val filter = parameter.filterParameter
      val toPuzzle = parameter.puzzleParameter
      val transitions = from.lasts.map { c ⇒ new EndExplorationTransition(c, toPuzzle.firstSlot, trigger, filter) }
      Puzzle.merge(from.firstSlot, toPuzzle.lasts, from :: toPuzzle :: Nil, transitions)
    }

    private def buildTransitions(parameter: TransitionParameter) =
      from.lasts.map { c ⇒ new Transition(c, parameter.puzzleParameter.firstSlot, parameter.conditionParameter, parameter.filterParameter) }

    def --[T: ToTransitionParameter](ts: T*): Puzzle = {
      val parameters = ts.map(implicitly[ToTransitionParameter[T]].apply)
      val transitions = parameters.flatMap { buildTransitions }
      Puzzle.merge(from.firstSlot, parameters.flatMap(_.puzzleParameter.lasts), from :: parameters.map(_.puzzleParameter).toList, transitions)
    }

    private def --=(to: Puzzle, condition: Condition = Condition.True, filter: BlockList = BlockList.empty): Puzzle = {
      val transitions =
        from.lasts.map {
          c ⇒ new Transition(c, Slot(to.first), condition, filter)
        }
      Puzzle.merge(from.firstSlot, to.lasts, from :: to :: Nil, transitions)
    }

    def --=[T: ToTransitionParameter](ts: T*): Puzzle = {
      val parameters = ts.map(implicitly[ToTransitionParameter[T]].apply)
      val puzzles = parameters.map { case TransitionParameter(t, condition, filter) ⇒ this.--=(t, condition, filter) }
      Puzzle.merge(from.firstSlot, puzzles.flatMap(_.lasts), puzzles)
    }

    def oo[T: ToTransitionParameter](t: T): Puzzle = {
      val parameters = implicitly[ToTransitionParameter[T]].apply(t)
      oo(parameters.puzzleParameter, parameters.filterParameter)
    }

    def oo(to: Puzzle, prototypes: Val[_]*): Puzzle = {
      def blockList: BlockList = if (prototypes.isEmpty) BlockList.empty else Keep(prototypes: _*)
      oo(to, filter = blockList)
    }

    def oo(to: Puzzle, filter: BlockList = BlockList.empty): Puzzle = {
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

package object transition