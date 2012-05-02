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

package org.openmole.plugin.builder

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.sampling._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.sampling.IDiscreteFactor
import org.openmole.core.model.sampling.ISampling
import org.openmole.core.model.task.IPluginSet
import org.openmole.plugin.builder.Builder._
import org.openmole.plugin.task.stat._

package object stochastic {

  class Statistics {
    var medians: List[(IPrototype[Double], IPrototype[Double])] = Nil
    var medianAbsoluteDeviations: List[(IPrototype[Double], IPrototype[Double])] = Nil
    var averages: List[(IPrototype[Double], IPrototype[Double])] = Nil
    var sums: List[(IPrototype[Double], IPrototype[Double])] = Nil
    var mses: List[(IPrototype[Double], IPrototype[Double])] = Nil

    def addMedian(output: IPrototype[Double], median: IPrototype[Double]) = medians ::= (output, median)
    def addMedianAbsoluteDeviation(output: IPrototype[Double], deviation: IPrototype[Double]) = medianAbsoluteDeviations ::= (output, deviation)
    def addAverage(output: IPrototype[Double], average: IPrototype[Double]) = averages ::= (output, average)
    def addSum(output: IPrototype[Double], sum: IPrototype[Double]) = sums ::= (output, sum)
    def addMeanSquareError(output: IPrototype[Double], mse: IPrototype[Double]) = mses ::= (output, mse)
  }

  def statistics(
    name: String,
    model: Puzzle,
    replicationFactor: DiscreteFactor[_, _],
    statistics: Statistics)(implicit plugins: IPluginSet): Puzzle = {
    val exploration = ExplorationTask(name + "Replication", replicationFactor)
    val explorationCapsule = new StrainerCapsule(exploration)
    val aggregationCapsule = new StrainerCapsule(EmptyTask(name + "Aggregation"))

    val medianTask = MedianTask(name + "Median")
    statistics.medians.foreach { case (out, stat) ⇒ medianTask addSequence (out.toArray, stat) }
    val medianCapsule = new Capsule(medianTask)

    val medianAbsoluteDeviationTask = MedianAbsoluteDeviationTask(name + "MedianAbsoluteDeviation")
    statistics.medianAbsoluteDeviations.foreach { case (out, stat) ⇒ medianAbsoluteDeviationTask addSequence (out.toArray, stat) }
    val medianAbsoluteDeviationCapsule = new Capsule(medianAbsoluteDeviationTask)

    val averageTask = AverageTask(name + "Average")
    statistics.averages.foreach { case (out, stat) ⇒ averageTask addSequence (out.toArray, stat) }
    val averageCapsule = new Capsule(averageTask)

    val sumTask = SumTask(name + "Sum")
    statistics.sums.foreach { case (out, stat) ⇒ sumTask addSequence (out.toArray, stat) }
    val sumCapsule = new Capsule(sumTask)

    val mseTask = MeanSquareErrorTask(name + "MeanSquareError")
    statistics.mses.foreach { case (out, stat) ⇒ mseTask addSequence (out.toArray, stat) }
    val mseCapsule = new Capsule(mseTask)

    val endCapsule = new StrainerCapsule(EmptyTask(name + "End"))

    new ExplorationTransition(explorationCapsule, model.first)
    new AggregationTransition(model.last, aggregationCapsule)

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

    model.copy(first = explorationCapsule, last = endCapsule)
  }

  def replicate(
    name: String,
    model: Puzzle,
    replications: ISampling)(implicit plugins: IPluginSet) = {
    val exploration = ExplorationTask(name + "Replication", replications)
    val explorationCapsule = new StrainerCapsule(exploration)
    val aggregationCapsule = new StrainerCapsule(EmptyTask(name + "Aggregation"))
    new ExplorationTransition(explorationCapsule, model.first)
    new AggregationTransition(model.last, aggregationCapsule)
    new DataChannel(explorationCapsule, aggregationCapsule)
    model.copy(first = explorationCapsule, last = aggregationCapsule)
  }

}
