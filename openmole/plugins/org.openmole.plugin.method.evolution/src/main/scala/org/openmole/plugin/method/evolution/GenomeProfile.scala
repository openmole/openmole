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
import mgo.algorithm.CDGenome
import mgo.niche._
import monocle.macros._

object GenomeProfile {

  def nichePrototype = Val[Int]("niche", GAIntegration.namespace)

  object DeterministicParams {

    import CDGenome.DeterministicIndividual
    import mgo.algorithm._
    import mgo.algorithm.Profile._
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] {
      type G = CDGenome.Genome
      type I = DeterministicIndividual.Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.Profile.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← mgo.algorithm.Profile.state[M]
        } yield (newState, t)
      }

      def operations(om: DeterministicParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def genome(i: I) = CDGenome.DeterministicIndividual.Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = CDGenome.DeterministicIndividual.vectorFitness.get(individual)
        def buildIndividual(genome: G, phenotype: Vector[Double]) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def result(population: Vector[I]) = FromContext { p ⇒
          import p._

          val res = Profile.result(population, om.niche, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete)).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)
          val niches = Variable(nichePrototype.array, res.map(_.niche).toArray)

          genomes ++ fitness ++ Seq(niches)
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(mgo.algorithm.Profile.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(population: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(mgo.algorithm.Profile.adaptiveBreeding[DSL](n, om.operatorExploration, discrete).run(population)).eval
            }
          }

        def elitism(population: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← Profile.elitism[DSL, Int](om.niche, 1, continuous) apply population
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
        }

        def migrateToIsland(population: Vector[I]) = population
        def migrateFromIsland(population: Vector[I]) = population
      }

    }
  }

  case class DeterministicParams(
    niche:               Niche[CDGenome.DeterministicIndividual.Individual, Int],
    genome:              Genome,
    objectives:          Objectives,
    operatorExploration: Double)

  def apply(
    x:         Val[Double],
    nX:        Int,
    genome:    Genome,
    objective: Objective
  ) = {

    val xIndex =
      Genome.vals(genome).indexWhere(_ == x) match {
        case -1 ⇒ throw new UserBadDataError(s"Variable $x not found in the genome")
        case x  ⇒ x
      }

    new WorkflowIntegration.DeterministicGA(
      DeterministicParams(
        genome = genome,
        objectives = Seq(objective),
        niche = Profile.genomeProfile(xIndex, nX),
        operatorExploration = operatorExploration
      ),
      genome,
      Seq(objective)
    )

  }

  object StochasticParams {
    import mgo.algorithm._
    import mgo.algorithm.NoisyProfile
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
        mgo.algorithm.NoisyProfile.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← mgo.algorithm.NoisyProfile.state[M]
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

          val res = NoisyProfile.result(population, om.aggregation, om.niche, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete)).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(NoisyProfile.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(NoisyProfile.adaptiveBreeding[DSL](n, om.operatorExploration, om.cloneProbability, om.aggregation, discrete).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← NoisyProfile.elitism[DSL, Int](
                    om.niche,
                    om.muByNiche,
                    om.historySize,
                    om.aggregation,
                    continuous) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
        }

        def migrateToIsland(population: Vector[I]) = population.map(_.copy(historyAge = 0))
        def migrateFromIsland(population: Vector[I]) =
          population.filter(_.historyAge != 0).map {
            i ⇒ CDGenome.NoisyIndividual.Individual.fitnessHistory.modify(_.take(math.min(i.historyAge, om.historySize).toInt))(i)
          }
      }

    }
  }

  case class StochasticParams(
    muByNiche:           Int,
    niche:               Niche[mgo.algorithm.CDGenome.NoisyIndividual.Individual, Int],
    operatorExploration: Double,
    genome:              Genome,
    objectives:          Objectives,
    historySize:         Int,
    cloneProbability:    Double,
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double])

  def apply(
    x:          Val[Double],
    nX:         Int,
    genome:     Genome,
    objective:  Objective,
    stochastic: Stochastic[Id],
    nicheSize:  Int            = 20
  ) = {

    val xIndex =
      Genome.vals(genome).indexWhere(_ == x) match {
        case -1 ⇒ throw new UserBadDataError(s"Variable $x not found in the genome")
        case x  ⇒ x
      }

    import org.openmole.core.dsl.seqIsFunctor

    val seqStochastic: Stochastic[Seq] =
      Stochastic[Seq](
        seed = stochastic.seed,
        replications = stochastic.replications,
        reevaluate = stochastic.reevaluate,
        aggregation = OptionalArgument(stochastic.aggregation.map { a: FitnessAggregation ⇒ Seq(a) })
      )

    def aggregation(h: Vector[Vector[Double]]) =
      StochasticGAIntegration.aggregateVector(seqStochastic.aggregation, h)

    WorkflowIntegration.StochasticGA(
      StochasticParams(
        muByNiche = nicheSize,
        niche = NoisyProfile.genomeProfile(xIndex, nX),
        operatorExploration = operatorExploration,
        genome = genome,
        objectives = Seq(objective),
        historySize = stochastic.replications,
        cloneProbability = stochastic.reevaluate,
        aggregation = aggregation),
      genome,
      Seq(objective),
      seqStochastic
    )

    //      StochasticGenomeProfile(
    //        StochasticParams(
    //          mu = paretoSize,
    //          niche = StochasticGenomeProfile.niche(xIndex, nX),
    //          operatorExploration = operatorExploration,
    //          genomeSize = UniqueGenome.size(ug),
    //          historySize = stochastic.replications,
    //          cloneProbability = stochastic.reevaluate,
    //          aggregation = aggregation
    //        ),
    //        ug,
    //        objective,
    //        stochastic
    //      )
  }

}
