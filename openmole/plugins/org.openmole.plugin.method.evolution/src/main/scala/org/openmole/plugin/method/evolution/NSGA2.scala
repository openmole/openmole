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

import fr.iscpif.mgo._
import algorithm._
import fitness._
import fr.iscpif.mgo.clone.History
import org.openmole.core.workflow.data.PrototypeType
import org.openmole.core.workflow.tools.TextClosure
import org.openmole.tool.statistics._

object NSGA2 {

  def apply(
    mu: Int,
    genome: Genome,
    objectives: Objectives) =
    WorkflowIntegration.DeterministicGA(
      ga.NSGA2[Seq[Double]](mu, Fitness(_.phenotype)),
      genome,
      objectives)

  def apply(
    mu: Int,
    genome: Genome,
    objectives: Objectives,
    replication: Replication[Seq[FitnessAggregation]]) = {

    def fit = Fitness((i: Individual[Any, History[Seq[Double]]]) ⇒ StochasticGAAlgorithm.aggregateSeq(replication.aggregation, i.phenotype.history))

    WorkflowIntegration.StochasticGA(
      ga.noisyNSGA2[Seq[Double]](mu, fit, replication.max, cloneRate = replication.reevaluate),
      genome,
      objectives,
      replication)
  }

}

