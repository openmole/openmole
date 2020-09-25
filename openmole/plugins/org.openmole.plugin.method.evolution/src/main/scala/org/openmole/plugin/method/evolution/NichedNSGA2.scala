package org.openmole.plugin.method.evolution

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.{ FromContext, Condition }
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.workflow.tools.OptionalArgument
import cats._
import cats.implicits._
import mgo.evolution._
import mgo.evolution.algorithm._
import mgo.evolution.breeding._
import mgo.evolution.elitism._
import mgo.evolution.niche._
import monocle.macros.GenLens
import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.method.evolution.Genome.Suggestion
import org.openmole.plugin.method.evolution.NichedNSGA2.NichedElement
import squants.time.Time

object NichedNSGA2Algorithm {

  import CDGenome._
  import DeterministicIndividual._

  case class Result[N](continuous: Vector[Double], discrete: Vector[Int], fitness: Vector[Double], niche: N)

  def result[N, P](population: Vector[Individual[P]], niche: Individual[P] ⇒ N, continuous: Vector[C], fitness: P ⇒ Vector[Double], keepAll: Boolean) = {
    val individuals = if (keepAll) population else nicheElitism[Individual[P], N](population, keepFirstFront[Individual[P]](_, i ⇒ fitness(i.phenotype)), niche)

    individuals.map { i ⇒
      Result(
        scaleContinuousValues(continuousValues.get(i.genome), continuous),
        Individual.genome composeLens discreteValues get i,
        fitness(i.phenotype),
        niche(i))
    }
  }

  def continuousProfile[P](x: Int, nX: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.continuousProfile[Individual[P]]((Individual.genome[P] composeLens continuousValues).get _, x, nX)

  def discreteProfile[P](x: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.discreteProfile[Individual[P]]((Individual.genome[P] composeLens discreteValues).get _, x)

  def boundedContinuousProfile[P](continuous: Vector[C], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, nX, min, max)

  def gridContinuousProfile[P](continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, intervals)

  def boundedObjectiveProfile[P](x: Int, nX: Int, min: Double, max: Double, fitness: P ⇒ Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i ⇒ fitness(i.phenotype), x, nX, min, max)

  def gridObjectiveProfile[P](x: Int, intervals: Vector[Double], fitness: P ⇒ Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i ⇒ fitness(i.phenotype), x, intervals)

  def initialGenomes(lambda: Int, continuous: Vector[C], discrete: Vector[D], reject: Option[Genome ⇒ Boolean], rng: scala.util.Random) =
    CDGenome.initialGenomes(lambda, continuous, discrete, reject, rng)

  def adaptiveBreeding[S, P](lambda: Int, reject: Option[Genome ⇒ Boolean], operatorExploration: Double, discrete: Vector[D], fitness: P ⇒ Vector[Double]) =
    NSGA2Operations.adaptiveBreeding[S, Individual[P], Genome](
      i ⇒ fitness(i.phenotype),
      Individual.genome.get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      buildGenome,
      logOfPopulationSize,
      lambda,
      reject,
      operatorExploration)

  def expression[P](fitness: (Vector[Double], Vector[Int]) ⇒ P, components: Vector[C]): Genome ⇒ Individual[P] =
    DeterministicIndividual.expression[P](fitness, components)

  def elitism[S, N, P](niche: Niche[Individual[P], N], mu: Int, components: Vector[C], fitness: P ⇒ Vector[Double]) =
    ProfileOperations.elitism[S, Individual[P], N](
      i ⇒ fitness(i.phenotype),
      i ⇒ values(Individual.genome.get(i), components),
      niche,
      mu)

}

object NoisyNichedNSGA2Algorithm {

  import CDGenome._
  import NoisyIndividual._
  import cats.implicits._
  import shapeless._

  def aggregatedFitness[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double]): Individual[P] ⇒ Vector[Double] = NoisyNSGA2.fitness[P](aggregation)
  case class Result[N](continuous: Vector[Double], discrete: Vector[Int], fitness: Vector[Double], niche: N, replications: Int)

  def result[N, P: Manifest](
    population:  Vector[Individual[P]],
    aggregation: Vector[P] ⇒ Vector[Double],
    niche:       Individual[P] ⇒ N,
    continuous:  Vector[C],
    onlyOldest:  Boolean,
    keepAll:     Boolean) = {
    def nicheResult(population: Vector[Individual[P]]) =
      if (population.isEmpty) population
      else if (onlyOldest) {
        val firstFront = keepFirstFront(population, NoisyNSGA2.fitness[P](aggregation))
        val sorted = firstFront.sortBy(-_.phenotypeHistory.size)
        val maxHistory = sorted.head.phenotypeHistory.size
        firstFront.filter(_.phenotypeHistory.size == maxHistory)
      }
      else keepFirstFront(population, NoisyNSGA2.fitness[P](aggregation))

    val individuals = if (keepAll) population else nicheElitism[Individual[P], N](population, nicheResult, niche)

    individuals.map { i ⇒
      val (c, d, f, r) = NoisyIndividual.aggregate[P](i, aggregation, continuous)
      Result(c, d, f, niche(i), r)
    }
  }

  def continuousProfile[P](x: Int, nX: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.continuousProfile[Individual[P]]((Individual.genome[P] composeLens continuousValues).get _, x, nX)

  def discreteProfile[P](x: Int): Niche[Individual[P], Int] =
    mgo.evolution.niche.discreteProfile[Individual[P]]((Individual.genome[P] composeLens discreteValues).get _, x)

  def boundedContinuousProfile[P](continuous: Vector[C], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, nX, min, max)

  def gridContinuousProfile[P](continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](i ⇒ scaleContinuousValues(continuousValues.get(i.genome), continuous), x, intervals)

  def boundedObjectiveProfile[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double], x: Int, nX: Int, min: Double, max: Double): Niche[Individual[P], Int] =
    mgo.evolution.niche.boundedContinuousProfile[Individual[P]](aggregatedFitness[P](aggregation), x, nX, min, max)

  def gridObjectiveProfile[P: Manifest](aggregation: Vector[P] ⇒ Vector[Double], x: Int, intervals: Vector[Double]): Niche[Individual[P], Int] =
    mgo.evolution.niche.gridContinuousProfile[Individual[P]](aggregatedFitness(aggregation), x, intervals)

  def adaptiveBreeding[S, P: Manifest](lambda: Int, reject: Option[Genome ⇒ Boolean], operatorExploration: Double, cloneProbability: Double, aggregation: Vector[P] ⇒ Vector[Double], discrete: Vector[D]) =
    NoisyNSGA2Operations.adaptiveBreeding[S, Individual[P], Genome, P](
      aggregatedFitness(aggregation),
      Individual.genome.get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      buildGenome,
      logOfPopulationSize,
      lambda,
      reject,
      operatorExploration,
      cloneProbability)

  def elitism[S, N, P: Manifest](niche: Niche[Individual[P], N], muByNiche: Int, historySize: Int, aggregation: Vector[P] ⇒ Vector[Double], components: Vector[C]): Elitism[S, Individual[P]] = {
    def individualValues(i: Individual[P]) = values(Individual.genome.get(i), components)

    NoisyProfileOperations.elitism[S, Individual[P], N, P](
      aggregatedFitness(aggregation),
      mergeHistories(individualValues, vectorPhenotype, Individual.historyAge, historySize),
      individualValues,
      niche,
      muByNiche)
  }

  def expression[P: Manifest](fitness: (util.Random, Vector[Double], Vector[Int]) ⇒ P, continuous: Vector[C]): (util.Random, Genome) ⇒ Individual[P] =
    NoisyIndividual.expression(fitness, continuous)

  def initialGenomes(lambda: Int, continuous: Vector[C], discrete: Vector[D], reject: Option[Genome ⇒ Boolean], rng: scala.util.Random) =
    CDGenome.initialGenomes(lambda, continuous, discrete, reject, rng)

}

object NichedNSGA2 {

  object NichedElement {
    implicit def fromValDouble(v: (Val[Double], Int)) = Continuous(v._1, v._2)
    implicit def fromValInt(v: Val[Int]) = Discrete(v)
    implicit def fromValString(v: Val[String]) = Discrete(v)
    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]) = GridContinuous(f.value, fix(f.domain).toVector)

    case class GridContinuous(v: Val[Double], intervals: Vector[Double]) extends NichedElement
    case class Continuous(v: Val[Double], n: Int) extends NichedElement
    case class ContinuousSequence(v: Val[Array[Double]], i: Int, n: Int) extends NichedElement
    case class Discrete(v: Val[_]) extends NichedElement
    case class DiscreteSequence(v: Val[Array[_]], i: Int) extends NichedElement
  }

  sealed trait NichedElement

  object DeterministicParams {

    def niche(genome: Genome, phenotypeContent: PhenotypeContent, objectives: Seq[ExactObjective[_]], profiled: Seq[NichedElement]) = {

      def notFoundInGenome(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

      val niches =
        profiled.toVector.map {
          case c: NichedElement.Continuous ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.continuousProfile[Phenotype](index, c.n)
          case c: NichedElement.ContinuousSequence ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.continuousProfile[Phenotype](index + c.i, c.n)
          case c: NichedElement.Discrete ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.discreteProfile[Phenotype](index)
          case c: NichedElement.DiscreteSequence ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NichedNSGA2Algorithm.discreteProfile[Phenotype](index + c.i)
          case c: NichedElement.GridContinuous ⇒
            (Genome.continuousIndex(genome, c.v), Objectives.index(objectives, c.v)) match {
              case (Some(index), _) ⇒ NichedNSGA2Algorithm.gridContinuousProfile[Phenotype](Genome.continuous(genome), index, c.intervals)
              case (_, Some(index)) ⇒ NichedNSGA2Algorithm.gridObjectiveProfile[Phenotype](index, c.intervals, ExactObjective.toFitnessFunction(phenotypeContent, objectives))
              case _                ⇒ throw new UserBadDataError(s"Variable ${c.v} not found neither in the genome nor in the objectives")
            }
        }

      FromContext.value(mgo.evolution.niche.sequenceNiches[CDGenome.DeterministicIndividual.Individual[Phenotype], Int](niches))
    }

    import CDGenome.DeterministicIndividual

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = DeterministicIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicParams) = new Ops {
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean) = FromContext { p ⇒
          import p._
          val res = NichedNSGA2Algorithm.result(population, om.niche.from(context), Genome.continuous(om.genome), ExactObjective.toFitnessFunction(om.phenotypeContent, om.objectives), keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))

          genomes ++ fitness
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          mgo.evolution.algorithm.Profile.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(population: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          mgo.evolution.algorithm.Profile.adaptiveBreeding[Phenotype](n, om.operatorExploration, discrete, ExactObjective.toFitnessFunction(om.phenotypeContent, om.objectives), rejectValue) apply (s, population, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val (s2, elited) = NichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](om.niche.from(context), om.nicheSize, Genome.continuous(om.genome), ExactObjective.toFitnessFunction(om.phenotypeContent, om.objectives)) apply (s, population, candidates, rng)
          val s3 = EvolutionState.generation.modify(_ + 1)(s2)
          (s3, elited)
        }

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)

        def migrateToIsland(population: Vector[I]) = DeterministicGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = DeterministicGAIntegration.migrateFromIsland(population)
      }

    }
  }

  case class DeterministicParams(
    nicheSize:           Int,
    niche:               FromContext[Niche[CDGenome.DeterministicIndividual.Individual[Phenotype], Vector[Int]]],
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[ExactObjective[_]],
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticParams {

    def niche(genome: Genome, phenotypeContent: PhenotypeContent, objectives: Seq[NoisyObjective[_]], profiled: Seq[NichedElement]) = {

      def notFoundInGenome(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

      val niches =
        profiled.toVector.map {
          case c: NichedElement.Continuous ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.continuousProfile[Phenotype](index, c.n)
          case c: NichedElement.ContinuousSequence ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.continuousProfile[Phenotype](index + c.i, c.n)
          case c: NichedElement.Discrete ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.discreteProfile[Phenotype](index)
          case c: NichedElement.DiscreteSequence ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            NoisyNichedNSGA2Algorithm.discreteProfile[Phenotype](index + c.i)
          case c: NichedElement.GridContinuous ⇒
            (Genome.continuousIndex(genome, c.v), Objectives.index(objectives, c.v)) match {
              case (Some(index), _) ⇒ NoisyNichedNSGA2Algorithm.gridContinuousProfile[Phenotype](Genome.continuous(genome), index, c.intervals)
              case (_, Some(index)) ⇒ NoisyNichedNSGA2Algorithm.gridObjectiveProfile[Phenotype](NoisyObjective.aggregate(phenotypeContent, objectives), index, c.intervals)
              case _                ⇒ throw new UserBadDataError(s"Variable ${c.v} not found neither in the genome nor in the objectives")
            }

        }

      FromContext.value(mgo.evolution.niche.sequenceNiches[CDGenome.NoisyIndividual.Individual[Phenotype], Int](niches))
    }

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticParams) = new Ops {
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean) = FromContext { p ⇒
          import p._

          val res = NoisyNichedNSGA2Algorithm.result(population, NoisyObjective.aggregate(om.phenotypeContent, om.objectives), om.niche.from(context), Genome.continuous(om.genome), onlyOldest = true, keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          NoisyNichedNSGA2Algorithm.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          NoisyNichedNSGA2Algorithm.adaptiveBreeding[S, Phenotype](n, rejectValue, om.operatorExploration, om.cloneProbability, NoisyObjective.aggregate(om.phenotypeContent, om.objectives), discrete) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext { p ⇒
            import p._
            val (s2, elited) = NoisyNichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](
              om.niche.from(context),
              om.nicheSize,
              om.historySize,
              NoisyObjective.aggregate(om.phenotypeContent, om.objectives),
              Genome.continuous(om.genome)) apply (s, population, candidates, rng)

            val s3 = EvolutionState.generation.modify(_ + 1)(s2)
            (s3, elited)
          }

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland[I](population, CDGenome.NoisyIndividual.Individual.historyAge)
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland[I, Phenotype](population, CDGenome.NoisyIndividual.Individual.historyAge, CDGenome.NoisyIndividual.Individual.phenotypeHistory)
      }

    }
  }

  case class StochasticParams(
    nicheSize:           Int,
    niche:               FromContext[Niche[mgo.evolution.algorithm.CDGenome.NoisyIndividual.Individual[Phenotype], Vector[Int]]],
    operatorExploration: Double,
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[NoisyObjective[_]],
    historySize:         Int,
    cloneProbability:    Double,
    reject:              Option[Condition])

  def apply[P](
    niche:      Seq[NichedElement],
    genome:     Genome,
    objective:  Objectives,
    nicheSize:  Int,
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None
  ): EvolutionWorkflow =
    WorkflowIntegration.stochasticity(objective, stochastic.option) match {
      case None ⇒
        val exactObjectives = Objectives.toExact(objective)
        val phenotypeContent = PhenotypeContent(exactObjectives)
        val integration: WorkflowIntegration.DeterministicGA[_] =
          new WorkflowIntegration.DeterministicGA(
            DeterministicParams(
              genome = genome,
              objectives = exactObjectives,
              phenotypeContent = phenotypeContent,
              niche = DeterministicParams.niche(genome, phenotypeContent, exactObjectives, niche),
              operatorExploration = operatorExploration,
              nicheSize = nicheSize,
              reject = reject.option),
            genome,
            phenotypeContent
          )

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)

      case Some(stochasticValue) ⇒
        val noisyObjectives = Objectives.toNoisy(objective)
        val phenotypeContent = PhenotypeContent(noisyObjectives)

        val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
          StochasticParams(
            nicheSize = nicheSize,
            niche = StochasticParams.niche(genome, phenotypeContent, noisyObjectives, niche),
            operatorExploration = operatorExploration,
            genome = genome,
            phenotypeContent = phenotypeContent,
            objectives = noisyObjectives,
            historySize = stochasticValue.sample,
            cloneProbability = stochasticValue.reevaluate,
            reject = reject.option),
          genome,
          phenotypeContent,
          stochasticValue
        )

        WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
    }

}

object NichedNSGA2Evolution {

  import org.openmole.core.dsl.DSL

  def apply(
    evaluation:   DSL,
    termination:  OMTermination,
    niche:        Seq[NichedElement],
    genome:       Genome,
    objective:    Objectives,
    nicheSize:    Int,
    stochastic:   OptionalArgument[Stochastic] = None,
    parallelism:  Int                          = 1,
    reject:       OptionalArgument[Condition]  = None,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Suggestion                   = Suggestion.empty,
    scope:        DefinitionScope              = "niched nsga2") =
    EvolutionPattern.build(
      algorithm =
        NichedNSGA2(
          niche = niche,
          genome = genome,
          nicheSize = nicheSize,
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

