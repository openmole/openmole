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

  case class DeterministicHDOSE(
    mu:                  Int,
    limit:               Vector[Double],
    genome:              Genome,
    significanceC:       Vector[Double],
    significanceD:       Vector[Int],
    diversityDistance:   Double,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    operatorExploration: Double,
    reject:              Option[Condition])

  object DeterministicHDOSE:

    import cats.data.*
    import mgo.evolution.algorithm.HDOSE.*
    import mgo.evolution.algorithm.{ HDOSE => MGOHDOSE, * }

    implicit def integration: MGOAPI.Integration[DeterministicHDOSE, (Vector[Double], Vector[Int]), Phenotype] = new MGOAPI.Integration[DeterministicHDOSE, (Vector[Double], Vector[Int]), Phenotype]:
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
          def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState: S = EvolutionState(s = Archive.empty)

        def distance: mgo.evolution.algorithm.HDOSEOperation.Distance =
          MGOHDOSE.distanceByComponent(
            Genome.continuous(om.genome),
            om.significanceC,
            om.significanceD
          )

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p ⇒
            import p._
            val res = MGOHDOSE.result[Phenotype](state, population, Genome.continuous(om.genome), Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

            val outputValues = if includeOutputs then DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

            genomes ++ fitness ++ Seq(generated) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
            MGOHDOSE.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val continous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
            MGOHDOSE.adaptiveBreeding[Phenotype](
              n,
              om.operatorExploration,
              continous,
              discrete,
              om.significanceC,
              om.significanceD,
              om.diversityDistance,
              Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context),
              rejectValue) apply (s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            MGOHDOSE.elitism[Phenotype](
              om.mu,
              om.limit,
              om.significanceC,
              om.significanceD,
              om.diversityDistance,
              Genome.continuous(om.genome),
              Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context)) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S =
          def isTooCloseFromArchive =
            HDOSEOperation.isTooCloseFromArchive[G, I](
              distance,
              state.s,
              _.continuousValues.toVector,
              _.discreteValues.toVector,
              _.genome,
              om.diversityDistance
            )

          val archive = state.s ++ islandState.s.sortBy(_.generation).filterNot(i => isTooCloseFromArchive(i.genome))
          //val map = (state.s._2 ++ islandState.s._2).distinct
          state.copy(s = archive)

        def migrateToIsland(population: Vector[I], state: S) = (DeterministicGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (DeterministicGAIntegration.migrateFromIsland(population, initialState.generation), state)



  type Significance = Seq[By[Val[?], Double]]

  object Significance:
    def significanceC(genome: Genome, s: Significance): Vector[Double] =
      val sMap = s.map(s => s.value -> s.by).toMap[Val[?], Double]
      val genomeC = Genome.continuousGenome(genome)
      (Genome.toVals(genomeC) zip Genome.sizes(genomeC)).flatMap: (v, s) =>
        val sValue = sMap.getOrElse(v, 1.0)
        Vector.fill(s)(sValue)
      .toVector

    def significanceD(genome: Genome, s: Significance): Vector[Int] =
      val sMap = s.map(s => s.value -> s.by).toMap[Val[?], Double]
      val genomeD = Genome.discreteGenome(genome)
      (Genome.toVals(genomeD) zip Genome.sizes(genomeD)).flatMap: (v, s) =>
        val sValue = sMap.getOrElse(v, 1.0).toInt
        Vector.fill(s)(sValue)
      .toVector


  def apply(
    genome: Genome,
    objective: Seq[OSE.FitnessPattern],
    diversityDistance: Double = 1,
    significance: Significance = Seq(),
    outputs: Seq[Val[?]] = Seq(),
    populationSize: Int = 200,
    stochastic: OptionalArgument[Stochastic] = None,
    reject: OptionalArgument[Condition] = None): EvolutionWorkflow =

    val significanceC = Significance.significanceC(genome, significance)
    val significanceD = Significance.significanceD(genome, significance)

    EvolutionWorkflow.stochasticity(objective.map(_.objective), stochastic.option) match
      case None ⇒
        val exactObjectives = Objectives.toExact(OSE.FitnessPattern.toObjectives(objective))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicHDOSE(
            mu = populationSize,
            genome = genome,
            significanceC = significanceC,
            significanceD = significanceD,
            diversityDistance = diversityDistance,
            phenotypeContent = phenotypeContent,
            objectives = exactObjectives,
            limit = OSE.FitnessPattern.toLimit(objective),
            operatorExploration = EvolutionWorkflow.operatorExploration,
            reject = reject.option),
          genome,
          phenotypeContent,
          validate = Objectives.validate(exactObjectives, outputs)
        )
//      case Some(stochasticValue) ⇒
//        val fg = OriginAxe.fullGenome(origin, genome)
//        val noisyObjectives = Objectives.toNoisy(FitnessPattern.toObjectives(objective))
//        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)
//
//        def validation: Validate =
//          val aOutputs = outputs.map(_.toArray)
//          Objectives.validate(noisyObjectives, aOutputs)
//
//        EvolutionWorkflow.stochasticGAIntegration(
//          StochasticOSE(
//            mu = populationSize,
//            origin = OriginAxe.toOrigin(origin, genome),
//            genome = fg,
//            phenotypeContent = phenotypeContent,
//            objectives = noisyObjectives,
//            limit = FitnessPattern.toLimit(objective),
//            operatorExploration = EvolutionWorkflow.operatorExploration,
//            historySize = stochasticValue.sample,
//            cloneProbability = stochasticValue.reevaluate,
//            reject = reject.option),
//          fg,
//          phenotypeContent,
//          stochasticValue,
//          validate = validation
//        )


object HDOSEEvolution:


  import org.openmole.core.dsl._

//  given EvolutionMethod[HDOSEEvolution] =
//    p =>
//      HDOSE(
//        origin = p.origin,
//        genome = p.genome,
//        objective = p.objective,
//        outputs = p.evaluation.outputs,
//        //stochastic = p.stochastic,
//        populationSize = p.populationSize,
//        reject = p.reject
//      )


import EvolutionWorkflow.*

case class HDOSEEvolution(
  genome:         Genome,
  objective:      Seq[OSE.FitnessPattern],
  evaluation:     DSL,
  termination:    OMTermination,
  significance:   HDOSE.Significance           = Seq(),
  populationSize: Int                          = 200,
  stochastic:     OptionalArgument[Stochastic] = None,
  reject:         OptionalArgument[Condition]  = None,
  parallelism:    Int                          = EvolutionWorkflow.parallelism,
  distribution:   EvolutionPattern             = EvolutionWorkflow.SteadyState(),
  suggestion:     Suggestion                   = Suggestion.empty,
  scope:          DefinitionScope              = "hdose")
