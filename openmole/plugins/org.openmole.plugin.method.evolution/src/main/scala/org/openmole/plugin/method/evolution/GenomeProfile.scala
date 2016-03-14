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

import fr.iscpif.mgo.algorithm.{ profile, noisyprofile }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.FromContext

import scalaz._
import Scalaz._

object GenomeProfile {

  def apply(
    x:         Prototype[Double],
    nX:        Int,
    genome:    Genome,
    objective: Objective
  ) = {

    val xIndex =
      genome.indexWhere(_.prototype == x) match {
        case -1 ⇒ throw new UserBadDataError(s"Variable $x not found in the genome")
        case x  ⇒ x
      }

    DeterministicGenomeProfile(
      profile.OpenMOLE(
        genomeSize = Genome.size(genome),
        niche = DeterministicGenomeProfile.niche(xIndex, nX),
        operatorExploration = operatorExploration
      ),
      genome,
      objective
    )
  }

  def apply(
    x:           Prototype[Double],
    nX:          Int,
    genome:      Genome,
    objective:   Objective,
    replication: Replication[Id],
    paretoSize:  Int               = 20
  ) = {

    val xIndex =
      genome.indexWhere(_.prototype == x) match {
        case -1 ⇒ throw new UserBadDataError(s"Variable $x not found in the genome")
        case x  ⇒ x
      }

    def aggregation(h: Vector[Double]) = StochasticGAIntegration.aggregate(replication.aggregationClosures, h)

    StochasticGenomeProfile(
      noisyprofile.OpenMOLE(
        mu = paretoSize,
        niche = StochasticGenomeProfile.niche(xIndex, nX),
        operatorExploration = operatorExploration,
        genomeSize = Genome.size(genome),
        historySize = replication.max,
        cloneProbability = replication.reevaluate,
        aggregation = aggregation
      ),
      genome,
      objective,
      replication
    )
  }

  object DeterministicGenomeProfile {

    import fr.iscpif.mgo

    def niche(x: Int, nX: Int) =
      mgo.niche.genomeProfile[profile.Individual](
        values = (profile.Individual.genome composeLens profile.vectorValues).get,
        x = x,
        nX = nX
      )

    implicit def workflowIntegration = new WorkflowIntegration[DeterministicGenomeProfile] {
      override def apply(a: DeterministicGenomeProfile): EvolutionWorkflow = new EvolutionWorkflow {
        type MGOAG = profile.OpenMOLE
        def mgoAG = a.algo

        type V = Vector[Double]
        type P = Double

        lazy val integration = implicitly[mgo.openmole.Integration[MGOAG, V, P] with mgo.openmole.Profile[MGOAG]]

        def buildIndividual(genome: G, context: Context): I =
          operations.buildIndividual(genome, variablesToPhenotype(context))

        def inputPrototypes = a.genome.map(_.prototype)
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
          )(integration.profile(mgoAG)(population))

        def variablesToPhenotype(context: Context) = context(a.objective)
      }
    }
  }

  case class DeterministicGenomeProfile(algo: profile.OpenMOLE, genome: Genome, objective: Objective)

  object StochasticGenomeProfile {
    import fr.iscpif.mgo
    import mgo.algorithm.noisyprofile._

    def niche(x: Int, nX: Int) =
      mgo.niche.genomeProfile[Individual](
        values = (Individual.genome composeLens noisyprofile.vectorValues).get,
        x = x,
        nX = nX
      )

    implicit def workflowIntegration = new WorkflowIntegration[StochasticGenomeProfile] {
      override def apply(a: StochasticGenomeProfile): EvolutionWorkflow = new EvolutionWorkflow {
        type MGOAG = noisyprofile.OpenMOLE
        def mgoAG = a.algo

        type V = Vector[Double]
        type P = Double

        lazy val integration = implicitly[mgo.openmole.Integration[MGOAG, V, P] with mgo.openmole.Stochastic with mgo.openmole.Profile[MGOAG]]

        def samples = Prototype[Long]("samples", namespace)

        def buildIndividual(genome: G, context: Context): I =
          operations.buildIndividual(genome, variablesToPhenotype(context))

        def inputPrototypes = a.genome.map(_.prototype) ++ a.replication.seed.prototype
        def outputPrototypes = Vector(a.objective)
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

        def variablesToPhenotype(context: Context) = context(a.objective)
      }
    }

  }

  case class StochasticGenomeProfile(
    algo:        noisyprofile.OpenMOLE,
    genome:      Genome,
    objective:   Objective,
    replication: Replication[Id]
  )

}
