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

package org.openmole.core.implementation

import org.openmole.core.implementation.mole.StrainerCapsule
import org.openmole.core.implementation.puzzle.Puzzle
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.ISlot
import org.openmole.core.model.transition.ITransition

import puzzle._
import task._

package object transition {

  def newJoin = new StrainerCapsule(EmptyTask("join"))
  
  implicit def transitionsPuzzleDecorator(from: Puzzle) = new TransitionDecorator(from)
  implicit def transitionsCapsuleDecorator(from: ICapsule) = new TransitionDecorator(from)
  implicit def transitionsTaskDecorator(from: ITask) = new TransitionDecorator(from)
  implicit def transitionsTaskBuilderDecorator(from: TaskBuilder) = new TransitionDecorator(from.toTask)
  
  def join(first: Puzzle, last: Iterable[Puzzle]) = {
    val join = newJoin
    last foreach { p => new Transition(p.last, join) }
    Puzzle.merge(first.first, join, first :: last.toList)
  }
  
  class TransitionDecorator(from: Puzzle){
     
    def -< (to: Puzzle) = {
      new ExplorationTransition(from.last, to.first)
      from + to
    }
    
    def -< (toHead: Puzzle, toTail: Puzzle*) = {
      val toPuzzles = (toHead :: toTail.toList)
      toPuzzles foreach {
        p =>
        new ExplorationTransition(from.last, p.first)
      }
      join(from, toPuzzles)
    }
    
    def >- (to: Puzzle) = {
      new AggregationTransition(from.last, to.first)
      from + to
    }
    
    def >- (toHead: Puzzle, toTail: Puzzle*) = {
      val toPuzzles = (toHead :: toTail.toList)
      toPuzzles foreach {
        p => 
        new AggregationTransition(from.last, p.first)
      }
      join(from, toPuzzles)
    }
    
    def -- (to: Puzzle) = {
      new Transition(from.last, to.first)
      from + to
    }
    
    def -- (toHead: Puzzle, toTail: Puzzle*) = {
      val toPuzzles = (toHead :: toTail.toList)
      toPuzzles foreach {
        p =>
        new Transition(from.last, p.first)
      }
      join(from, toPuzzles)
    }
  }
  
  implicit def transitionToSlotConverter(transition: ITransition) = transition.end
  implicit def conditionStringConverter(condition: String) = new Condition(condition)
}
