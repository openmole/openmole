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
    objectives: Objectives) = OMNSGA2(ga.NSGA2[Seq[Double]](mu, Fitness(_.phenotype)), genome, objectives)

  def apply(
    mu: Int,
    genome: Genome,
    objectives: Objectives,
    replication: Replication) = {
    def fit = Fitness(StochasticGAAlgorithm.aggregate(replication.aggregation))
    OMStochasticNSGA2(ga.noisyNSGA2[Seq[Double]](mu, fit, replication.max, cloneRate = replication.reevaluate), genome, objectives, replication)
  }

  case class OMNSGA2(algo: Algorithm[ga.GAGenome, Seq[Double], Unit], genome: Genome, objectives: Objectives)

  implicit def OMNSGA2Integration = new WorkflowIntegration[OMNSGA2] {
    def apply(algo: OMNSGA2) = WorkflowIntegration.deterministicGAIntegration[Unit](algo.algo, algo.genome, algo.objectives)
  }

  case class OMStochasticNSGA2(algo: Algorithm[ga.GAGenome, History[Seq[Double]], Unit], genome: Genome, objectives: Objectives, replication: Replication)

  implicit def OMStochasticNSGA2Integration = new WorkflowIntegration[OMStochasticNSGA2] {
    override def apply(t: OMStochasticNSGA2): EvolutionWorkflow = WorkflowIntegration.stochasticGAIntegration(t.algo, t.genome, t.objectives, t.replication)
  }

}

