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
import algorithm.ga
import fr.iscpif.mgo.algorithm.ga._
import fr.iscpif.mgo.clone.History
import niche._
import fr.iscpif.mgo.fitness._
import org.openmole.core.workflow.data._

object GenomeProfile {

  def apply(
    x: Int,
    nX: Int,
    genome: Genome,
    objective: Objective) =
    DeterministicGenomeProfile(
      ga.profile[Double](
        fitness = Fitness(_.phenotype),
        niche = genomeProfile[ga.GAGenome](x, nX)
      ),
      genome,
      objective
    )

  object DeterministicGenomeProfile {
    implicit val workflowIntegration = new WorkflowIntegration[DeterministicGenomeProfile] {
      override def apply(t: DeterministicGenomeProfile): EvolutionWorkflow = new DeterministicGAAlgorithmIntegration {
        override type S = Unit
        override type P = Double
        override def objectives: Objectives = Seq(t.objective)
        override def genome: Genome = t.genome
        override def valuesToPhenotype(s: Seq[Double]): P = s.head
        override def phenotypeToValues(p: P): Seq[Double] = Seq(p)
        override def phenotypeType: PrototypeType[P] = PrototypeType[P]
        override def algorithm: Algorithm[G, P, S] = t.algo
        override def stateType: PrototypeType[S] = PrototypeType[S]
      }
    }
  }

  case class DeterministicGenomeProfile(algo: Algorithm[ga.GAGenome, Double, Unit], genome: Genome, objective: Objective)

  def apply(
    x: Int,
    nX: Int,
    genome: Genome,
    objective: Objective,
    replication: Replication[FitnessAggregation],
    paretoSize: Int = 20) = {
    val niche = genomeProfile[ga.GAGenome](x, nX)

    StochasticGenomeProfile(
      ga.noisyProfile[Double](
        fitness = Fitness { i ⇒ StochasticGAAlgorithm.aggregate(replication.aggregation, i.phenotype.history) },
        niche = niche,
        nicheSize = paretoSize,
        history = replication.max,
        cloneRate = replication.reevaluate
      ),
      genome,
      objective,
      replication,
      niche
    )
  }

  object StochasticGenomeProfile {
    implicit def OMStochasticProfile = new WorkflowIntegration[StochasticGenomeProfile] {
      override def apply(t: StochasticGenomeProfile): EvolutionWorkflow =
        new StochasticGAAlgorithm {
          override def seed = t.replication.seed
          override def stateType = PrototypeType[Unit]
          override def genome: Genome = t.genome
          override def objectives: Objectives = Seq(t.objective)
          override def algorithm: Algorithm[G, P, S] = t.algo
          override type S = Unit
          override type PC = Double

          override def phenotypeType = PrototypeType[P]
          override def valuesToPhenotype(s: Seq[Double]): P = History(s.head)
          override def phenotypeToValues(p: P): Seq[Double] = Seq(StochasticGAAlgorithm.aggregate(t.replication.aggregation, p.history))

          override def populationToVariables(population: Population[Individual[G, P]]) = {
            val profile = for { (_, is) ← population.groupBy(t.niche.apply).toVector } yield is.maxBy(_.phenotype.age: Int)
            super.populationToVariables(profile)
          }
        }
    }
  }

  case class StochasticGenomeProfile(algo: Algorithm[ga.GAGenome, History[Double], Unit], genome: Genome, objective: Objective, replication: Replication[FitnessAggregation], niche: Niche[GAGenome, History[Double], Int])

}
