package org.openmole.plugin.method.evolution

/*
 * Copyright (C) 2024 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import cats.implicits.*
import mgo.evolution.algorithm.NoisyHDOSE as MGONoisyHDOSE
import monocle.*
import org.openmole.plugin.method.evolution.Objective.ToObjective
import squants.time.Time

import scala.reflect.ClassTag
import monocle.*
import monocle.syntax.all.*


object HDOSE:

  def distanceVal = Val[Double]("distance", GAIntegration.namespace)

  case class DeterministicHDOSE(
    mu:                  Int,
    limit:               Vector[Double],
    genome:              Genome,
    weightC:             Vector[Double],
    weightD:             Vector[Double],
    archiveSize:         Int,
    distance:            Double,
    phenotypeContent:    PhenotypeContent,
    objectives:          Objectives,
    operatorExploration: Double,
    reject:              Option[Condition])

  object DeterministicHDOSE:

    import cats.data.*
    import mgo.evolution.algorithm.HDOSE.*
    import mgo.evolution.algorithm.{ HDOSE => MGOHDOSE, * }

    given MGOAPI.Integration[DeterministicHDOSE, (IArray[Double], IArray[Int]), Phenotype] with
      api =>

      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual[Phenotype]
      type S = HDOSEState[Phenotype]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def startTimeLens = Focus[S](_.startTime)
      def generationLens = Focus[S](_.generation)
      def evaluatedLens = Focus[S](_.evaluated)

      def operations(om: DeterministicHDOSE) = new Ops:
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues(om.genome.continuous).get, CDGenome.discreteValues(om.genome.discrete).get)(genome)

        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(om.genome.discrete)(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState: S = MGOHDOSE.initialState(om.distance)

        def distance: mgo.evolution.algorithm.HDOSEOperation.TooClose = MGOHDOSE.tooCloseByComponent(om.weightC, om.weightD, om.genome.discrete)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p =>
            import p.*
            val res = MGOHDOSE.result[Phenotype](state, population, om.genome.continuous, om.genome.discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)
            val distance = Variable(distanceVal, MGOHDOSE.distanceLens.get(state))
            val archive = Variable(GAIntegration.archiveVal.array, res.map(_.archive).toArray)

            val outputValues = if includeOutputs then DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

            genomes ++ fitness ++ Seq(generated, distance, archive) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p =>
            import p.*
            val continuous = om.genome.continuous
            val discrete = om.genome.discrete
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
            MGOHDOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p.*
            val continuous = om.genome.continuous
            val discrete = om.genome.discrete
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
            MGOHDOSE.adaptiveBreeding[Phenotype](
              n,
              om.operatorExploration,
              continuous,
              discrete,
              om.weightC,
              om.weightD,
              Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context),
              rejectValue) apply (s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p.*
            MGOHDOSE.elitism[Phenotype](
              om.mu,
              om.limit,
              om.weightC,
              om.weightD,
              om.archiveSize,
              om.genome.continuous,
              om.genome.discrete,
              Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context),
              om.distance) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S =
          def genomeValue(i: I) = (i.genome.continuousValues, i.genome.discreteValues(om.genome.discrete))

          def isTooCloseFromArchive =
            HDOSEOperation.isTooCloseFromArchive(
              distance,
              MGOHDOSE.archiveLens.get(state),
              genomeValue,
              MGOHDOSE.distanceLens.get(state)
            )

          MGOHDOSE.archiveLens[Phenotype].modify: a =>
            def notTooClose = MGOHDOSE.archiveLens.get(islandState).sortBy(_.generation).filterNot(i => isTooCloseFromArchive(genomeValue(i)))
            a ++ notTooClose
          .apply(state)

        def migrateToIsland(population: Vector[I], state: S) =
          val islandState = MGOHDOSE.archiveLens[Phenotype].modify(_.map(_.copy(initial = true)))(state)
          (DeterministicGAIntegration.migrateToIsland(population), islandState)

        def migrateFromIsland(population: Vector[I], initialState: S, state: S) =
          val islandState =  MGOHDOSE.archiveLens[Phenotype].modify(_.filterNot(_.initial))(state)
          (DeterministicGAIntegration.migrateFromIsland(population, initialState.generation), islandState)

  case class StochasticHDOSE(
    mu: Int,
    limit: Vector[Double],
    genome: Genome,
    weightC: Vector[Double],
    weightD: Vector[Double],
    archiveSize: Int,
    distance: Double,
    phenotypeContent: PhenotypeContent,
    objectives: Objectives,
    historySize: Int,
    cloneProbability: Double,
    operatorExploration: Double,
    reject: Option[Condition])

  object StochasticHDOSE:

    import mgo.evolution.algorithm.NoisyHDOSE.*
    import mgo.evolution.algorithm.{NoisyHDOSE => MGONoisyHDOSE, *}

    given MGOAPI.Integration[StochasticHDOSE, (IArray[Double], IArray[Int]), Phenotype] with
      api =>

      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = HDOSEState[Phenotype]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def startTimeLens = Focus[S](_.startTime)
      def generationLens = Focus[S](_.generation)
      def evaluatedLens = Focus[S](_.evaluated)

      def operations(om: StochasticHDOSE) = new Ops:
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues(om.genome.continuous).get, CDGenome.discreteValues(om.genome.discrete).get)(genome)

        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(om.genome.discrete)(v._1, None, v._2, None)

          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState = MGONoisyHDOSE.initialState(om.distance)

        def distance: mgo.evolution.algorithm.HDOSEOperation.TooClose = mgo.evolution.algorithm.HDOSE.tooCloseByComponent(om.weightC, om.weightD, om.genome.discrete)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p =>
            import p.*

            val res = MGONoisyHDOSE.result(state, population, Objective.aggregate(om.phenotypeContent, om.objectives).from(context), om.genome.continuous, om.genome.discrete, om.limit, keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)
            val distance = Variable(distanceVal, MGONoisyHDOSE.distanceLens.get(state))
            val archive = Variable(GAIntegration.archiveVal.array, res.map(_.archive).toArray)

            val outputValues =
              if includeOutputs
              then StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory))
              else Seq()

            genomes ++ fitness ++ Seq(samples, generated, distance, archive) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p =>
            import p._

            val continuous = om.genome.continuous
            val discrete = om.genome.discrete
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
            MGONoisyHDOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p.*
            val continuous = om.genome.continuous
            val discrete = om.genome.discrete
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))

            MGONoisyHDOSE.adaptiveBreeding[Phenotype](
              n,
              om.operatorExploration,
              om.cloneProbability,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              continuous,
              discrete,
              om.weightC,
              om.weightD,
              om.limit,
              rejectValue) apply(s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p.*

            MGONoisyHDOSE.elitism(
              om.mu,
              om.historySize,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              om.genome.continuous,
              om.genome.discrete,
              om.weightC,
              om.weightD,
              om.archiveSize,
              om.limit,
              om.distance) apply(s, population, candidates, rng)


        def mergeIslandState(state: S, islandState: S): S =
          def genomeValue(i: I) = (i.genome.continuousValues, i.genome.discreteValues(om.genome.discrete))

          def isTooCloseFromArchive =
            HDOSEOperation.isTooCloseFromArchive(
              distance,
              MGONoisyHDOSE.archiveLens.get(state),
              genomeValue,
              MGONoisyHDOSE.distanceLens.get(state)
            )

          MGONoisyHDOSE.archiveLens[Phenotype].modify: a =>
            def notTooClose = MGONoisyHDOSE.archiveLens.get(islandState).sortBy(_.generation).filterNot(i => isTooCloseFromArchive(genomeValue(i)))
            a ++ notTooClose
          .apply(state)


        def migrateToIsland(population: Vector[I], state: S) =
          val islandState = MGONoisyHDOSE.archiveLens[Phenotype].modify(_.map(_.copy(initial = true)))(state)
          (StochasticGAIntegration.migrateToIsland(population), islandState)

        def migrateFromIsland(population: Vector[I], initialState: S, state: S) =
          val islandState =  MGONoisyHDOSE.archiveLens[Phenotype].modify(_.filterNot(_.initial))(state)
          (StochasticGAIntegration.migrateFromIsland(population, initialState.generation), islandState)

  
  object OriginAxe:
    given factorDouble[D](using bounds: BoundedDomain[D, Double], weight: DomainWeight[D, Double]): Conversion[Factor[D, Double], OriginAxe] = f =>
      val (min, max) = bounds(f.domain).domain
      ScalarDoubleOriginAxe(Genome.GenomeBound.ScalarDouble(f.value, min, max), weight(f.domain))

    given factorSeqDouble[D](using bounds: BoundedDomain[D, Double], weight: DomainWeight[D, Double]): Conversion[Factor[Seq[D], Array[Double]], OriginAxe] = f =>
      SequenceOfDoubleOriginAxe(
        Genome.GenomeBound.SequenceOfDouble(f.value, f.domain.map(d => bounds(d).domain._1).toArray, f.domain.map(d => bounds(d).domain._2).toArray, f.domain.size),
        f.domain.map(d => weight(d)).toVector
      )

    given factorInt[D](using bounds: BoundedDomain[D, Int], weight: DomainWeight[D, Double]): Conversion[Factor[D, Int], OriginAxe] = f =>
      val (min, max) = bounds(f.domain).domain
      ScalarIntOriginAxe(Genome.GenomeBound.ScalarInt(f.value, min, max), weight(f.domain))

    given factorSeqInt[D](using bounds: BoundedDomain[D, Int], weight: DomainWeight[D, Double]): Conversion[Factor[Seq[D], Array[Int]], OriginAxe] = f =>
      SequenceOfIntOriginAxe(
        Genome.GenomeBound.SequenceOfInt(f.value, f.domain.map(d => bounds(d).domain._1).toArray, f.domain.map(d => bounds(d).domain._2).toArray, f.domain.size),
        f.domain.map(d => weight(d)).toVector
      )

    given factorContinuousInt[D](using bounds: BoundedDomain[D, Double], weight: DomainWeight[D, Double]): Conversion[Factor[D, Int], OriginAxe] = f =>
      val (min, max) = bounds(f.domain).domain
      ContinuousIntOriginAxe(Genome.GenomeBound.ContinuousInt(f.value, min.toInt, max.toInt), weight(f.domain))

    given factorSeqContinousInt[D](using bounds: BoundedDomain[D, Double], weight: DomainWeight[D, Double]): Conversion[Factor[Seq[D], Array[Int]], OriginAxe] = f =>
      SequenceOfContinuousIntOriginAxe(
        Genome.GenomeBound.SequenceOfContinuousInt(f.value, f.domain.map(d => bounds(d).domain._1.toInt).toArray, f.domain.map(d => bounds(d).domain._2.toInt).toArray, f.domain.size),
        f.domain.map(d => weight(d)).toVector
      )

    given enumeration[D, T](using fix: FixDomain[D, T], weight: DomainWeight[D, Double]): Conversion[Factor[D, T], OriginAxe] = f =>
      val domain = fix(f.domain).domain.toVector
      EnumerationOriginAxe(
        Genome.GenomeBound.Enumeration(f.value, domain),
        weight(f.domain)
      )
    given enumerationSeq[D, T: ClassTag](using fix: FixDomain[D, T], weight: DomainWeight[D, Double]): Conversion[Factor[Seq[D], Array[T]], OriginAxe] = f =>
      SequenceOfEnumerationOriginAxe(
        Genome.GenomeBound.SequenceOfEnumeration(f.value, f.domain.map(d => fix(d).domain.toArray).toVector),
        f.domain.map(d => weight(d)).toVector
      )


    def genomeBound(originAxe: OriginAxe) = originAxe match
      case c: ScalarDoubleOriginAxe => c.p
      case d: ScalarIntOriginAxe => d.p
      case cs: SequenceOfDoubleOriginAxe => cs.p
      case ds: SequenceOfIntOriginAxe => ds.p
      case en: EnumerationOriginAxe => en.p
      case en: SequenceOfEnumerationOriginAxe => en.p
      case d: ContinuousIntOriginAxe => d.p
      case d: SequenceOfContinuousIntOriginAxe => d.p

    def toGenome(axes: Seq[OriginAxe]): Genome = axes.map(genomeBound)

    def weightC(axes: Seq[OriginAxe]): Vector[Double] =
      axes.toVector.flatMap:
        case c: ScalarDoubleOriginAxe => Seq(c.weight)
        case cs: SequenceOfDoubleOriginAxe => cs.weight
        case d: ContinuousIntOriginAxe => Seq(d.weight)
        case d: SequenceOfContinuousIntOriginAxe => d.weight
        case _ => Seq()

    def weightD(axes: Seq[OriginAxe]): Vector[Double] =
      axes.toVector.flatMap:
        case d: ScalarIntOriginAxe => Seq(d.weight)
        case ds: SequenceOfIntOriginAxe => ds.weight
        case en: EnumerationOriginAxe => Seq(en.weight)
        case en: SequenceOfEnumerationOriginAxe => en.weight
        case _ => Seq()


  sealed trait OriginAxe
  case class ScalarDoubleOriginAxe(p: Genome.GenomeBound.ScalarDouble, weight: Double) extends OriginAxe
  case class SequenceOfDoubleOriginAxe(p: Genome.GenomeBound.SequenceOfDouble, weight: Vector[Double]) extends OriginAxe
  case class ScalarIntOriginAxe(p: Genome.GenomeBound.ScalarInt, weight: Double) extends OriginAxe
  case class SequenceOfIntOriginAxe(p: Genome.GenomeBound.SequenceOfInt, weight: Vector[Double]) extends OriginAxe
  case class ContinuousIntOriginAxe(p: Genome.GenomeBound.ContinuousInt, weight: Double) extends OriginAxe
  case class SequenceOfContinuousIntOriginAxe(p: Genome.GenomeBound.SequenceOfContinuousInt, weight: Vector[Double]) extends OriginAxe
  case class EnumerationOriginAxe(p: Genome.GenomeBound.Enumeration[?], weight: Double) extends OriginAxe
  case class SequenceOfEnumerationOriginAxe(p: Genome.GenomeBound.SequenceOfEnumeration[?], weight: Vector[Double]) extends OriginAxe


  def apply(
    origin: Seq[OriginAxe],
    objective: Seq[OSE.FitnessPattern],
    archiveSize: Int = 1000,
    distance: Double = 0.01,
    outputs: Seq[Val[?]] = Seq(),
    populationSize: Int = 200,
    stochastic: OptionalArgument[Stochastic] = None,
    reject: OptionalArgument[Condition] = None): EvolutionWorkflow =

    val genomeValue = OriginAxe.toGenome(origin)
    val weightC = OriginAxe.weightC(origin)
    val weightD = OriginAxe.weightD(origin)

    assert(Genome.discrete(genomeValue).size == weightD.size, s"Discrete ${Genome.discrete(genomeValue)} should be of the same size as weights ${weightD}")
    assert(Genome.continuous(genomeValue).size == weightC.size, s"Discrete ${Genome.continuous(genomeValue)} should be of the same size as weights ${weightC}")

    EvolutionWorkflow.stochasticity(objective.map(_.objective), stochastic.option) match
      case None =>
        val exactObjectives = Objectives.toExact(OSE.FitnessPattern.toObjectives(objective))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

        EvolutionWorkflow.deterministicGA(
          DeterministicHDOSE(
            mu = populationSize,
            genome = genomeValue,
            weightC = weightC,
            weightD = weightD,
            archiveSize = archiveSize,
            distance = distance,
            phenotypeContent = phenotypeContent,
            objectives = exactObjectives,
            limit = OSE.FitnessPattern.toLimit(objective),
            operatorExploration = EvolutionWorkflow.operatorExploration,
            reject = reject.option),
          genomeValue,
          phenotypeContent,
          validate = Objectives.validate(exactObjectives, outputs)
        )
      case Some(stochasticValue) =>
        val noisyObjectives = Objectives.toNoisy(OSE.FitnessPattern.toObjectives(objective))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)

        def validation: Validate =
          val aOutputs = outputs.map(_.toArray)
          Objectives.validate(noisyObjectives, aOutputs)

        EvolutionWorkflow.stochasticGA(
          StochasticHDOSE(
            mu = populationSize,
            genome = genomeValue,
            weightC = weightC,
            weightD = weightD,
            archiveSize = archiveSize,
            distance = distance,
            phenotypeContent = phenotypeContent,
            objectives = noisyObjectives,
            limit = OSE.FitnessPattern.toLimit(objective),
            operatorExploration = EvolutionWorkflow.operatorExploration,
            historySize = stochasticValue.sample,
            cloneProbability = stochasticValue.reevaluate,
            reject = reject.option),
          genomeValue,
          phenotypeContent,
          stochasticValue,
          validate = validation
        )

import EvolutionWorkflow.*

object HDOSEEvolution:
  import org.openmole.core.dsl.*

  given EvolutionMethod[HDOSEEvolution] =
    p =>
      HDOSE(
        origin = p.origin,
        objective = p.objective,
        archiveSize = p.archiveSize,
        distance = p.distance,
        outputs = p.evaluation.outputs,
        stochastic = p.stochastic,
        populationSize = p.populationSize,
        reject = p.reject
      )

  given ExplorationMethod[HDOSEEvolution, EvolutionWorkflow] =
    p =>
      EvolutionWorkflow(
        method = p,
        evaluation = p.evaluation,
        termination = p.termination,
        parallelism = p.parallelism,
        distribution = p.distribution,
        suggestion = p.suggestion(HDOSE.OriginAxe.toGenome(p.origin)),
        scope = p.scope
      )

  given ExplorationMethodSetter[HDOSEEvolution, EvolutionPattern] = (e, p) => e.copy(distribution = p)


import EvolutionWorkflow.*

case class HDOSEEvolution(
  origin:         Seq[HDOSE.OriginAxe],
  objective:      Seq[OSE.FitnessPattern],
  evaluation:     DSL,
  termination:    OMTermination,
  archiveSize:    Int                          = 500,
  populationSize: Int                          = 200,
  distance:       Double                       = 0.01,
  stochastic:     OptionalArgument[Stochastic] = None,
  reject:         OptionalArgument[Condition]  = None,
  parallelism:    Int                          = EvolutionWorkflow.parallelism,
  distribution:   EvolutionPattern             = EvolutionWorkflow.SteadyState(),
  suggestion:     Suggestion                   = Suggestion.empty,
  scope:          DefinitionScope              = "hdose")
