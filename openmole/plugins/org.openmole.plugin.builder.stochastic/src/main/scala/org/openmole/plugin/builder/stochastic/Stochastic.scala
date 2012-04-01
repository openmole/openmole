/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.builder.stochastic

import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.mole.StrainerCapsule
import org.openmole.core.implementation.puzzle.PuzzleFirstAndLast
import org.openmole.core.implementation.sampling.DiscreteFactor
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.transition.AggregationTransition
import org.openmole.core.implementation.transition.ExplorationTransition
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.implementation.data.DataChannel
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.model.IPuzzleFirstAndLast
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.plugin.builder.Builder._
import org.openmole.plugin.task.stat.MedianAbsoluteDeviationTask
import org.openmole.plugin.task.stat.MedianTask

object Stochastic {

  class Medians {
    var medians: List[(IPrototype[Double], IPrototype[Double], IPrototype[Double])] = Nil
    def add(output: IPrototype[Double], median: IPrototype[Double], deviation: IPrototype[Double]) = 
      medians ::= (output, median, deviation)
  }
  
  def medians = new Medians
  
  def medianAndDeviation(
    puzzle: IPuzzleFirstAndLast,
    replicationFactor: DiscreteFactor[_, _],
    medians: Medians
  ): IPuzzleFirstAndLast = {
    val exploration = new ExplorationTask("replication", replicationFactor)
    
    val explorationCapsule = new StrainerCapsule(exploration)
    
    val medianTask = new MedianTask("median")
    medians.medians.foreach{ case (output, median, _) => medianTask.median(toArray(output), median) }
    
    val medianCapsule = new Capsule(medianTask)
    
    val medianAbsoluteDeviationTask = new MedianAbsoluteDeviationTask("medianAbsoluteDeviation")
    medians.medians.foreach{ case (output, _, deviation) => medianAbsoluteDeviationTask.deviation(toArray(output), deviation) }
    
    val medianAbsoluteDeviationCapsule = new Capsule(medianAbsoluteDeviationTask)
    
    val aggregationCapsule = new StrainerCapsule(new EmptyTask("aggregation"))
    
    val endCapsule = new StrainerCapsule(new EmptyTask("end"))
    
    new ExplorationTransition(explorationCapsule, puzzle.first)
    new AggregationTransition(puzzle.last, aggregationCapsule)
    new Transition(aggregationCapsule, medianCapsule)
    new Transition(aggregationCapsule, medianAbsoluteDeviationCapsule)
    new Transition(medianCapsule, endCapsule)
    new Transition(medianAbsoluteDeviationCapsule, endCapsule)
  
    new DataChannel(explorationCapsule, endCapsule)
    
    new PuzzleFirstAndLast(explorationCapsule, endCapsule)
  }
  
  def medianAndDeviation(
    model: ICapsule,
    replicationFactor: DiscreteFactor[_, _],
    medians: Medians
  ): IPuzzleFirstAndLast = 
      medianAndDeviation(puzzle(model), replicationFactor, medians)
  
}
