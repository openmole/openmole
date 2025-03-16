package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import cats.implicits._
import monocle.macros.GenLens
import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.setter.{ DefinitionScope, ValueAssignment }
import org.openmole.plugin.method.evolution.Genome.{ GenomeBound, Suggestion }
import org.openmole.plugin.method.evolution.Objective.ToObjective
import org.openmole.tool.types.ToDouble
import squants.time.Time

import scala.reflect.ClassTag

import monocle._
import monocle.syntax.all._

object OSE {

  case class DeterministicOSE(
    mu:                  Int,
    origin:              (IArray[Double], IArray[Int]) => Vector[Int],
    limit:               Vector[Double],
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Objectives,
    operatorExploration: Double,
    reject:              Option[Condition])

  object DeterministicOSE {

    import cats.data._
    import mgo.evolution.algorithm.OSE._
    import mgo.evolution.algorithm.{ OSE => MGOOSE, _ }


    implicit def integration: MGOAPI.Integration[DeterministicOSE, (IArray[Double], IArray[Int]), Phenotype] = new MGOAPI.Integration[DeterministicOSE, (IArray[Double], IArray[Int]), Phenotype] { api =>
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual[Phenotype]
      type S = OSEState[Phenotype]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicOSE) = new Ops:
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues(om.genome.continuous).get, CDGenome.discreteValues(om.genome.discrete).get)(genome)

        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(om.genome.discrete)(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState = EvolutionState(s = (Archive.empty, Array.empty))

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p =>
            import p._
            val res = MGOOSE.result[Phenotype](state, population, om.genome.continuous, om.genome.discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)
            val archive = Variable(GAIntegration.archiveVal.array, res.map(_.archive).toArray)


            val outputValues =
              if includeOutputs
              then DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype))
              else Seq()

            genomes ++ fitness ++ Seq(generated, archive) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p =>
            import p._
            val continuous = om.genome.continuous
            val discrete = om.genome.discrete
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
            MGOOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p._
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
            MGOOSE.adaptiveBreeding[Phenotype](
              n,
              om.operatorExploration,
              om.genome.continuous,
              om.genome.discrete,
              om.origin,
              Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context),
              rejectValue) apply (s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p._
            MGOOSE.elitism[Phenotype](om.mu, om.limit, om.origin, om.genome.continuous, om.genome.discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context)) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S =
          def origin(i: I): Vector[Int] = om.origin(i.genome.continuousValues, CDGenome.discreteValues(om.genome.discrete).get(i.genome))
          val archive = (state.s._1 ++ islandState.s._1).sortBy(_.generation).distinctBy(origin)
          val map = (state.s._2 ++ islandState.s._2).distinct
          state.copy(s = (archive, map))

        def migrateToIsland(population: Vector[I], state: S) = (DeterministicGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (DeterministicGAIntegration.migrateFromIsland(population, initialState.generation), state)
      
    }
  }

  case class StochasticOSE(
    mu:                  Int,
    origin:              (IArray[Double], IArray[Int]) => Vector[Int],
    limit:               Vector[Double],
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Objectives,
    historySize:         Int,
    cloneProbability:    Double,
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticOSE {

    import mgo.evolution.algorithm.NoisyOSE._
    import mgo.evolution.algorithm.{ NoisyOSE => MGONoisyOSE, _ }

    implicit def integration: MGOAPI.Integration[StochasticOSE, (IArray[Double], IArray[Int]), Phenotype] = new MGOAPI.Integration[StochasticOSE, (IArray[Double], IArray[Int]), Phenotype] { api =>
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = OSEState[Phenotype]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticOSE) = new Ops:
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)

        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues(om.genome.continuous).get, CDGenome.discreteValues(om.genome.discrete).get)(genome)
        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(om.genome.discrete)(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState = EvolutionState(s = (Archive.empty, Array.empty))

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p =>
            import p.*

            val res = MGONoisyOSE.result(state, population, Objective.aggregate(om.phenotypeContent, om.objectives).from(context), om.genome.continuous, om.genome.discrete, om.limit, keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)
            val archive = Variable(GAIntegration.archiveVal.array, res.map(_.archive).toArray)


            val outputValues =
              if includeOutputs
              then StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory))
              else Seq()

            genomes ++ fitness ++ Seq(samples, generated, archive) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p =>
            import p._

            val continuous = om.genome.continuous
            val discrete = om.genome.discrete
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
            MGONoisyOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p._
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))

            MGONoisyOSE.adaptiveBreeding[Phenotype](
              n,
              om.operatorExploration,
              om.cloneProbability,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              om.genome.continuous,
              om.genome.discrete,
              om.origin,
              om.limit,
              rejectValue) apply (s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p._

            MGONoisyOSE.elitism(
              om.mu,
              om.historySize,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              om.genome.continuous,
              om.genome.discrete,
              om.origin,
              om.limit) apply (s, population, candidates, rng)


        def mergeIslandState(state: S, islandState: S): S =
          def origin(i: I): Vector[Int] = om.origin(i.genome.continuousValues, CDGenome.discreteValues(om.genome.discrete).get(i.genome))
          val archive = (state.s._1 ++ islandState.s._1).sortBy(_.generation).distinctBy(origin)
          val map = (state.s._2 ++ islandState.s._2).distinct
          state.copy(s = (archive, map))

        def migrateToIsland(population: Vector[I], state: S) = (StochasticGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (StochasticGAIntegration.migrateFromIsland(population, initialState.generation), state)

    }
  }

  import org.openmole.core.dsl.*

  object OriginAxe:
    given scalaDouble[D](using fix: FixDomain[D, Double]): Conversion[Factor[D, Double], OriginAxe] = f =>
      val domain = fix(f.domain).domain.toVector
      ScalarDoubleOriginAxe(GenomeBound.ScalarDouble(f.value, domain.min, domain.max), domain)

    given sequenceOfDouble[D](using fix: FixDomain[D, Array[Double]]): Conversion[Factor[D, Array[Double]], OriginAxe] = f =>
      val domain = fix(f.domain).domain
      SequenceOfDoubleOriginAxe(
        GenomeBound.SequenceOfDouble(f.value, domain.map(_.min).toArray, domain.map(_.max).toArray, domain.size),
        domain.toVector.map(_.toVector))

    given continuousInt[D](using fix: FixDomain[D, Double]): Conversion[Factor[D, Int], OriginAxe] = f =>
      val domain = fix(f.domain).domain.toVector
      ContinuousIntOriginAxe(GenomeBound.ContinuousInt(f.value, domain.min.toInt, domain.max.toInt), domain)

    given sequenceOfContinuousInt[D](using fix: FixDomain[D, Array[Double]]): Conversion[Factor[D, Array[Int]], OriginAxe] = f =>
      val domain = fix(f.domain).domain
      SequenceOfContinuousIntOriginAxe(
        GenomeBound.SequenceOfContinuousInt(f.value, domain.map(_.min.toInt).toArray, domain.map(_.max.toInt).toArray, domain.size),
        domain.toVector.map(_.toVector))

    given scalarInt[D](using fix: FixDomain[D, Int]): Conversion[Factor[D, Int], OriginAxe] = f =>
      val domain = fix(f.domain).domain.toVector
      ScalarIntOriginAxe(GenomeBound.ScalarInt(f.value, domain.min, domain.max), domain)

    given sequenceOfInt[D](using fix: FixDomain[D, Int]): Conversion[Factor[Seq[D], Array[Int]], OriginAxe] = f =>
      val domain = f.domain.map(d => fix(d).domain)
      SequenceOfIntOriginAxe(
        GenomeBound.SequenceOfInt(f.value, domain.map(_.min).toArray, domain.map(_.max).toArray, domain.size),
        domain.toVector.map(_.toVector))

    given enumeration[D, T](using fix: FixDomain[D, T]): Conversion[Factor[D, T], OriginAxe] = f =>
      val domain = fix(f.domain).domain.toVector
      EnumerationOriginAxe(GenomeBound.Enumeration(f.value, domain))

    def genomeBound(originAxe: OriginAxe) = originAxe match
      case c: ScalarDoubleOriginAxe          => c.p
      case d: ScalarIntOriginAxe            => d.p
      case d: ContinuousIntOriginAxe => d.p
      case cs: SequenceOfDoubleOriginAxe => cs.p
      case ds: SequenceOfIntOriginAxe   => ds.p
      case s: SequenceOfContinuousIntOriginAxe => s.p
      case en: EnumerationOriginAxe => en.p

    def fullGenome(origin: Seq[OriginAxe], genome: Genome): Genome =
      origin.map(genomeBound) ++ genome

    def toOrigin(origin: Seq[OriginAxe], genome: Genome) =
      val fg = fullGenome(origin, genome)
      def grid(continuous: IArray[Double], discrete: IArray[Int]): Vector[Int] =
        origin.toVector.flatMap:
          case ScalarDoubleOriginAxe(p, scale)         => Vector(mgo.tools.findInterval(scale, Genome.continuousValue(fg, p.v, continuous)))
          case ContinuousIntOriginAxe(p, scale)         => Vector(mgo.tools.findInterval(scale, Genome.continuousValue(fg, p.v, continuous)))
          case ScalarIntOriginAxe(p, scale)           => Vector(mgo.tools.findInterval(scale, Genome.discreteValue(fg, p.v, discrete)))
          case SequenceOfDoubleOriginAxe(p, scale) => mgo.evolution.niche.irregularGrid[Double](scale)(Genome.continuousSequenceValue(fg, p.v, p.size, continuous).toVector)
          case SequenceOfContinuousIntOriginAxe(p, scale) => mgo.evolution.niche.irregularGrid[Double](scale)(Genome.continuousSequenceValue(fg, p.v, p.size, continuous).toVector)
          case SequenceOfIntOriginAxe(p, scale)   => mgo.evolution.niche.irregularGrid[Int](scale)(Genome.discreteSequenceValue(fg, p.v, p.size, discrete).toVector)
          case EnumerationOriginAxe(p) => Vector(Genome.discreteValue(fg, p.v, discrete))

      grid


  sealed trait OriginAxe
  case class ScalarDoubleOriginAxe(p: Genome.GenomeBound.ScalarDouble, scale: Vector[Double]) extends OriginAxe
  case class SequenceOfDoubleOriginAxe(p: Genome.GenomeBound.SequenceOfDouble, scale: Vector[Vector[Double]]) extends OriginAxe
  case class ContinuousIntOriginAxe(p: Genome.GenomeBound.ContinuousInt, scale: Vector[Double]) extends OriginAxe
  case class SequenceOfContinuousIntOriginAxe(p: Genome.GenomeBound.SequenceOfContinuousInt, scale: Vector[Vector[Double]]) extends OriginAxe
  case class ScalarIntOriginAxe(p: Genome.GenomeBound.ScalarInt, scale: Vector[Int]) extends OriginAxe
  case class SequenceOfIntOriginAxe(p: Genome.GenomeBound.SequenceOfInt, scale: Vector[Vector[Int]]) extends OriginAxe
  case class EnumerationOriginAxe(p: Genome.GenomeBound.Enumeration[?]) extends OriginAxe

  object FitnessPattern:
    implicit def fromUnderExactToPattern[T, V](v: Under[T, V])(implicit td: ToDouble[V], te: ToObjective[T]): FitnessPattern = FitnessPattern(te.apply(v.value), td(v.under))
    //    implicit def fromUnderNoisyToPattern[T, V](v: Under[T, V])(implicit td: ToDouble[V], te: ToNoisyObjective[T]) = FitnessPattern(te.apply(v.value), td(v.under))

    //    implicit def fromUnderToObjective[T](v: Under[Val[T], T])(implicit td: ToDouble[T]) = FitnessPattern(v.value, td(v.under))
    //    implicit def fromNegativeUnderToObjective[T](v: Under[Negative[Val[T]], T])(implicit td: ToDouble[T]) = FitnessPattern(v.value, td(v.under))

    //    implicit def fromAggregate[DT: ClassTag, T](v: Under[Aggregate[Val[DT], Array[DT] => Double], T])(implicit td: ToDouble[T]) = FitnessPattern(Objective.aggregateToObjective(v.value), td(v.under))
    //    implicit def fromNegativeAggregate[DT: ClassTag, T](v: Under[Aggregate[Negative[Val[DT]], Array[DT] => Double], T])(implicit td: ToDouble[T]) = FitnessPattern(v.value, td(v.under))

    def toLimit(f: Seq[FitnessPattern]) = f.toVector.map(_.limit)
    def toObjectives(f: Seq[FitnessPattern]) = f.map(_.objective)

  case class FitnessPattern(objective: Objective, limit: Double)

  def apply(
    origin:         Seq[OriginAxe],
    objective:      Seq[FitnessPattern],
    outputs:        Seq[Val[?]]                  = Seq(),
    genome:         Genome                       = Seq(),
    populationSize: Int                          = 200,
    stochastic:     OptionalArgument[Stochastic] = None,
    reject:         OptionalArgument[Condition]  = None): EvolutionWorkflow =
    EvolutionWorkflow.stochasticity(objective.map(_.objective), stochastic.option) match
      case None =>
        val exactObjectives = Objectives.toExact(FitnessPattern.toObjectives(objective))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)
        val fg = OriginAxe.fullGenome(origin, genome)

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicOSE(
            mu = populationSize,
            origin = OriginAxe.toOrigin(origin, genome),
            genome = fg,
            phenotypeContent = phenotypeContent,
            objectives = exactObjectives,
            limit = FitnessPattern.toLimit(objective),
            operatorExploration = EvolutionWorkflow.operatorExploration,
            reject = reject.option),
          fg,
          phenotypeContent,
          validate = Objectives.validate(exactObjectives, outputs)
        )
      case Some(stochasticValue) =>
        val fg = OriginAxe.fullGenome(origin, genome)
        val noisyObjectives = Objectives.toNoisy(FitnessPattern.toObjectives(objective))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)

        def validation: Validate =
          val aOutputs = outputs.map(_.toArray)
          Objectives.validate(noisyObjectives, aOutputs)

        EvolutionWorkflow.stochasticGAIntegration(
          StochasticOSE(
            mu = populationSize,
            origin = OriginAxe.toOrigin(origin, genome),
            genome = fg,
            phenotypeContent = phenotypeContent,
            objectives = noisyObjectives,
            limit = FitnessPattern.toLimit(objective),
            operatorExploration = EvolutionWorkflow.operatorExploration,
            historySize = stochasticValue.sample,
            cloneProbability = stochasticValue.reevaluate,
            reject = reject.option),
          fg,
          phenotypeContent,
          stochasticValue,
          validate = validation
        )


}

import EvolutionWorkflow.*

object OSEEvolution:

  import org.openmole.core.dsl._

  given EvolutionMethod[OSEEvolution] =
    p =>
      OSE(
        origin = p.origin,
        genome = p.genome,
        objective = p.objective,
        outputs = p.evaluation.outputs,
        stochastic = p.stochastic,
        populationSize = p.populationSize,
        reject = p.reject
      )

  given ExplorationMethod[OSEEvolution, EvolutionWorkflow] =
    p =>
      EvolutionWorkflow(
        method = p,
        evaluation = p.evaluation,
        termination = p.termination,
        parallelism = p.parallelism,
        distribution = p.distribution,
        suggestion = p.suggestion(OSE.OriginAxe.fullGenome(p.origin, p.genome)),
        scope = p.scope
      )

  given ExplorationMethodSetter[OSEEvolution, EvolutionPattern] = (e, p) => e.copy(distribution = p)



import monocle.macros._

case class OSEEvolution(
  origin:         Seq[OSE.OriginAxe],
  objective:      Seq[OSE.FitnessPattern],
  evaluation:     DSL,
  termination:    OMTermination,
  populationSize: Int                          = 200,
  genome:         Genome                       = Seq(),
  stochastic:     OptionalArgument[Stochastic] = None,
  reject:         OptionalArgument[Condition]  = None,
  parallelism:    Int                          = EvolutionWorkflow.parallelism,
  distribution:   EvolutionPattern             = SteadyState(),
  suggestion:     Suggestion                   = Suggestion.empty,
  scope:          DefinitionScope              = "ose")
