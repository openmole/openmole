
package org.openmole.plugin.method.evolution

import cats.implicits._
import monocle.macros.GenLens
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.method.evolution.Genome.{ Suggestion }
import squants.time.Time

import scala.language.higherKinds

import monocle._
import monocle.syntax.all._

object NSGA3 {

  import mgo.evolution.algorithm.NSGA3Operations.ReferencePoints

  object References {
    case object None extends References
    case class Division(division: Int) extends References
    case class List(points: Vector[Vector[Double]]) extends References

    implicit def fromInt(i: Int): Division = Division(i)
    implicit def fromVector(p: Vector[Vector[Double]]): List = List(p)
  }

  sealed trait References

  object DeterministicNSGA3 {
    import mgo.evolution.algorithm.{ CDGenome, NSGA3 ⇒ MGONSGA3, _ }

    implicit def integration: MGOAPI.Integration[DeterministicNSGA3, (Vector[Double], Vector[Int]), Phenotype] = new MGOAPI.Integration[DeterministicNSGA3, (Vector[Double], Vector[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicNSGA3) = new Ops {
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[_]]] = {
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)
        }

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)

        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
          import p._
          val res = MGONSGA3.result[Phenotype](population, Genome.continuous(om.genome), Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))

          val outputsValues = if (includeOutputs) DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

          genomes ++ fitness ++ outputsValues
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONSGA3.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONSGA3.adaptiveBreeding[S, Phenotype](om.operatorExploration, discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), rejectValue, lambda = n)(s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, evaluated: Long, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val (s2, elited) = MGONSGA3.elitism[S, Phenotype](om.mu, om.references, Genome.continuous(om.genome), Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context))(s, population, candidates, rng)
          val s3 = Focus[S](_.generation).modify(_ + 1)(s2)
          val s4 = Focus[S](_.evaluated).modify(_ + evaluated)(s3)
          (s4, elited)
        }

        def migrateToIsland(population: Vector[I]) = DeterministicGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = DeterministicGAIntegration.migrateFromIsland(population)

        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)
      }

    }

  }

  case class DeterministicNSGA3(
    mu:                  Int,
    references:          ReferencePoints,
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticNSGA3 {
    import mgo.evolution.algorithm.{ CDGenome, NoisyNSGA3 ⇒ MGONoisyNSGA3, _ }

    implicit def integration: MGOAPI.Integration[StochasticNSGA3, (Vector[Double], Vector[Int]), Phenotype] = new MGOAPI.Integration[StochasticNSGA3, (Vector[Double], Vector[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly[Manifest[I]]
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticNSGA3) = new Ops {
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[_]]] = {
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)
        }

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[Unit](s = ())

        def aggregate = Objective.aggregate(om.phenotypeContent, om.objectives)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
          import p._

          val res = MGONoisyNSGA3.result(population, aggregate.from(context), Genome.continuous(om.genome), keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))

          val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)

          val outputValues = if (includeOutputs) StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory)) else Seq()

          genomes ++ fitness ++ Seq(samples) ++ outputValues
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._

          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONoisyNSGA3.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: util.Random) = FromContext { p ⇒
          import p._

          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGONoisyNSGA3.adaptiveBreeding[S, Phenotype](om.operatorExploration, om.cloneProbability, discrete, aggregate.from(context), rejectValue, lambda = n) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, evaluated: Long, rng: util.Random) = FromContext { p ⇒
          import p._

          val (s2, elited) = MGONoisyNSGA3.elitism[S, Phenotype](om.mu, om.references, om.historySize, aggregate.from(context), Genome.continuous(om.genome)) apply (s, population, candidates, rng)
          val s3 = Focus[S](_.generation).modify(_ + 1)(s2)
          val s4 = Focus[S](_.evaluated).modify(_ + evaluated)(s3)
          (s4, elited)
        }

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland[I](population, Focus[I](_.historyAge))
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland[I, Phenotype](population, Focus[I](_.historyAge), Focus[I](_.phenotypeHistory))

        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)
      }

    }
  }

  case class StochasticNSGA3(
    mu:                  Int,
    references:          ReferencePoints,
    operatorExploration: Double,
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    historySize:         Int,
    cloneProbability:    Double,
    reject:              Option[Condition]
  )

  def apply[P](
    genome:         Genome,
    objective:      Objectives,
    references:     ReferencePoints,
    outputs:        Seq[Val[_]]                  = Seq(),
    populationSize: Int                          = 200,
    stochastic:     OptionalArgument[Stochastic] = None,
    reject:         OptionalArgument[Condition]  = None
  ): EvolutionWorkflow =
    EvolutionWorkflow.stochasticity(objective, stochastic.option) match {
      case None ⇒
        val exactObjectives = Objectives.toExact(objective)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicNSGA3(populationSize, references, genome, phenotypeContent, exactObjectives, EvolutionWorkflow.operatorExploration, reject),
          genome,
          phenotypeContent,
          validate = Objectives.validate(exactObjectives, outputs)
        )
      case Some(stochasticValue) ⇒
        val noisyObjectives = Objectives.toNoisy(objective)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)

        def validation: Validate = {
          val aOutputs = outputs.map(_.toArray)
          Objectives.validate(noisyObjectives, aOutputs)
        }

        EvolutionWorkflow.stochasticGAIntegration(
          StochasticNSGA3(populationSize, references, EvolutionWorkflow.operatorExploration, genome, phenotypeContent, noisyObjectives, stochasticValue.sample, stochasticValue.reevaluate, reject.option),
          genome,
          phenotypeContent,
          stochasticValue,
          validate = validation
        )
    }

}

import monocle.macros._
import EvolutionWorkflow._

object NSGA3Evolution {

  import org.openmole.core.dsl.DSL

  given EvolutionMethod[NSGA3Evolution] =
    p =>
      val refPoints =
        p.references match
          case NSGA3.References.None ⇒ mgo.evolution.algorithm.NSGA3Operations.ReferencePoints(50, Objectives.toSeq(p.objective).size)
          case NSGA3.References.Division(i) ⇒ mgo.evolution.algorithm.NSGA3Operations.ReferencePoints(i, Objectives.toSeq(p.objective).size)
          case NSGA3.References.List(p) ⇒ mgo.evolution.algorithm.NSGA3Operations.ReferencePoints(p)

      NSGA3(
        populationSize = p.populationSize,
        references = refPoints,
        genome = p.genome,
        outputs = p.evaluation.outputs,
        objective = p.objective,
        stochastic = p.stochastic,
        reject = p.reject
      )

  given ExplorationMethod[NSGA3Evolution, EvolutionWorkflow] =
    p ⇒
      EvolutionWorkflow(
        method = p,
        evaluation = p.evaluation,
        termination = p.termination,
        parallelism = p.parallelism,
        distribution = p.distribution,
        suggestion = p.suggestion(p.genome),
        scope = p.scope
      )


  given ExplorationMethodSetter[NSGA3Evolution, EvolutionPattern] = (e, p) ⇒ e.copy(distribution = p)

}

case class NSGA3Evolution(
  genome:         Genome,
  objective:      Objectives,
  evaluation:     DSL,
  termination:    OMTermination,
  populationSize: Int                          = 200,
  references:     NSGA3.References             = NSGA3.References.None,
  stochastic:     OptionalArgument[Stochastic] = None,
  reject:         OptionalArgument[Condition]  = None,
  parallelism:    Int                          = EvolutionWorkflow.parallelism,
  distribution:   EvolutionPattern             = SteadyState(),
  suggestion:     Suggestion                   = Suggestion.empty,
  scope:          DefinitionScope              = "nsga3")