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

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.tools.OptionalArgument
import cats._
import cats.data._
import cats.implicits._
import mgo.evolution._
import mgo.evolution.algorithm._
import mgo.evolution.breeding._
import mgo.evolution.contexts._
import mgo.evolution.elitism._
import mgo.evolution.niche._
import mgo.tagtools._
import monocle.macros.GenLens
import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.composition.DSLContainer
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.method.evolution.NichedNSGA2.NichedElement
import squants.time.Time

import scala.language.higherKinds

object NSGA2 {

  object DeterministicParams {
    import mgo.evolution.algorithm.{ NSGA2 ⇒ MGONSGA2, _ }
    import mgo.evolution.algorithm.CDGenome
    import cats.data._
    import mgo.evolution.contexts._

    implicit def integration: MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] {
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.evolution.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        MGONSGA2.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← mgo.evolution.algorithm.NSGA2.state[M]
        } yield (newState, t)
      }

      def operations(om: DeterministicParams) = new Ops {

        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Vector[Double], context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = MGONSGA2.result(population, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
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

        def elitism(population: Vector[I], candidates: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGONSGA2.elitism[DSL](om.mu, continuous) apply (population, candidates)
                  _ ← mgo.evolution.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def migrateToIsland(population: Vector[I]) = DeterministicGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = population

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.evolution.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.evolution.afterDuration[DSL, I](d).run(population)).eval
        }
      }

    }

  }

  case class DeterministicParams(
    mu:                  Int,
    genome:              Genome,
    objectives:          Seq[ExactObjective[_]],
    operatorExploration: Double)

  object StochasticParams {
    import mgo.evolution.algorithm.{ NoisyNSGA2 ⇒ MGONoisyNSGA2, _ }
    import mgo.evolution.algorithm.CDGenome
    import cats.data._
    import mgo.evolution.contexts._

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Array[Any]] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Array[Any]]
      type S = EvolutionState[Unit]

      def iManifest = implicitly[Manifest[I]]
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.evolution.contexts.run.Implicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒ MGONoisyNSGA2.run(s)(f) }

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
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Array[Any], context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[Unit](random = rng, s = ())

        def aggregate(v: Vector[Array[Any]]): Vector[Double] =
          for {
            (vs, obj) ← v.transpose zip om.objectives
          } yield NoisyObjective.aggregateAny(obj, vs)

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = MGONoisyNSGA2.result(population, aggregate(_), Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
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
              zipWithState(MGONoisyNSGA2.adaptiveBreeding[DSL, Array[Any]](n, om.operatorExploration, om.cloneProbability, aggregate, discrete).run(individuals)).eval
            }
          }

        def elitism(population: Vector[I], candidates: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← MGONoisyNSGA2.elitism[DSL, Array[Any]](om.mu, om.historySize, aggregate, continuous) apply (population, candidates)
                  _ ← mgo.evolution.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.evolution.afterGeneration[DSL, I](g).run(population)).eval
        }

        def afterDuration(d: Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
          import impl._
          zipWithState(mgo.evolution.afterDuration[DSL, I](d).run(population)).eval
        }

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland[I](population, CDGenome.NoisyIndividual.Individual.historyAge)
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland[I, Array[Any]](population, CDGenome.NoisyIndividual.Individual.historyAge, CDGenome.NoisyIndividual.Individual.fitnessHistory)
      }

    }
  }

  case class StochasticParams(
    mu:                  Int,
    operatorExploration: Double,
    genome:              Genome,
    objectives:          Seq[NoisyObjective[_]],
    historySize:         Int,
    cloneProbability:    Double
  )

  def apply[P](
    genome:     Genome,
    objectives: Objectives,
    mu:         Int                          = 200,
    stochastic: OptionalArgument[Stochastic] = None
  ): EvolutionWorkflow =
    WorkflowIntegration.stochasticity(objectives, stochastic.option) match {
      case None ⇒
        val exactObjectives = objectives.map(o ⇒ Objective.toExact(o))
        val integration: WorkflowIntegration.DeterministicGA[_] = WorkflowIntegration.DeterministicGA(
          DeterministicParams(mu, genome, exactObjectives, operatorExploration),
          genome,
          exactObjectives
        )(DeterministicParams.integration)

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
      case Some(stochasticValue) ⇒
        val noisyObjectives = objectives.map(o ⇒ Objective.toNoisy(o))

        val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
          StochasticParams(mu, operatorExploration, genome, noisyObjectives, stochasticValue.replications, stochasticValue.reevaluate),
          genome,
          noisyObjectives,
          stochasticValue
        )(StochasticParams.integration)

        WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
    }

}

object NSGA2Evolution {

  import org.openmole.core.dsl.DSL

  def apply(
    genome:       Genome,
    objectives:   Objectives,
    evaluation:   DSL,
    termination:  OMTermination,
    mu:           Int                          = 200,
    stochastic:   OptionalArgument[Stochastic] = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Seq[Seq[ValueAssignment[_]]] = Seq(),
    scope:        DefinitionScope              = "nsga2") =
    EvolutionPattern.build(
      algorithm =
        NSGA2(
          mu = mu,
          genome = genome,
          objectives = objectives,
          stochastic = stochastic
        ),
      evaluation = evaluation,
      termination = termination,
      stochastic = stochastic,
      parallelism = parallelism,
      distribution = distribution,
      suggestion = suggestion,
      scope = scope
    )

}

