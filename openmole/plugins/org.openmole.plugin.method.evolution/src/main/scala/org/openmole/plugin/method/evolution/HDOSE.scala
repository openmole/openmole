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
import monocle.macros.GenLens
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
    significanceC:       Vector[Double],
    significanceD:       Vector[Int],
    archiveSize:         Int,
    distance:            Double,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
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

      def operations(om: DeterministicHDOSE) = new Ops:
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get, CDGenome.discreteValues.get)(genome)

        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState: S = MGOHDOSE.initialState(om.distance)

        def distance: mgo.evolution.algorithm.HDOSEOperation.TooClose = MGOHDOSE.tooCloseByComponent(om.significanceC, om.significanceD)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p ⇒
            import p.*
            val res = MGOHDOSE.result[Phenotype](state, population, Genome.continuous(om.genome), Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)
            val distance = Variable(distanceVal, MGOHDOSE.distanceLens.get(state))

            val outputValues = if includeOutputs then DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

            genomes ++ fitness ++ Seq(generated, distance) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p ⇒
            import p.*
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            MGOHDOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p.*
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            MGOHDOSE.adaptiveBreeding[Phenotype](
              n,
              om.operatorExploration,
              continuous,
              discrete,
              om.significanceC,
              om.significanceD,
              Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context),
              rejectValue) apply (s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p.*
            MGOHDOSE.elitism[Phenotype](
              om.mu,
              om.limit,
              om.significanceC,
              om.significanceD,
              om.archiveSize,
              Genome.continuous(om.genome),
              Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context),
              om.distance) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S =
          def isTooCloseFromArchive =
            HDOSEOperation.isTooCloseFromArchive[G, I](
              distance,
              MGOHDOSE.archiveLens.get(state),
              CDGenome.scaledValues(Genome.continuous(om.genome)),
              _.genome,
              MGOHDOSE.distanceLens.get(state)
            )

          MGOHDOSE.archiveLens[Phenotype].modify: a =>
            def notTooClose = MGOHDOSE.archiveLens.get(islandState).sortBy(_.generation).filterNot(i => isTooCloseFromArchive(i.genome))
            a ++ notTooClose
          .apply(state)

        def migrateToIsland(population: Vector[I], state: S) = (DeterministicGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (DeterministicGAIntegration.migrateFromIsland(population, initialState.generation), state)

  case class StochasticHDOSE(
    mu: Int,
    limit: Vector[Double],
    genome: Genome,
    significanceC: Vector[Double],
    significanceD: Vector[Int],
    archiveSize: Int,
    distance: Double,
    phenotypeContent: PhenotypeContent,
    objectives: Seq[Objective],
    historySize: Int,
    cloneProbability: Double,
    operatorExploration: Double,
    reject: Option[Condition])

  object StochasticHDOSE:

    import mgo.evolution.algorithm.NoisyHDOSE.*
    import mgo.evolution.algorithm.{NoisyHDOSE ⇒ MGONoisyHDOSE, *}

    given MGOAPI.Integration[StochasticHDOSE, (IArray[Double], IArray[Int]), Phenotype] with
      api =>

      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = HDOSEState[Phenotype]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticHDOSE) = new Ops:
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get, CDGenome.discreteValues.get)(genome)

        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)

          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState = MGONoisyHDOSE.initialState(om.distance)

        def distance: mgo.evolution.algorithm.HDOSEOperation.TooClose = mgo.evolution.algorithm.HDOSE.tooCloseByComponent(om.significanceC, om.significanceD)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p =>
            import p.*

            val res = MGONoisyHDOSE.result(state, population, Objective.aggregate(om.phenotypeContent, om.objectives).from(context), Genome.continuous(om.genome), om.limit, keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)
            val distance = Variable(distanceVal, MGONoisyHDOSE.distanceLens.get(state))

            val outputValues =
              if includeOutputs
              then StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory))
              else Seq()

            genomes ++ fitness ++ Seq(samples, generated, distance) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._

            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            MGONoisyHDOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p.*
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))

            MGONoisyHDOSE.adaptiveBreeding[Phenotype](
              n,
              om.operatorExploration,
              om.cloneProbability,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              continuous,
              discrete,
              om.significanceC,
              om.significanceD,
              om.limit,
              rejectValue) apply(s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p.*

            MGONoisyHDOSE.elitism(
              om.mu,
              om.historySize,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              Genome.continuous(om.genome),
              om.significanceC,
              om.significanceD,
              om.archiveSize,
              om.limit,
              om.distance) apply(s, population, candidates, rng)


        def mergeIslandState(state: S, islandState: S): S =
          def isTooCloseFromArchive =
            HDOSEOperation.isTooCloseFromArchive[G, I](
              distance,
              MGONoisyHDOSE.archiveLens.get(state),
              CDGenome.scaledValues(Genome.continuous(om.genome)),
              _.genome,
              MGONoisyHDOSE.distanceLens.get(state)
            )

          MGONoisyHDOSE.archiveLens[Phenotype].modify: a =>
            def notTooClose = MGONoisyHDOSE.archiveLens.get(islandState).sortBy(_.generation).filterNot(i => isTooCloseFromArchive(i.genome))
            a ++ notTooClose
          .apply(state)



        def migrateToIsland(population: Vector[I], state: S) = (StochasticGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (StochasticGAIntegration.migrateFromIsland(population, initialState.generation), state)


  object OriginAxe:
    given factorDouble[D](using bounds: BoundedDomain[D, Double], step: DomainStep[D, Double]): Conversion[Factor[D, Double], OriginAxe] = f =>
      val (min, max) = bounds(f.domain).domain
      ContinuousOriginAxe(Genome.GenomeBound.ScalarDouble(f.value, min, max), step(f.domain))

    given doubleRange[D]: Conversion[Factor[Seq[DoubleRange], Array[Double]], OriginAxe] = f =>
      ContinuousSequenceOriginAxe(
        Genome.GenomeBound.SequenceOfDouble(f.value, f.domain.map(_.low).toArray, f.domain.map(_.high).toArray, f.domain.size),
        f.domain.map(_.step).toVector
      )

    given factorInt[D](using bounds: BoundedDomain[D, Int], step: DomainStep[D, Int]): Conversion[Factor[D, Int], OriginAxe] = f =>
      val (min, max) = bounds(f.domain).domain
      DiscreteOriginAxe(Genome.GenomeBound.ScalarInt(f.value, min, max), step(f.domain))

    given intRange[D]: Conversion[Factor[Seq[Range], Array[Int]], OriginAxe] = f =>
      DiscreteSequenceOriginAxe(
        Genome.GenomeBound.SequenceOfInt(f.value, f.domain.map(_.start).toArray, f.domain.map(_.end).toArray, f.domain.size),
        f.domain.map(_.step).toVector
      )

    given boolean: Conversion[Val[Boolean], OriginAxe] = v =>
      EnumerationOriginAxe(
        Genome.GenomeBound.Enumeration(v, Vector(true, false))
      )

    given booleanArray: Conversion[Factor[Int, Array[Boolean]], OriginAxe] = f =>
      EnumerationSequenceOriginAxe(
        Genome.GenomeBound.SequenceOfEnumeration(f.value, Vector.fill(f.domain)(Array(true, false)))
      )


    given enumeration[D, T](using fix: FixDomain[D, T]): Conversion[Factor[D, T], OriginAxe] = f =>
      val domain = fix(f.domain).domain.toVector
      EnumerationOriginAxe(Genome.GenomeBound.Enumeration(f.value, domain))

    def genomeBound(originAxe: OriginAxe) = originAxe match
      case c: ContinuousOriginAxe ⇒ c.p
      case d: DiscreteOriginAxe ⇒ d.p
      case cs: ContinuousSequenceOriginAxe ⇒ cs.p
      case ds: DiscreteSequenceOriginAxe ⇒ ds.p
      case en: EnumerationOriginAxe => en.p
      case en: EnumerationSequenceOriginAxe => en.p

    def toGenome(axes: Seq[OriginAxe]): Genome = axes.map(genomeBound)

    def significanceC(axes: Seq[OriginAxe]): Vector[Double] =
      axes.toVector.flatMap:
        case c: ContinuousOriginAxe ⇒ Seq(c.step)
        case cs: ContinuousSequenceOriginAxe ⇒ cs.step
        case _ => Seq()

    def significanceD(axes: Seq[OriginAxe]): Vector[Int] =
      axes.toVector.flatMap:
        case d: DiscreteOriginAxe ⇒ Seq(d.step)
        case ds: DiscreteSequenceOriginAxe ⇒ ds.step
        case en: EnumerationOriginAxe => Seq(1)
        case en: EnumerationSequenceOriginAxe => en.p.values.map(_ => 1)
        case _ => Seq()


  sealed trait OriginAxe
  case class ContinuousOriginAxe(p: Genome.GenomeBound.ScalarDouble, step: Double) extends OriginAxe
  case class ContinuousSequenceOriginAxe(p: Genome.GenomeBound.SequenceOfDouble, step: Vector[Double]) extends OriginAxe
  case class DiscreteOriginAxe(p: Genome.GenomeBound.ScalarInt, step: Int) extends OriginAxe
  case class DiscreteSequenceOriginAxe(p: Genome.GenomeBound.SequenceOfInt, step: Vector[Int]) extends OriginAxe
  case class EnumerationOriginAxe(p: Genome.GenomeBound.Enumeration[?]) extends OriginAxe
  case class EnumerationSequenceOriginAxe(p: Genome.GenomeBound.SequenceOfEnumeration[?]) extends OriginAxe


  def apply(
    origin: Seq[OriginAxe],
    objective: Seq[OSE.FitnessPattern],
    archiveSize: Int = 1000,
    distance: Double = 1.0,
    outputs: Seq[Val[?]] = Seq(),
    populationSize: Int = 200,
    stochastic: OptionalArgument[Stochastic] = None,
    reject: OptionalArgument[Condition] = None): EvolutionWorkflow =

    val genomeValue = OriginAxe.toGenome(origin)
    val significanceC = OriginAxe.significanceC(origin)
    val significanceD = OriginAxe.significanceD(origin)

    EvolutionWorkflow.stochasticity(objective.map(_.objective), stochastic.option) match
      case None ⇒
        val exactObjectives = Objectives.toExact(OSE.FitnessPattern.toObjectives(objective))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicHDOSE(
            mu = populationSize,
            genome = genomeValue,
            significanceC = significanceC,
            significanceD = significanceD,
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
      case Some(stochasticValue) ⇒
        val noisyObjectives = Objectives.toNoisy(OSE.FitnessPattern.toObjectives(objective))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)

        def validation: Validate =
          val aOutputs = outputs.map(_.toArray)
          Objectives.validate(noisyObjectives, aOutputs)

        EvolutionWorkflow.stochasticGAIntegration(
          StochasticHDOSE(
            mu = populationSize,
            genome = genomeValue,
            significanceC = significanceC,
            significanceD = significanceD,
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
    p ⇒
      EvolutionWorkflow(
        method = p,
        evaluation = p.evaluation,
        termination = p.termination,
        parallelism = p.parallelism,
        distribution = p.distribution,
        suggestion = p.suggestion(HDOSE.OriginAxe.toGenome(p.origin)),
        scope = p.scope
      )

  given ExplorationMethodSetter[HDOSEEvolution, EvolutionPattern] = (e, p) ⇒ e.copy(distribution = p)


import EvolutionWorkflow.*

case class HDOSEEvolution(
  origin:         Seq[HDOSE.OriginAxe],
  objective:      Seq[OSE.FitnessPattern],
  evaluation:     DSL,
  termination:    OMTermination,
  archiveSize:    Int                          = 500,
  populationSize: Int                          = 200,
  distance:       Double                       = 1.0,
  stochastic:     OptionalArgument[Stochastic] = None,
  reject:         OptionalArgument[Condition]  = None,
  parallelism:    Int                          = EvolutionWorkflow.parallelism,
  distribution:   EvolutionPattern             = EvolutionWorkflow.SteadyState(),
  suggestion:     Suggestion                   = Suggestion.empty,
  scope:          DefinitionScope              = "hdose")
