
package org.openmole.plugin.method.evolution

import cats.implicits._
import monocle.macros.GenLens
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.method.evolution.Genome.{ Suggestion }
import squants.time.Time

import scala.language.higherKinds

object NSGA3 {

  type ReferencePoints = mgo.evolution.algorithm.NSGA3Operations.ReferencePoints

  object DeterministicParams {
    import mgo.evolution.algorithm.{ CDGenome, NSGA3 ⇒ MGONSGA3, _ }

    implicit def integration: MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Array[Any]] = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Array[Any]] {
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual[Array[Any]]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicParams) = new Ops {
        def startTimeLens = GenLens[EvolutionState[Unit]](_.startTime)

        def generationLens = GenLens[EvolutionState[Unit]](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)

        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)

        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Array[Any], context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)

        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = MGONSGA3.result[Array[Any]](population, Genome.continuous(om.genome).from(context), ExactObjective.toFitnessFunction(om.objectives))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome).from(context)
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONSGA3.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONSGA3.adaptiveBreeding[S, Array[Any]](om.operatorExploration, discrete, ExactObjective.toFitnessFunction(om.objectives), rejectValue)(s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          Genome.continuous(om.genome).map { continuous ⇒
            val (s2, elited) = MGONSGA3.elitism[S, Array[Any]](om.mu, om.references, continuous, ExactObjective.toFitnessFunction(om.objectives))(s, population, candidates, rng)
            val s3 = EvolutionState.generation.modify(_ + 1)(s2)
            (s3, elited)
          }

        def migrateToIsland(population: Vector[I]) = DeterministicGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = DeterministicGAIntegration.migrateFromIsland(population)

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)
      }

    }

  }

  case class DeterministicParams(
    mu:                  Int,
    references:          ReferencePoints,
    genome:              Genome,
    objectives:          Seq[ExactObjective[_]],
    operatorExploration: Double,
    reject:              Option[Condition])

  def apply[P](
    genome:     Genome,
    objective:  Objectives,
    mu:         Int                          = 200,
    references: Int                          = 50,
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None
  ): EvolutionWorkflow =
    WorkflowIntegration.stochasticity(objective, stochastic.option) match {
      //case None ⇒ // FIXME
      case _ ⇒
        val exactObjectives = objective.map(o ⇒ Objective.toExact(o))
        val integration: WorkflowIntegration.DeterministicGA[_] = WorkflowIntegration.DeterministicGA(
          DeterministicParams(mu, mgo.evolution.algorithm.NSGA3Operations.ReferencePoints(references, objective.size), genome, exactObjectives, operatorExploration, reject),
          genome,
          exactObjectives
        )(DeterministicParams.integration)

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
      //case Some(stochasticValue) ⇒

    }

}

object NSGA3Evolution {

  import org.openmole.core.dsl.DSL

  def apply(
    genome:       Genome,
    objective:    Objectives,
    evaluation:   DSL,
    termination:  OMTermination,
    mu:           Int                          = 200,
    references:   Int                          = 50,
    stochastic:   OptionalArgument[Stochastic] = None,
    reject:       OptionalArgument[Condition]  = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Suggestion                   = Suggestion.empty,
    scope:        DefinitionScope              = "nsga3") =
    EvolutionPattern.build(
      algorithm =
        NSGA3(
          mu = mu,
          references = references,
          genome = genome,
          objective = objective,
          stochastic = stochastic,
          reject = reject
        ),
      evaluation = evaluation,
      termination = termination,
      parallelism = parallelism,
      distribution = distribution,
      suggestion = suggestion(genome),
      scope = scope
    )

}

