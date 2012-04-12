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

import org.openmole.core.implementation.puzzle.PuzzleFirstAndLast
import org.openmole.core.model.IPuzzleFirstAndLast
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.ITransition

import puzzle._
import task._

package object transition {

  implicit def transitionsPuzzleDecorator(from: IPuzzleFirstAndLast) = new TransitionDecorator(from)
  implicit def transitionsCapsuleDecorator(from: ICapsule) = new TransitionDecorator(from)
  implicit def transitionsTaskDecorator(from: ITask) = new TransitionDecorator(from)
  
  class TransitionDecorator(from: IPuzzleFirstAndLast){
    def -< (to: IPuzzleFirstAndLast) = {
      new ExplorationTransition(from.last, to.first)
      new PuzzleFirstAndLast(from.first, to.last)
    }
    
    def >- (to: IPuzzleFirstAndLast) = {
      new AggregationTransition(from.last, to.first)
      new PuzzleFirstAndLast(from.first, to.last)
    }
    
    def -- (to: IPuzzleFirstAndLast) = {
      new Transition(from.last, to.first)
      new PuzzleFirstAndLast(from.first, to.last)
    }
  }
  
  implicit def transitionToSlotConverter(transition: ITransition) = transition.end
  implicit def conditionStringConverter(condition: String) = new Condition(condition)
}
