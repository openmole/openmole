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

object NSGA2 {

  object DeterministicParams {
    import mgo.algorithm._
    import mgo.algorithm.nsga2._
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration: MGOAPI.Integration[DeterministicParams, Vector[Double], Vector[Double]] = new MGOAPI.Integration[DeterministicParams, Vector[Double], Vector[Double]] {
      type G = mgo.algorithm.nsga2.Genome
      type I = Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.nsga2.NSGA2(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← nsga2.state[M]
        } yield (newState, t)
      }

      def operations(om: DeterministicParams) = new Ops {

        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: EvolutionState[Unit]) = s.generation
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = vectorFitness.get(individual)
        def buildIndividual(genome: G, phenotype: Vector[Double]) = nsga2.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def initialGenomes(n: Int) = om.genomeSize.map { size ⇒
          interpret { impl ⇒
            import impl._
            zipWithState(nsga2.initialGenomes[DSL](n, size)).eval
          }
        }

        def breeding(individuals: Vector[Individual], n: Int) = interpret { impl ⇒
          import impl._
          zipWithState(nsga2.adaptiveBreeding[DSL](n, om.operatorExploration).run(individuals)).eval
        }

        def elitism(individuals: Vector[Individual]) = interpret { impl ⇒
          import impl._
          zipWithState(nsga2.elitism[DSL](om.mu).run(individuals)).eval
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

  case class DeterministicParams(mu: Int, genomeSize: FromContext[Int], operatorExploration: Double)

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
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[StochasticParams, Vector[Double], Vector[Double]] with MGOAPI.Stochastic {
      type G = mgo.algorithm.noisynsga2.Genome
      type I = Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.noisynsga2.NoisyNSGA2(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← noisynsga2.state[M]
        } yield (newState, t)
      }

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = om.aggregation(vectorFitness.get(individual))
        def buildIndividual(genome: G, phenotype: Vector[Double]) = noisynsga2.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def initialGenomes(n: Int) = om.genomeSize.map { size ⇒
          interpret { impl ⇒
            import impl._
            zipWithState(noisynsga2.initialGenomes[DSL](n, size)).eval
          }
        }

        def breeding(individuals: Vector[Individual], n: Int) = interpret { impl ⇒
          import impl._
          zipWithState(noisynsga2.adaptiveBreeding[DSL](n, om.operatorExploration, om.cloneProbability, om.aggregation).run(individuals)).eval
        }

        def elitism(individuals: Vector[Individual]) = interpret { impl ⇒
          import impl._
          zipWithState(noisynsga2.elitism[DSL](om.mu, om.historySize, om.aggregation).run(individuals)).eval
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
            i ⇒ Individual.fitnessHistory.modify(_.take(scala.math.min(i.historyAge, om.historySize).toInt))(i)
          }
      }

      def samples(i: I): Long = i.fitnessHistory.size
    }
  }

  case class StochasticParams(
    mu:                  Int,
    operatorExploration: Double,
    genomeSize:          FromContext[Int],
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

