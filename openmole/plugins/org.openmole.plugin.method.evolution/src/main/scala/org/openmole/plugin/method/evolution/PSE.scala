/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.plugin.method.evolution

import fr.iscpif.mgo
import fr.iscpif.mgo.algorithm.{ pse, noisypse }

object PSE {

  def apply(
    genome: Genome,
    objectives: Objectives,
    gridSize: Seq[Double]) = {
    WorkflowIntegration.DeterministicGA(
      pse.OpenMOLE(mgo.niche.grid(gridSize), Genome.size(genome), operatorExploration),
      genome,
      objectives)
  }

  def apply(
    genome: Genome,
    objectives: Objectives,
    gridSize: Seq[Double],
    replication: Replication[Seq[FitnessAggregation]]) = {

    WorkflowIntegration.StochasticGA(
      noisypse.OpenMOLE(
        pattern = mgo.niche.grid(gridSize),
        aggregation = StochasticGAIntegration.aggregateVector(replication.aggregation, _),
        genomeSize = Genome.size(genome),
        historySize = replication.max,
        cloneProbability = replication.reevaluate,
        operatorExploration = operatorExploration),
      genome,
      objectives,
      replication)
  }

}

