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
    WorkflowIntegration.DeterministicGA(
      ga.profile[Seq[Double]](
        fitness = Fitness(_.phenotype.head),
        niche = genomeProfile[ga.GAGenome](x, nX)
      ),
      genome,
      Seq(objective)
    )

  def apply(
    x: Int,
    nX: Int,
    genome: Genome,
    objective: Objective,
    replication: Replication,
    paretoSize: Int = 20) = {
    val niche = genomeProfile[ga.GAGenome](x, nX)

    StochasticProfile(
      ga.noisyProfile[Seq[Double]](
        fitness = Fitness(i ⇒ StochasticGAAlgorithm.aggregate(replication.aggregation)(i).head),
        niche = niche,
        nicheSize = paretoSize,
        history = replication.max,
        cloneRate = replication.reevaluate
      ),
      genome,
      Seq(objective),
      replication,
      niche
    )
  }
  object StochasticProfile {
    implicit def OMStochasticProfile = new WorkflowIntegration[StochasticProfile] {
      override def apply(t: StochasticProfile): EvolutionWorkflow =
        new StochasticGAAlgorithm {
          override def replication = t.replication
          override def stateType = PrototypeType[Unit]
          override def genome: Genome = t.genome
          override def objectives: Objectives = t.objectives
          override def algorithm: Algorithm[G, P, S] = t.algo
          override type S = Unit

          override def populationToVariables(population: Population[Individual[G, P]], context: Context)(implicit rng: RandomProvider) = {
            val profile = for { (_, is) ← population.groupBy(t.niche.apply).toVector } yield is.maxBy(_.phenotype.age: Int)
            populationToVariables(profile, context)
          }
        }
    }
  }

  case class StochasticProfile(algo: Algorithm[ga.GAGenome, History[Seq[Double]], Unit], genome: Genome, objectives: Objectives, replication: Replication, niche: Niche[GAGenome, History[Seq[Double]], Int])

}
