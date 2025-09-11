package org.openmole.plugin.method.evolution


/*
 * Copyright (C) 2025 Romain Reuillon
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


import monocle.Focus
import monocle.syntax.all.*
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.method.evolution.PSE.PatternAxe
import squants.time.Time

object PPSE:

  def likelihoodVal = Val[Double]("likelihood", GAIntegration.namespace)

  case class DeterministicParams(
    pattern:          Vector[Double] => Vector[Int],
    genome:           GenomeDouble,
    phenotypeContent: PhenotypeContent,
    objectives:       Seq[Objective],
    grid:             Seq[PatternAxe],
    dilation:         Double,
    reject:           Option[Condition],
    density:          Option[FromContext[Double]],
    gmmIterations:    Int,
    gmmTolerance:     Double,
    warmupSampler:    Int,
    maxRareSample:    Int,
    minClusterSize:   Int,
    regularisationEpsilon: Double)

  object DeterministicParams:

    import mgo.evolution.algorithm.*

    given MGOAPI.Integration[DeterministicParams, IArray[Double], Phenotype] with MGOAPI.MGOState[mgo.evolution.algorithm.PPSE.PPSEState]:
      api =>
      type G = mgo.evolution.algorithm.PPSE.Genome
      type I = mgo.evolution.algorithm.PPSE.Individual[Phenotype]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicParams) = new Ops:
        def startTimeLens = Focus[S](_.startTime)
        def generationLens = Focus[S](_.generation)
        def evaluatedLens = Focus[S](_.evaluated)

        def genomeValues(genome: G) = genome._1
        def buildGenome(vs: Vector[Variable[?]]) = (Genome.fromVariables(vs, om.genome)._1, Double.PositiveInfinity)

        def rejectValue = FromContext: p =>
          import p.*
          om.reject.map(f => GAIntegration.rejectValue[IArray[Double]](f, om.genome, c => IArray.unsafeFromArray(c.toArray), _ => IArray.empty).from(context))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val cs = genomeValues(g)
          Genome.toVariables(om.genome, cs, IArray.empty, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S)  =
          mgo.evolution.algorithm.PPSE.buildIndividual(genome, phenotype, state.generation, false)

        def initialState = EvolutionState(s = mgo.evolution.algorithm.PPSE.PPSEState())

        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext: p =>
          import p._
          val toFitness = Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context)
          val res = mgo.evolution.algorithm.PPSE.result[Phenotype](population, state, om.genome.continuous, toFitness andThen om.pattern)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_ => Vector.empty), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype).map(toFitness))
          val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)
          val densities = Seq(Variable(likelihoodVal.array, res.map(_.density).toArray))
          val outputValues = if includeOutputs then DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

          genomes ++ fitness ++ Seq(generated) ++ densities ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext: p =>
          import p.*
          val continuous = om.genome.continuous
          mgo.evolution.algorithm.PPSE.initialGenomes(n, continuous, rejectValue.from(context), om.warmupSampler, rng)

        private def pattern(phenotype: Phenotype) = FromContext: p =>
          import p.*
          om.pattern(Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context).apply(phenotype))

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext: p =>
          import p.*

          mgo.evolution.algorithm.PPSEOperation.breeding[S, I, G](
            om.genome.continuous,
            identity,
            n,
            rejectValue.from(context),
            _.s.gmm,
            om.warmupSampler)(s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) = FromContext: p =>
          import p.*

          def densityValue =
            om.density.map: density =>
              (g: IArray[Double]) =>
                val scaled = Genome.toVariables(om.genome, g, IArray.empty, scale = true)
                density.from(scaled)

          val (s2, elited) = mgo.evolution.algorithm.PPSEOperation.elitism[S, I, Phenotype](
            _.genome,
            _.phenotype,
            pattern(_).from(context),
            om.genome.continuous,
            rejectValue.from(context),
            Focus[S](_.s.likelihoodRatioMap),
            Focus[S](_.s.hitmap),
            Focus[S](_.s.gmm),
            density = densityValue,
            maxRareSample = om.maxRareSample,
            iterations = om.gmmIterations,
            tolerance = om.gmmTolerance,
            dilation = om.dilation,
            minClusterSize = om.minClusterSize,
            regularisationEpsilon = om.regularisationEpsilon) apply (s, population, candidates, rng)

          val s3 = Focus[S](_.generation).modify(_ + 1)(s2)
          (s3, elited)

        def mergeIslandState(state: S, islandState: S): S =
          def sumMap[K, V](m1: Map[K, V], m2: Map[K, V], sum: (V, V) => V, zero: V): Map[K, V] =
            def allKeys = m1.keys ++ m2.keys
            def newMap = allKeys.map: k =>
              k -> sum(m1.getOrElse(k, zero), m2.getOrElse(k, zero))
            newMap.toMap

          state.
            focus(_.s.likelihoodRatioMap).modify(m => sumMap(initialState.s.likelihoodRatioMap, m, _ + _, 0)).
            focus(_.s.hitmap).modify(m => sumMap(initialState.s.hitmap, m, _ + _, 0))


        def migrateToIsland(population: Vector[I], state: S) =  (population.map(_.copy(initial = true)), state)

        def migrateFromIsland(population: Vector[I], initialState: S, state: S) =
          def diffMap[K, V](m1: Map[K, V], m2: Map[K, V], diff: (V, V) => V, zero: V): Map[K, V] =
            def allKeys = m2.keys
            def newMap = allKeys.map: k =>
              k -> diff(m2(k), m1.getOrElse(k, zero))
            newMap.toMap

          val migratedPopulation = population.filter(!_.initial).map(_.copy(generation = initialState.generation))
          val migratedState =
            state.
              focus(_.s.likelihoodRatioMap).modify(m => diffMap(initialState.s.likelihoodRatioMap, m, _ - _, 0)).
              focus(_.s.hitmap).modify(m => diffMap(initialState.s.hitmap, m, _ - _, 0))

          (migratedPopulation, migratedState)


  def apply(
    genome:    GenomeDouble,
    objective: Seq[PatternAxe],
    stochastic: OptionalArgument[Stochastic],
    dilation:  Double,
    gmmIterations:    Int,
    gmmTolerance:     Double,
    warmupSampler:    Int,
    maxRareSample:    Int,
    minClusterSize:   Int,
    regularisationEpsilon: Double,
    reject:    Option[Condition],
    density:   Option[FromContext[Double]],
    outputs:   Seq[Val[?]]     = Seq()) =
    val exactObjectives = Objectives.toExact(objective.map(_.p))
    val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

    def params =
      DeterministicParams(
        mgo.evolution.niche.irregularGrid(objective.map(_.scale).toVector),
        genome,
        phenotypeContent,
        exactObjectives,
        reject = reject,
        density = density,
        grid = objective,
        dilation = dilation,
        gmmIterations = gmmIterations,
        gmmTolerance = gmmTolerance,
        warmupSampler = warmupSampler,
        maxRareSample = maxRareSample,
        minClusterSize = minClusterSize,
        regularisationEpsilon = regularisationEpsilon)

    EvolutionWorkflow.stochasticity(objective.map(_.p), stochastic.option) match
      case None =>
        EvolutionWorkflow.deterministicGA(
          params,
          genome,
          phenotypeContent,
          validate = Objectives.validate(exactObjectives, outputs)
        )
      case Some(stochastic) =>
        EvolutionWorkflow.stochasticGA(
          params,
          genome,
          phenotypeContent,
          stochastic,
          validate = Objectives.validate(exactObjectives, outputs)
        )



import monocle.macros._
import EvolutionWorkflow._

object PPSEEvolution:

  object Density:

    import org.apache.commons.math3.distribution.*

    case class IndependentJoint(density: Seq[Density]) extends Density
    case class GaussianDensity(v: Val[Double], mean: Double, sd: Double) extends Density

    def density(d: Density): FromContext[Double] = FromContext: p =>
      import p.*
      d match
        case d: IndependentJoint =>
          if d.density.nonEmpty
          then d.density.map(di => density(di).from(context)).reduceLeft(_ * _)
          else 1.0
        case d: GaussianDensity =>
          val dist = new NormalDistribution(d.mean, d.sd)
          dist.density(context(d.v))

    //TODO implement validation

    given Conversion[Val[Double] In om.NormalDistribution, GaussianDensity] = x => GaussianDensity(x.value, x.domain.mean, x.domain.std)

  sealed trait Density

  import org.openmole.core.dsl.DSL

  given EvolutionMethod[PPSEEvolution] = p =>
    def density =
      if p.density.isEmpty
      then None
      else Some(Density.density(Density.IndependentJoint(p.density)))

    PPSE(
      genome = p.genome,
      objective = p.objective,
      outputs = p.evaluation.outputs,
      stochastic = p.stochastic,
      dilation = p.dilation,
      gmmIterations = p.gmmIterations,
      gmmTolerance = p.gmmTolerance,
      warmupSampler = p.warmupSampler,
      maxRareSample = p.maxRareSample,
      minClusterSize = p.minClusterSize,
      regularisationEpsilon = p.regularisationEpsilon,
      reject = p.reject,
      density = density
    )

  given ExplorationMethod[PPSEEvolution, EvolutionWorkflow] = p =>
    EvolutionWorkflow(
      method = p,
      evaluation = p.evaluation,
      termination = p.termination,
      parallelism = p.parallelism,
      distribution = p.distribution,
      suggestion = p.suggestion(p.genome),
      scope = p.scope
    )

  given ExplorationMethodSetter[PPSEEvolution, EvolutionPattern] = (e, p) => e.copy(distribution = p)


case class PPSEEvolution(
  genome:      GenomeDouble,
  objective:   Seq[PSE.PatternAxe],
  evaluation:  DSL,
  termination: OMTermination,
  reject:      OptionalArgument[Condition]              = None,
  density:     Seq[PPSEEvolution.Density]               = Seq(),
  stochastic:  OptionalArgument[Stochastic]             = None,
  parallelism: Int                                      = EvolutionWorkflow.parallelism,
  distribution: EvolutionPattern                        = SteadyState(),
  suggestion: Suggestion                                = Suggestion.empty,
  dilation: Double                                      = 4.0,
  gmmIterations: Int                                    = 100,
  gmmTolerance: Double                                  = 0.0001,
  warmupSampler: Int                                    = 10000,
  maxRareSample: Int                                    = 10,
  minClusterSize: Int                                   = 10,
  regularisationEpsilon: Double                         = 10e-6,
  scope:       DefinitionScope                          = "ppse")

