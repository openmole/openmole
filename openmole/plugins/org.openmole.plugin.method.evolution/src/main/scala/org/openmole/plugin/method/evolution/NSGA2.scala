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

import monocle.macros._

object NSGA2 {

  object DeterministicParams {
    import mgo.algorithm._
    import mgo.algorithm.nsga2._
    import context._
    import context.implicits._

    implicit def integration: MGOAPI.Integration[DeterministicParams, Vector[Double], Vector[Double]] = new MGOAPI.Integration[DeterministicParams, Vector[Double], Vector[Double]] {
      type M[T] = context.M[T]
      type G = mgo.algorithm.nsga2.Genome
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
        def generation(s: EvolutionState[Unit]) = s.generation
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = vectorFitness.get(individual)
        def buildIndividual(genome: G, phenotype: Vector[Double]) = nsga2.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())
        def initialGenomes(n: Int) = nsga2.initialGenomes(n, om.genomeSize)
        def breeding(n: Int) = nsga2.breeding(n, om.operatorExploration)
        def elitism = nsga2.elitism(om.mu)
        def migrateToIsland(population: Vector[I]) = population
        def migrateFromIsland(population: Vector[I]) = population
      }

      def run[A](s: S, x: M[A]) = {
        val res =
          for {
            xv ← x
            s ← nsga2.state[M]
          } yield (s, xv)
        interpreter(s).run(res).right.get
      }

    }

  }

  case class DeterministicParams(mu: Int, genomeSize: Int, operatorExploration: Double)

  def apply(
    mu:         Int,
    genome:     Genome,
    objectives: Objectives
  ) = {
    val ug = UniqueGenome(genome)

    new WorkflowIntegration.DeterministicGA(
      DeterministicParams(mu, UniqueGenome.size(ug), operatorExploration),
      ug,
      objectives
    )
  }

  object StochasticParams {
    import mgo.algorithm._
    import mgo.algorithm.noisynsga2._
    import context._
    import context.implicits._

    implicit def integration = new MGOAPI.Integration[StochasticParams, Vector[Double], Vector[Double]] with MGOAPI.Stochastic {
      type M[A] = context.M[A]
      type G = mgo.algorithm.noisynsga2.Genome
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
        def phenotype(individual: I): Vector[Double] = om.aggregation(vectorFitness.get(individual))
        def buildIndividual(genome: G, phenotype: Vector[Double]) = noisynsga2.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())
        def initialGenomes(n: Int) = noisynsga2.initialGenomes(n, om.genomeSize)
        def breeding(n: Int) = noisynsga2.breeding(n, om.operatorExploration, om.cloneProbability, om.aggregation)
        def elitism = noisynsga2.elitism(om.mu, om.historySize, om.aggregation)

        def migrateToIsland(population: Vector[I]) = population.map(_.copy(historyAge = 0))
        def migrateFromIsland(population: Vector[I]) =
          population.filter(_.historyAge != 0).map {
            i ⇒ Individual.fitnessHistory.modify(_.take(scala.math.min(i.historyAge, om.historySize).toInt))(i)
          }
      }

      def run[A](s: S, x: M[A]) = {
        val res =
          for {
            xv ← x
            s ← noisynsga2.state[M]
          } yield (s, xv)
        interpreter(s).run(res).right.get
      }

      def samples(i: I): Long = i.fitnessHistory.size
    }
  }

  case class StochasticParams(
    mu:                  Int,
    operatorExploration: Double,
    genomeSize:          Int,
    historySize:         Int,
    cloneProbability:    Double,
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double]
  )

  def apply(
    mu:         Int,
    genome:     Genome,
    objectives: Objectives,
    stochastic: Stochastic[Seq]
  ) = {
    val ug = UniqueGenome(genome)

    def aggregation(h: Vector[Vector[Double]]) = StochasticGAIntegration.aggregateVector(stochastic.aggregation, h)

    WorkflowIntegration.StochasticGA(
      StochasticParams(mu, operatorExploration, UniqueGenome.size(ug), stochastic.replications, stochastic.reevaluate, aggregation),
      ug,
      objectives,
      stochastic
    )
  }

}

