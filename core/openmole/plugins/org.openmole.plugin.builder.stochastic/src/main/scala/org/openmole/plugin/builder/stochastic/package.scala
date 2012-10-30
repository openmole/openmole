/*
 * Copyright (C) 2012 Romain Reuillon
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
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.tools._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.sampling._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.plugin.task.stat._

package object stochastic {

  class Statistics {
    var medians: List[(Prototype[Double], Prototype[Double])] = Nil
    var medianAbsoluteDeviations: List[(Prototype[Double], Prototype[Double])] = Nil
    var averages: List[(Prototype[Double], Prototype[Double])] = Nil
    var sums: List[(Prototype[Double], Prototype[Double])] = Nil
    var mses: List[(Prototype[Double], Prototype[Double])] = Nil

    def addMedian(output: Prototype[Double], median: Prototype[Double]) = medians ::= (output, median)
    def addMedianAbsoluteDeviation(output: Prototype[Double], deviation: Prototype[Double]) = medianAbsoluteDeviations ::= (output, deviation)
    def addAverage(output: Prototype[Double], average: Prototype[Double]) = averages ::= (output, average)
    def addSum(output: Prototype[Double], sum: Prototype[Double]) = sums ::= (output, sum)
    def addMeanSquareError(output: Prototype[Double], mse: Prototype[Double]) = mses ::= (output, mse)
  }

  def statistics(
    name: String,
    model: Puzzle,
    replicationFactor: DiscreteFactor[_, _],
    statistics: Statistics)(implicit plugins: PluginSet): Puzzle = {
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

    val endCapsule = Slot(new StrainerCapsule(EmptyTask(name + "End")))

    (explorationCapsule -< model >- aggregationCapsule -- (medianCapsule, medianAbsoluteDeviationCapsule, averageCapsule, sumCapsule, mseCapsule) -- endCapsule) +
      (explorationCapsule oo (endCapsule, filter = Filter(replicationFactor.prototype)))
  }

  def replicate(
    name: String,
    model: Puzzle,
    replications: Sampling)(implicit plugins: PluginSet) = {
    val exploration = ExplorationTask(name + "Replication", replications)
    val explorationCapsule = new StrainerCapsule(exploration)
    val aggregationCapsule = Slot(new StrainerCapsule(EmptyTask(name + "Aggregation")))
    explorationCapsule -< model >- aggregationCapsule + explorationCapsule oo explorationCapsule
  }

}
