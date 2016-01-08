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

import fr.iscpif.mgo
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.FromContext

import scalaz._
import Scalaz._

object GenomeProfile {

  def apply(
    x: Int,
    nX: Int,
    genome: Genome,
    objective: Objective) =
    DeterministicGenomeProfile(
      mgo.algorithm.Profile.OpenMOLE(
        mu = 1,
        genomeSize = genome.size,
        niche = DeterministicGenomeProfile.niche(x, nX),
        operatorExploration = operatorExploration
      ),
      genome,
      objective
    )

  object DeterministicGenomeProfile {

    import mgo.algorithm.Profile.Algorithm._

    def niche(x: Int, nX: Int) =
      mgo.niche.genomeProfile[Individual](
        values = (Individual.genome composeLens Genome.values).get,
        x = 0,
        nX = 10)

    implicit def workflowIntegration = new WorkflowIntegration[DeterministicGenomeProfile] {
      override def apply(a: DeterministicGenomeProfile): EvolutionWorkflow = new EvolutionWorkflow {
        type MGOAG = mgo.algorithm.Profile.OpenMOLE
        def mgoAG = a.algo

        type V = Vector[Double]
        type P = Double

        lazy val integration = implicitly[mgo.openmole.Integration[MGOAG, V, P]]

        def buildIndividual(genome: G, context: Context): I =
          operations.buildIndividual(genome, variablesToPhenotype(context))

        def inputPrototypes = a.genome.inputs.map(_.prototype)
        def outputPrototypes = Seq(a.objective)
        def resultPrototypes = (inputPrototypes ++ outputPrototypes).distinct

        def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]] =
          GAIntegration.scaled(a.genome, operations.values(genome))

        def populationToVariables(population: Pop): FromContext[Seq[Variable[_]]] =
          GAIntegration.populationToVariables[I](
            a.genome,
            Seq(a.objective),
            operations.genomeValues,
            i ⇒ Vector(operations.phenotype(i))
          )(population)

        def variablesToPhenotype(context: Context) = context(a.objective)
      }
    }
  }

  case class DeterministicGenomeProfile(algo: mgo.algorithm.Profile.OpenMOLE, genome: Genome, objective: Objective)

  //
  //  def apply(
  //    x: Int,
  //    nX: Int,
  //    genome: Genome,
  //    objective: Objective,
  //    replication: Replication[FitnessAggregation],
  //    paretoSize: Int = 20) = {
  //    val niche = genomeProfile[ga.GAGenome](x, nX)
  //
  //    StochasticGenomeProfile(
  //      ga.noisyProfile[Double](
  //        fitness = Fitness { i ⇒ StochasticGAIntegration.aggregate(replication.aggregation, i.phenotype.history) },
  //        niche = niche,
  //        nicheSize = paretoSize,
  //        history = replication.max,
  //        cloneRate = replication.reevaluate
  //      ),
  //      genome,
  //      objective,
  //      replication,
  //      niche
  //    )
  //  }
  //
  //  object StochasticGenomeProfile {
  //    implicit def OMStochasticProfile = new WorkflowIntegration[StochasticGenomeProfile] {
  //      override def apply(a: StochasticGenomeProfile): EvolutionWorkflow =
  //        new EvolutionWorkflow {
  //          type G = GAGenome
  //          def genomeType: PrototypeType[G] = PrototypeType[G]
  //          def randomGenome = ga.randomGenome(a.genome.size)
  //
  //          type P = History[Double]
  //          override def phenotypeType: PrototypeType[P] = PrototypeType[P]
  //
  //          override type S = Unit
  //          override def stateType = PrototypeType[Unit]
  //
  //          def replications = Prototype[Int]("replications", namespace)
  //
  //          def inputPrototypes = a.genome.inputs.map(_.prototype) ++ a.replication.seed.prototype
  //          def outputPrototypes: Seq[Prototype[_]] = Seq(a.objective)
  //          def resultPrototypes = (a.genome.inputs.map(_.prototype) ++ outputPrototypes ++ Seq(replications)).distinct
  //
  //          override def algorithm: Algorithm[G, P, S] = a.algo
  //
  //          def variablesToPhenotype(context: Context): P = History(context(a.objective))
  //
  //          def phenotypeToValues(p: P): Double =
  //            StochasticGAIntegration.aggregate(a.replication.aggregation, p.history)
  //
  //          def populationToVariables(population: Population[Individual[G, P]]) = {
  //            val profile = for { (_, is) ← population.groupBy(a.niche.apply).toVector } yield is.maxBy(_.phenotype.age: Int)
  //            StochasticGAIntegration.populationToVariables[Double](a.genome, Seq(a.objective), replications, p ⇒ Seq(phenotypeToValues(p)))(profile)
  //          }
  //
  //          def genomeToVariables(genome: G) =
  //            StochasticGAIntegration.genomeToVariables(a.genome, genomeValues.get(genome), a.replication.seed)
  //
  //          def prepareIndividualForIsland(i: Ind) = i.copy(phenotype = i.phenotype.copy(age = 0))
  //        }
  //    }
  //  }

  //  case class StochasticGenomeProfile(
  //    algo: mgo.algorithm.NoisyProfile.OpenMOLE,
  //    genome: Genome,
  //    objective: Objective,
  //    replication: Replication[FitnessAggregation],
  //    niche: mgo.niche.Niche[mgo.algorithm.NoisyProfile.Algorithm.Individual, History[Double], Int])

}
