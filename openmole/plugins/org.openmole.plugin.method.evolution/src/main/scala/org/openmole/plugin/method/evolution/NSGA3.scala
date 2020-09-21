
package org.openmole.plugin.method.evolution

import cats.implicits._
import monocle.macros.GenLens
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.method.evolution.Genome.{ Suggestion }
import squants.time.Time

import scala.language.higherKinds

object NSGA3 {

  import mgo.evolution.algorithm.NSGA3Operations.ReferencePoints

  object References {
    case object None extends References
    case class Division(division: Int) extends References
    case class List(points: Vector[Vector[Double]]) extends References

    implicit def fromInt(i: Int) = Division(i)
    implicit def fromVector(p: Vector[Vector[Double]]) = List(p)
  }

  sealed trait References

  object DeterministicParams {
    import mgo.evolution.algorithm.{ CDGenome, NSGA3 ⇒ MGONSGA3, _ }

    implicit def integration: MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Phenotype] = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual[Phenotype]
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
        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)

        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean) = FromContext { p ⇒
          import p._

          val res = MGONSGA3.result[Phenotype](population, Genome.continuous(om.genome).from(context), ExactObjective.toFitnessFunction(om.objectives), keepAll = keepAll)
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
          MGONSGA3.adaptiveBreeding[S, Phenotype](om.operatorExploration, discrete, ExactObjective.toFitnessFunction(om.objectives), rejectValue, lambda = n)(s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          Genome.continuous(om.genome).map { continuous ⇒
            val (s2, elited) = MGONSGA3.elitism[S, Phenotype](om.mu, om.references, continuous, ExactObjective.toFitnessFunction(om.objectives))(s, population, candidates, rng)
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

  object StochasticParams {
    import mgo.evolution.algorithm.{ CDGenome, NoisyNSGA3 ⇒ MGONoisyNSGA3, _ }

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly[Manifest[I]]
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticParams) = new Ops {

        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[Unit](s = ())

        def aggregate(v: Vector[Phenotype]): Vector[Double] = NoisyObjective.aggregate(om.objectives)(v)

        def result(population: Vector[I], state: S, keepAll: Boolean) = FromContext { p ⇒
          import p._

          val res = MGONoisyNSGA3.result(population, aggregate(_), Genome.continuous(om.genome).from(context), keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness)).from(context)

          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome).from(context)
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONoisyNSGA3.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONoisyNSGA3.adaptiveBreeding[S, Phenotype](om.operatorExploration, om.cloneProbability, discrete, aggregate, rejectValue, lambda = n) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: util.Random) =
          Genome.continuous(om.genome).map { continuous ⇒
            println(continuous)
            val (s2, elited) = MGONoisyNSGA3.elitism[S, Phenotype](om.mu, om.references, om.historySize, aggregate, continuous) apply (s, population, candidates, rng)
            val s3 = EvolutionState.generation.modify(_ + 1)(s2)
            (s3, elited)
          }

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland[I](population, CDGenome.NoisyIndividual.Individual.historyAge)
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland[I, Phenotype](population, CDGenome.NoisyIndividual.Individual.historyAge, CDGenome.NoisyIndividual.Individual.phenotypeHistory[Phenotype])

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)
      }

    }
  }

  case class StochasticParams(
    mu:                  Int,
    references:          ReferencePoints,
    operatorExploration: Double,
    genome:              Genome,
    objectives:          Seq[NoisyObjective[_]],
    historySize:         Int,
    cloneProbability:    Double,
    reject:              Option[Condition]
  )

  def apply[P](
    genome:     Genome,
    objective:  Objectives,
    references: ReferencePoints,
    mu:         Int                          = 200,
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None
  ): EvolutionWorkflow =
    WorkflowIntegration.stochasticity(objective, stochastic.option) match {
      case None ⇒
        val exactObjectives = objective.map(o ⇒ Objective.toExact(o))
        val integration: WorkflowIntegration.DeterministicGA[_] = WorkflowIntegration.DeterministicGA(
          DeterministicParams(mu, references, genome, exactObjectives, operatorExploration, reject),
          genome,
          exactObjectives
        )(DeterministicParams.integration)

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
      case Some(stochasticValue) ⇒
        val noisyObjectives = objective.map(o ⇒ Objective.toNoisy(o))

        val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
          StochasticParams(mu, references, operatorExploration, genome, noisyObjectives, stochasticValue.sample, stochasticValue.reevaluate, reject.option),
          genome,
          noisyObjectives,
          stochasticValue
        )(StochasticParams.integration)

        WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
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
    references:   NSGA3.References             = NSGA3.References.None,
    stochastic:   OptionalArgument[Stochastic] = None,
    reject:       OptionalArgument[Condition]  = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Suggestion                   = Suggestion.empty,
    scope:        DefinitionScope              = "nsga3") = {

    val refPoints =
      references match {
        case NSGA3.References.None        ⇒ mgo.evolution.algorithm.NSGA3Operations.ReferencePoints(50, objective.size)
        case NSGA3.References.Division(i) ⇒ mgo.evolution.algorithm.NSGA3Operations.ReferencePoints(i, objective.size)
        case NSGA3.References.List(p)     ⇒ mgo.evolution.algorithm.NSGA3Operations.ReferencePoints(p)
      }
    EvolutionPattern.build(
      algorithm =
        NSGA3(
          mu = mu,
          references = refPoints,
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

}

