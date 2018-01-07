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
import org.openmole.core.expansion.FromContext
import squants.Time
import cats.implicits._
import org.openmole.core.context.Variable

object NSGA2 {

  object DeterministicParams {
    import mgo.algorithm.{ NSGA2 ⇒ MGONSGA2, _ }
    import mgo.algorithm.CDGenome
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration: MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] {
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        MGONSGA2.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← mgo.algorithm.NSGA2.state[M]
        } yield (newState, t)
      }

      def operations(om: DeterministicParams) = new Ops {

        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: EvolutionState[Unit]) = s.generation
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def genome(i: I) = CDGenome.DeterministicIndividual.Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = CDGenome.DeterministicIndividual.vectorFitness.get(individual)
        def buildIndividual(genome: G, phenotype: Vector[Double]) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def result(population: Vector[I]) = FromContext { p ⇒
          import p._

          val res = MGONSGA2.result(population, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete)).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(
                MGONSGA2.initialGenomes[DSL](n, continuous, discrete)
              ).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(MGONSGA2.adaptiveBreeding[DSL](n, om.operatorExploration, discrete).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGONSGA2.elitism[DSL](om.mu, continuous) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def migrateToIsland(population: Vector[I]) = population
        def migrateFromIsland(population: Vector[I]) = population

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
        }
      }

    }

  }

  case class DeterministicParams(
    mu:                  Int,
    genome:              Genome,
    objectives:          Objectives,
    operatorExploration: Double)

  def apply(
    mu:         Int,
    genome:     Genome,
    objectives: Objectives
  ) = {
    new WorkflowIntegration.DeterministicGA(
      DeterministicParams(mu, genome, objectives, operatorExploration),
      genome,
      objectives
    )
  }

  object StochasticParams {
    import mgo.algorithm.{ NoisyNSGA2 ⇒ MGONoisyNSGA2, _ }
    import mgo.algorithm.CDGenome
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Vector[Double]] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        MGONoisyNSGA2.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← MGONoisyNSGA2.state[M]
        } yield (newState, t)
      }

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def genome(i: I) = CDGenome.NoisyIndividual.Individual.genome.get(i)

        def phenotype(individual: I): Vector[Double] = om.aggregation(CDGenome.NoisyIndividual.vectorFitness.get(individual))
        def buildIndividual(genome: G, phenotype: Vector[Double]) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def result(population: Vector[I]) = FromContext { p ⇒
          import p._

          val res = MGONoisyNSGA2.result(population, om.aggregation, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete)).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(MGONoisyNSGA2.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(MGONoisyNSGA2.adaptiveBreeding[DSL](n, om.operatorExploration, om.cloneProbability, om.aggregation, discrete).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGONoisyNSGA2.elitism[DSL](om.mu, om.historySize, om.aggregation, continuous) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
        }

        def migrateToIsland(population: Vector[I]) = population.map(_.copy(historyAge = 0))
        def migrateFromIsland(population: Vector[I]) =
          population.filter(_.historyAge != 0).map {
            i ⇒ CDGenome.NoisyIndividual.Individual.fitnessHistory.modify(_.take(scala.math.min(i.historyAge, om.historySize).toInt))(i)
          }
      }

    }
  }

  case class StochasticParams(
    mu:                  Int,
    operatorExploration: Double,
    genome:              Genome,
    objectives:          Objectives,
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
    def aggregation(h: Vector[Vector[Double]]) =
      StochasticGAIntegration.aggregateVector(stochastic.aggregation, h)

    WorkflowIntegration.StochasticGA(
      StochasticParams(mu, operatorExploration, genome, objectives, stochastic.replications, stochastic.reevaluate, aggregation),
      genome,
      objectives,
      stochastic
    )
  }

}

