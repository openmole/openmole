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

import mgo.algorithm._
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.dsl._

import cats._
import cats.implicits._

object GenomeProfile {

  def apply(
    x:         Val[Double],
    nX:        Int,
    genome:    Genome,
    objective: Objective
  ): DeterministicGenomeProfile = {
    val ug = UniqueGenome(genome)

    val xIndex =
      ug.inputs.indexWhere(_.prototype == x) match {
        case -1 ⇒ throw new UserBadDataError(s"Variable $x not found in the genome")
        case x  ⇒ x
      }

    DeterministicGenomeProfile(
      profile.OpenMOLE(
        genomeSize = UniqueGenome.size(ug),
        niche = DeterministicGenomeProfile.niche(xIndex, nX),
        operatorExploration = operatorExploration
      ),
      ug,
      objective
    )
  }

  def apply(
    x:          Val[Double],
    nX:         Int,
    genome:     Genome,
    objective:  Objective,
    stochastic: Stochastic[Id],
    paretoSize: Int            = 20
  ): StochasticGenomeProfile = {
    val ug = UniqueGenome(genome)

    val xIndex =
      ug.indexWhere(_.prototype == x) match {
        case -1 ⇒ throw new UserBadDataError(s"Variable $x not found in the genome")
        case x  ⇒ x
      }

    def aggregation(h: Vector[Double]) = StochasticGAIntegration.aggregate(stochastic.aggregation, h)

    StochasticGenomeProfile(
      noisyprofile.OpenMOLE(
        mu = paretoSize,
        niche = StochasticGenomeProfile.niche(xIndex, nX),
        operatorExploration = operatorExploration,
        genomeSize = UniqueGenome.size(ug),
        historySize = stochastic.replications,
        cloneProbability = stochastic.reevaluate,
        aggregation = aggregation
      ),
      ug,
      objective,
      stochastic
    )
  }

  object DeterministicGenomeProfile {

    def niche(x: Int, nX: Int) =
      mgo.algorithm.profile.genomeProfile[profile.Individual](
        values = (profile.Individual.genome composeLens profile.vectorValues).get,
        x = x,
        nX = nX
      )

    implicit def workflowIntegration: WorkflowIntegration[DeterministicGenomeProfile] = new WorkflowIntegration[DeterministicGenomeProfile] {
      override def apply(a: DeterministicGenomeProfile): EvolutionWorkflow = new EvolutionWorkflow {
        type MGOAG = profile.OpenMOLE
        def mgoAG = a.algo

        type V = Vector[Double]
        type P = Double

        lazy val integration = implicitly[mgo.openmole.Integration[MGOAG, V, P] with mgo.openmole.Profile[MGOAG]]

        def buildIndividual(genome: G, context: Context): I =
          operations.buildIndividual(genome, variablesToPhenotype(context))

        def inputPrototypes = a.genome.inputs.map(_.prototype)
        def objectives = Seq(a.objective)
        def resultPrototypes = (inputPrototypes ++ outputPrototypes).distinct

        def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]] =
          GAIntegration.scaled(a.genome, operations.values(genome))

        def populationToVariables(population: Pop): FromContext[Seq[Variable[_]]] =
          GAIntegration.populationToVariables[I](
            a.genome,
            Seq(a.objective),
            operations.genomeValues,
            i ⇒ Vector(operations.phenotype(i))
          )(integration.profile(mgoAG)(population))

        def variablesToPhenotype(context: Context) = a.objective.fromContext(context)
      }
    }
  }

  case class DeterministicGenomeProfile(algo: profile.OpenMOLE, genome: UniqueGenome, objective: Objective)

  object StochasticGenomeProfile {
    import mgo.algorithm.noisyprofile._

    def niche(x: Int, nX: Int) =
      mgo.algorithm.profile.genomeProfile[Individual](
        values = (Individual.genome composeLens noisyprofile.vectorValues).get,
        x = x,
        nX = nX
      )

    implicit def workflowIntegration: WorkflowIntegration[StochasticGenomeProfile] = new WorkflowIntegration[StochasticGenomeProfile] {
      override def apply(a: StochasticGenomeProfile): EvolutionWorkflow = new EvolutionWorkflow {
        type MGOAG = noisyprofile.OpenMOLE
        def mgoAG = a.algo

        type V = Vector[Double]
        type P = Double

        lazy val integration = implicitly[mgo.openmole.Integration[MGOAG, V, P] with mgo.openmole.Stochastic with mgo.openmole.Profile[MGOAG]]

        def samples = Val[Long]("samples", namespace)

        def buildIndividual(genome: G, context: Context): I =
          operations.buildIndividual(genome, variablesToPhenotype(context))

        import UniqueGenome._

        def inputPrototypes = a.genome.map(_.prototype) ++ a.replication.seed.prototype
        def objectives = Vector(a.objective)
        def resultPrototypes = (a.genome.map(_.prototype) ++ outputPrototypes ++ Seq(samples)).distinct

        def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]] =
          StochasticGAIntegration.genomeToVariables(a.genome, operations.values(genome), a.replication.seed)

        def populationToVariables(population: Pop): FromContext[Seq[Variable[_]]] =
          StochasticGAIntegration.populationToVariables[I](
            a.genome,
            Vector(a.objective),
            operations.genomeValues,
            i ⇒ Vector(operations.phenotype(i)),
            samples,
            integration.samples
          )(integration.profile(mgoAG)(population))

        def variablesToPhenotype(context: Context) = a.objective.fromContext(context)
      }
    }

  }

  case class StochasticGenomeProfile(
    algo:        noisyprofile.OpenMOLE,
    genome:      UniqueGenome,
    objective:   Objective,
    replication: Stochastic[Id]
  )

}
