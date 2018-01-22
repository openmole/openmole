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
import mgo.algorithm.NoisyPSE.Individual
import mgo.niche._
import monocle.macros._

object NichedNSGA2 {

  object NichedElement {
    case class Continuous(v: Val[Double], n: Int) extends NichedElement
    case class ContinuousSequence(v: Val[Array[Double]], i: Int, n: Int) extends NichedElement
    case class Discrete(v: Val[Int], n: Int) extends NichedElement
    case class DiscreteSequence(v: Val[Array[Int]], i: Int, n: Int) extends NichedElement
  }

  sealed trait NichedElement

  def niche[I](genome: Genome, profiled: Seq[NichedElement], continuousProfile: (Int, Int) ⇒ Niche[I, Int], discreteProfile: Int ⇒ Niche[I, Int]) = {

    def notFound(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

    val niches =
      profiled.toVector.map {
        case c: NichedElement.Continuous         ⇒ Genome.continuousIndex(genome, c.v).getOrElse(notFound(c.v)).map { index ⇒ continuousProfile(index, c.n) }
        case c: NichedElement.ContinuousSequence ⇒ Genome.continuousIndex(genome, c.v).getOrElse(notFound(c.v)).map { index ⇒ continuousProfile(index + c.i, c.n) }
        case c: NichedElement.Discrete           ⇒ Genome.discreteIndex(genome, c.v).getOrElse(notFound(c.v)).map { index ⇒ discreteProfile(index) }
        case c: NichedElement.DiscreteSequence   ⇒ Genome.discreteIndex(genome, c.v).getOrElse(notFound(c.v)).map { index ⇒ discreteProfile(index + c.i) }
      }.sequence

    niches.map { ns ⇒ Profile.sequenceNiches[I, Int](ns) }
  }

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

          val res = Profile.result(population, om.niche.from(context), Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)

          genomes ++ fitness
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

        def elitism(population: Vector[I]) = FromContext { p ⇒
          import p._
          interpret { impl ⇒
            import impl._
            def step =
              for {
                elited ← Profile.elitism[DSL, Vector[Int]](om.niche.from(context), 1, Genome.continuous(om.genome).from(context)) apply population
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
    niche:               FromContext[Niche[CDGenome.DeterministicIndividual.Individual, Vector[Int]]],
    genome:              Genome,
    objectives:          Objectives,
    operatorExploration: Double)

  def apply(
    profiled:  Seq[NichedElement],
    genome:    Genome,
    objective: Objective
  ): WorkflowIntegration.DeterministicGA[DeterministicParams] =
    new WorkflowIntegration.DeterministicGA(
      DeterministicParams(
        genome = genome,
        objectives = Seq(objective),
        niche = niche[CDGenome.DeterministicIndividual.Individual](genome, profiled, Profile.continuousProfile, Profile.discreteProfile),
        operatorExploration = operatorExploration
      ),
      genome,
      Seq(objective)
    )

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

          val res = NoisyProfile.result(population, om.aggregation, om.niche.from(context), Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
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
          FromContext { p ⇒
            import p._
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← NoisyProfile.elitism[DSL, Vector[Int]](
                    om.niche.from(context),
                    om.muByNiche,
                    om.historySize,
                    om.aggregation,
                    Genome.continuous(om.genome).from(context)) apply individuals
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
    niche:               FromContext[Niche[mgo.algorithm.CDGenome.NoisyIndividual.Individual, Vector[Int]]],
    operatorExploration: Double,
    genome:              Genome,
    objectives:          Objectives,
    historySize:         Int,
    cloneProbability:    Double,
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double])

  def apply(
    profiled:   Seq[NichedElement],
    genome:     Genome,
    objective:  Objective,
    stochastic: Stochastic[Id],
    nicheSize:  Int                = 20): WorkflowIntegration.StochasticGA[StochasticParams] = {

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
        niche = niche[CDGenome.NoisyIndividual.Individual](genome, profiled, NoisyProfile.continuousProfile, NoisyProfile.discreteProfile),
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

  }

}

object GenomeProfile {

  def apply(
    x:         Val[Double],
    nX:        Int,
    genome:    Genome,
    objective: Objective
  ): WorkflowIntegration.DeterministicGA[NichedNSGA2.DeterministicParams] =
    NichedNSGA2(
      Vector(NichedNSGA2.NichedElement.Continuous(x, nX)),
      genome,
      objective
    )

  def apply(
    x:          Val[Double],
    nX:         Int,
    genome:     Genome,
    objective:  Objective,
    stochastic: Stochastic[Id]
  ): WorkflowIntegration.StochasticGA[NichedNSGA2.StochasticParams] =
    NichedNSGA2(
      Vector(NichedNSGA2.NichedElement.Continuous(x, nX)),
      genome,
      objective,
      stochastic
    )

}
