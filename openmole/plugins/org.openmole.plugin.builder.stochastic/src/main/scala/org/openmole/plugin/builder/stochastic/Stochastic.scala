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
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.IPuzzleFirstAndLast
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.plugin.builder.Builder._
import org.openmole.plugin.task.stat.AverageTask
import org.openmole.plugin.task.stat.AverageTask
import org.openmole.plugin.task.stat.MeanSquareErrorTask
import org.openmole.plugin.task.stat.MedianAbsoluteDeviationTask
import org.openmole.plugin.task.stat.MedianTask
import org.openmole.plugin.task.stat.SumTask

object Stochastic {

  class Statistics {
    var medians: List[(IPrototype[Double], IPrototype[Double])] = Nil
    var medianAbsoluteDeviations: List[(IPrototype[Double], IPrototype[Double])] = Nil
    var averages: List[(IPrototype[Double], IPrototype[Double])] = Nil
    var sums: List[(IPrototype[Double], IPrototype[Double])] = Nil
    var mses: List[(IPrototype[Double], IPrototype[Double])] = Nil
    
    def median(output: IPrototype[Double], median: IPrototype[Double]) = medians ::= (output, median)
    def medianAbsoluteDeviation(output: IPrototype[Double], deviation: IPrototype[Double]) = medianAbsoluteDeviations ::= (output, deviation)
    def average(output: IPrototype[Double], average: IPrototype[Double]) = averages ::= (output, average)
    def sum(output: IPrototype[Double], sum: IPrototype[Double]) = sums ::= (output, sum)
    def meanSquareError(output: IPrototype[Double], mse: IPrototype[Double]) = mses ::= (output, mse)                                                                
  }
  
  def statistics = new Statistics
  
  private def toArray(x: List[(IPrototype[Double], IPrototype[Double])]) =
    x.map { case(output, stat) => (Prototype.toArray(output), stat)}
  
  def medianAndDeviation(
    puzzle: IPuzzleFirstAndLast,
    replicationFactor: DiscreteFactor[_, _],
    statistics: Statistics
  ): IPuzzleFirstAndLast = {
    val exploration = new ExplorationTask("replication", replicationFactor)
    
    val explorationCapsule = new StrainerCapsule(exploration)
    
    val medianTask = new MedianTask("median")
    medianTask.add(toArray(statistics.medians))
    val medianCapsule = new Capsule(medianTask)
    
    val medianAbsoluteDeviationTask = new MedianAbsoluteDeviationTask("medianAbsoluteDeviation")
    medianAbsoluteDeviationTask.add(toArray(statistics.medianAbsoluteDeviations))
    val medianAbsoluteDeviationCapsule = new Capsule(medianAbsoluteDeviationTask)
    
    val averageTask = new AverageTask("average")
    averageTask.add(toArray(statistics.averages))
    val averageCapsule = new Capsule(averageTask)
    
    val sumTask = new SumTask("sum")
    sumTask.add(toArray(statistics.sums))
    val sumCapsule = new Capsule(sumTask)
    
    val mseTask = new MeanSquareErrorTask("meanSquareError")
    mseTask.add(toArray(statistics.mses))
    val mseCapsule = new Capsule(mseTask)
    
    val aggregationCapsule = new StrainerCapsule(new EmptyTask("aggregation"))
    
    val endCapsule = new StrainerCapsule(new EmptyTask("end"))
    
    new ExplorationTransition(explorationCapsule, puzzle.first)
    new AggregationTransition(puzzle.last, aggregationCapsule)
    
    new Transition(aggregationCapsule, medianCapsule)
    new Transition(aggregationCapsule, medianAbsoluteDeviationCapsule)
    new Transition(aggregationCapsule, averageCapsule)
    new Transition(aggregationCapsule, sumCapsule)
    new Transition(aggregationCapsule, mseCapsule)
    
    new Transition(medianCapsule, endCapsule)
    new Transition(medianAbsoluteDeviationCapsule, endCapsule)
    new Transition(averageCapsule, endCapsule)
    new Transition(sumCapsule, endCapsule)
    new Transition(mseCapsule, endCapsule)

    new DataChannel(explorationCapsule, endCapsule)
    
    new PuzzleFirstAndLast(explorationCapsule, endCapsule)
  }
  
  def medianAndDeviation(
    model: ICapsule,
    replicationFactor: DiscreteFactor[_, _],
    statistics: Statistics
  ): IPuzzleFirstAndLast = 
      medianAndDeviation(puzzle(model), replicationFactor, statistics)
  
}
