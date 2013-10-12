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
import org.openmole.core.implementation.validation.Validation
import org.openmole.core.implementation.validation.DataflowProblem.MissingInput

package object stochastic {

  object Statistics {
    def apply() = new Statistics
  }

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

    def create(seq: List[(Prototype[Double], Prototype[Double])], builder: DoubleSequenceStatTaskBuilder) =
      if (!seq.isEmpty) {
        seq.foreach { case (out, stat) ⇒ builder addSequence (out.toArray, stat) }
        Some(Capsule(builder))
      }
      else None

    val capsules =
      List(
        create(statistics.medians, MedianTask(name + "Median")),
        create(statistics.medianAbsoluteDeviations, MedianAbsoluteDeviationTask(name + "MedianAbsoluteDeviation")),
        create(statistics.averages, AverageTask(name + "Average")),
        create(statistics.sums, SumTask(name + "Sum")),
        create(statistics.mses, MeanSquareErrorTask(name + "MeanSquareError"))
      ).flatten.map(_.toPuzzle)

    Validation(exploration -< model) foreach {
      case MissingInput(_, d) ⇒
        exploration.addInput(d)
        exploration.addOutput(d)
      case _ ⇒
    }

    val explorationCapsule = StrainerCapsule(exploration)
    val aggregationCapsule = StrainerCapsule(EmptyTask(name + "Aggregation"))
    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    explorationCapsule -< model >- aggregationCapsule -- capsules -- endCapsule

    //+(explorationCapsule oo (endCapsule, filter = Block(replicationFactor.prototype)))
  }

  def replicate(
    name: String,
    model: Puzzle,
    replications: Sampling)(implicit plugins: PluginSet) = {
    val exploration = ExplorationTask(name + "Replication", replications)

    Validation(exploration -< model) foreach {
      case MissingInput(_, d) ⇒
        exploration.addInput(d)
        exploration.addOutput(d)
      case _ ⇒
    }

    val explorationCapsule = StrainerCapsule(exploration)
    val aggregationCapsule = Slot(StrainerCapsule(EmptyTask(name + "Aggregation")))
    explorationCapsule -< model >- aggregationCapsule + explorationCapsule oo aggregationCapsule
  }

}
