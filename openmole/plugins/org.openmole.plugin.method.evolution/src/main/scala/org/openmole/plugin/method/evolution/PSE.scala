/*
 * Copyright (C) 2015 Romain Reuillon
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

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.tool.random._
import monocle.macros._
import cats.implicits._
import org.openmole.core.expansion.FromContext

object PSE {

  object PatternAxe {

    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector)

    implicit def fromIntDomainToPatternAxe[D](f: Factor[D, Int])(implicit fix: Fix[D, Int]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector.map(_.toDouble))

    implicit def fromLongDomainToPatternAxe[D](f: Factor[D, Long])(implicit fix: Fix[D, Long]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector.map(_.toDouble))

  }

  case class PatternAxe(p: Objective, scale: Vector[Double])

  case class DeterministicParams(
    pattern:             Vector[Double] ⇒ Vector[Int],
    genome:              Genome,
    objectives:          Objectives,
    operatorExploration: Double
  )

  object DeterministicParams {

    import mgo.algorithm.{ PSE ⇒ MGOPSE, _ }
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] { api ⇒
      type G = CDGenome.Genome
      type I = MGOPSE.Individual
      type S = EvolutionState[MGOPSE.HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: MGOPSE.PSEImplicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        MGOPSE.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: MGOPSE.HitMapM, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← MGOPSE.state[M]
        } yield (newState, t)
      }

      def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
      }

      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
      }

      def operations(om: DeterministicParams) = new Ops {

        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildIndividual(genome: G, phenotype: Vector[Double]) = MGOPSE.buildIndividual(genome, phenotype)

        def initialState(rng: util.Random) = EvolutionState[MGOPSE.HitMapState](random = rng, s = Map())

        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)

        def result(population: Vector[I]) = FromContext { p ⇒
          import p._

          val res = MGOPSE.result(population, Genome.continuous(om.genome).from(context), om.pattern)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              implicitly[Generation[DSL]]

              zipWithState(
                MGOPSE.initialGenomes[DSL](n, continuous, discrete)
              ).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(
                MGOPSE.adaptiveBreeding[DSL](
                  n,
                  om.operatorExploration,
                  discrete,
                  om.pattern).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGOPSE.elitism[DSL](om.pattern, continuous) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def migrateToIsland(population: Vector[I]) =
          population.map(MGOPSE.Individual.foundedIsland.set(true))
        def migrateFromIsland(population: Vector[I]) =
          population.filter(i ⇒ !MGOPSE.Individual.foundedIsland.get(i)).
            map(MGOPSE.Individual.mapped.set(false)).
            map(MGOPSE.Individual.foundedIsland.set(false))
      }

    }
  }

  def apply(
    genome:     Genome,
    objectives: Seq[PatternAxe]
  ) = {

    WorkflowIntegration.DeterministicGA(
      DeterministicParams(
        mgo.algorithm.PSE.irregularGrid(objectives.map(_.scale).toVector),
        genome,
        objectives.map(_.p),
        operatorExploration),
      genome,
      objectives.map(_.p)
    )
  }

  case class StochasticParams(
    pattern:             Vector[Double] ⇒ Vector[Int],
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double],
    genome:              Genome,
    objectives:          Objectives,
    historySize:         Int,
    cloneProbability:    Double,
    operatorExploration: Double
  )

  object StochasticParams {

    import mgo.algorithm._
    import mgo.algorithm.{ PSE ⇒ MGOPSE, NoisyPSE ⇒ MGONoisyPSE, _ }
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Vector[Double]] { api ⇒
      type G = CDGenome.Genome
      type I = NoisyPSE.Individual
      type S = EvolutionState[MGOPSE.HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: MGOPSE.PSEImplicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.NoisyPSE.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: MGOPSE.HitMapM, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← MGONoisyPSE.state[M]
        } yield (newState, t)
      }

      def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
      }

      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
      }

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)

        def generation(s: S) = s.generation
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)

        def buildIndividual(genome: G, phenotype: Vector[Double]) = MGONoisyPSE.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[MGOPSE.HitMapState](random = rng, s = Map())

        def result(population: Vector[I]) = FromContext { p ⇒
          import p._
          import org.openmole.core.context._

          val res = MGONoisyPSE.result(population, om.aggregation, om.pattern, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype)).from(context)
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(MGONoisyPSE.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(MGONoisyPSE.adaptiveBreeding[DSL](
                n,
                om.operatorExploration,
                om.cloneProbability,
                om.aggregation,
                discrete,
                om.pattern).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGONoisyPSE.elitism[DSL](
                    om.pattern,
                    om.aggregation,
                    om.historySize,
                    continuous) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)

        def migrateToIsland(population: Vector[I]) =
          population.map(MGONoisyPSE.Individual.foundedIsland.set(true)).map(MGONoisyPSE.Individual.historyAge.set(0))

        def migrateFromIsland(population: Vector[I]) =
          population.filter(i ⇒ !MGONoisyPSE.Individual.foundedIsland.get(i)).
            map(MGONoisyPSE.Individual.mapped.set(false)).
            map(MGONoisyPSE.Individual.foundedIsland.set(false))

      }

    }
  }

  def apply(
    genome:     Genome,
    objectives: Seq[PatternAxe],
    stochastic: Stochastic[Seq]
  ) = {

    WorkflowIntegration.StochasticGA(
      StochasticParams(
        pattern = mgo.algorithm.PSE.irregularGrid(objectives.map(_.scale).toVector),
        aggregation = StochasticGAIntegration.aggregateVector(stochastic.aggregation, _),
        genome = genome,
        objectives = objectives.map(_.p),
        historySize = stochastic.replications,
        cloneProbability = stochastic.reevaluate,
        operatorExploration = operatorExploration
      ),
      genome,
      objectives.map(_.p),
      stochastic
    )
  }

}

