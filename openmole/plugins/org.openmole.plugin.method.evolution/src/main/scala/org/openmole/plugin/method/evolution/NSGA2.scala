/*
 * Copyright (C) 2014 Romain Reuillon
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

import fr.iscpif.mgo.algorithm.{ nsga2, noisynsga2 }

object NSGA2 {

  def apply(
    mu:         Int,
    genome:     Genome,
    objectives: Objectives
  ) = {
    val ug = UniqueGenome(genome)

    new WorkflowIntegration.DeterministicGA(
      nsga2.OpenMOLE(mu, UniqueGenome.size(ug), operatorExploration),
      ug,
      objectives
    )
  }

  def apply(
    mu:          Int,
    genome:      Genome,
    objectives:  Objectives,
    replication: Replication[Seq]
  ) = {
    val ug = UniqueGenome(genome)

    def aggregation(h: Vector[Vector[Double]]) = StochasticGAIntegration.aggregateVector(replication.aggregation, h)

    WorkflowIntegration.StochasticGA(
      noisynsga2.OpenMOLE(mu, operatorExploration, UniqueGenome.size(ug), replication.max, replication.reevaluate, aggregation),
      ug,
      objectives,
      replication
    )
  }

}

