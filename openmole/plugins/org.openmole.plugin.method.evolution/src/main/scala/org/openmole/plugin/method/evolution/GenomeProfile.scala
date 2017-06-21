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
import mgo.niche._
import monocle.macros._

object GenomeProfile {

  object DeterministicParams {

    import mgo.algorithm._
    import mgo.algorithm.profile._
    import context._
    import context.implicits._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, Vector[Double], Double] with MGOAPI.Profile[DeterministicParams] {
      type M[A] = context.M[A]
      type G = mgo.algorithm.profile.Genome
      type I = Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def mMonad = implicitly
      def mGeneration = implicitly
      def mStartTime = implicitly

      def operations(om: DeterministicParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Double = Individual.fitness.get(individual)
        def buildIndividual(genome: G, phenotype: Double) = Individual(genome, phenotype, 0)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())
        def initialGenomes(n: Int) = mgo.algorithm.profile.initialGenomes(n, om.genomeSize)
        def breeding(n: Int) = mgo.algorithm.profile.breeding(n, om.niche, om.operatorExploration)
        def elitism = mgo.algorithm.profile.elitism(om.niche)
        def migrateToIsland(population: Vector[I]) = population
        def migrateFromIsland(population: Vector[I]) = population
      }

      def run[A](s: S, x: M[A]) = {
        val res =
          for {
            xv ← x
            s ← mgo.algorithm.profile.state[M]
          } yield (s, xv)
        interpreter(s).run(res).right.get
      }

      def profile(om: DeterministicParams)(population: Vector[I]) = population
    }
  }

  case class DeterministicParams(niche: Niche[mgo.algorithm.profile.Individual, Int], genomeSize: Int, operatorExploration: Double)

  object DeterministicGenomeProfile {

    def niche(x: Int, nX: Int) =
      mgo.algorithm.profile.genomeProfile[profile.Individual](
        values = (profile.Individual.genome composeLens profile.vectorValues).get,
        x = x,
        nX = nX
      )

    implicit def workflowIntegration: WorkflowIntegration[DeterministicGenomeProfile] = new WorkflowIntegration[DeterministicGenomeProfile] {
      override def apply(a: DeterministicGenomeProfile): EvolutionWorkflow = new EvolutionWorkflow {
        type MGOAG = DeterministicParams
        def mgoAG = a.algo

        type V = Vector[Double]
        type P = Double

        lazy val integration = implicitly[MGOAPI.Integration[MGOAG, V, P] with MGOAPI.Profile[MGOAG]]

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

  case class DeterministicGenomeProfile(algo: GenomeProfile.DeterministicParams, genome: UniqueGenome, objective: Objective)

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
      DeterministicParams(
        genomeSize = UniqueGenome.size(ug),
        niche = DeterministicGenomeProfile.niche(xIndex, nX),
        operatorExploration = operatorExploration
      ),
      ug,
      objective
    )
  }

  case class StochasticParams(
    mu:                  Int,
    niche:               Niche[mgo.algorithm.noisyprofile.Individual, Int],
    operatorExploration: Double,
    genomeSize:          Int,
    historySize:         Int,
    cloneProbability:    Double,
    aggregation:         Vector[Double] ⇒ Double
  )

  object StochasticParams {
    import mgo.algorithm._
    import mgo.algorithm.noisyprofile._
    import context._
    import context.implicits._

    implicit def integration = new MGOAPI.Integration[StochasticParams, Vector[Double], Double] with MGOAPI.Stochastic with MGOAPI.Profile[StochasticParams] {
      type M[A] = context.M[A]
      type G = mgo.algorithm.noisyprofile.Genome
      type I = Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def mMonad = implicitly
      def mGeneration = implicitly
      def mStartTime = implicitly

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Double = om.aggregation(vectorFitness.get(individual))
        def buildIndividual(genome: G, phenotype: Double) = noisyprofile.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())
        def initialGenomes(n: Int) = noisyprofile.initialGenomes(n, om.genomeSize)
        def breeding(n: Int) = noisyprofile.breeding(n, om.niche, om.operatorExploration, om.cloneProbability, om.aggregation)
        def elitism = noisyprofile.elitism(om.mu, om.niche, om.historySize, om.aggregation)

        def migrateToIsland(population: Vector[I]) = population.map(_.copy(historyAge = 0))
        def migrateFromIsland(population: Vector[I]) =
          population.filter(_.historyAge != 0).map {
            i ⇒ Individual.fitnessHistory.modify(_.take(math.min(i.historyAge, om.historySize).toInt))(i)
          }
      }

      def run[A](s: S, x: M[A]) = {
        val res =
          for {
            xv ← x
            s ← noisyprofile.state[M]
          } yield (s, xv)
        interpreter(s).run(res).right.get
      }

      def samples(i: I): Long = i.fitnessHistory.size
      def profile(om: StochasticParams)(population: Vector[I]) = noisyprofile.profile(population, om.niche)
    }
  }

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
        type MGOAG = StochasticParams
        def mgoAG = a.algo

        type V = Vector[Double]
        type P = Double

        lazy val integration = implicitly[MGOAPI.Integration[MGOAG, V, P] with MGOAPI.Stochastic with MGOAPI.Profile[MGOAG]]

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
    algo:        StochasticParams,
    genome:      UniqueGenome,
    objective:   Objective,
    replication: Stochastic[Id]
  )

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
      StochasticParams(
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

}
