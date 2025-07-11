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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

import scala.language.higherKinds
import cats._
import cats.implicits._
import org.openmole.core.exception.UserBadDataError
import org.openmole.plugin.task.tools.AssignTask
import org.openmole.plugin.tool.pattern
import org.openmole.plugin.tool.pattern._

trait EvolutionMethod[M]:
  def apply(m: M): EvolutionWorkflow

object EvolutionWorkflow:
  val operatorExploration = 0.1
  val parallelism = 100

  def apply[M](
    method: M,
    evaluation: DSL,
    termination: OMTermination,
    parallelism: Int = 1,
    distribution: EvolutionPattern = SteadyState(),
    suggestion: Genome.SuggestedValues,
    scope: DefinitionScope)(using evolutionMethod: EvolutionMethod[M]): DSLContainer[EvolutionWorkflow] =
    distribution match
      case s: SteadyState =>
        SteadyStateEvolution(
          method = method,
          evaluation = evaluation,
          parallelism = parallelism,
          termination = termination,
          wrap = s.wrap,
          suggestion = suggestion,
          scope = scope
        )
      case i: Island =>
        val steadyState =
          SteadyStateEvolution(
            method = method,
            evaluation = evaluation,
            termination = i.termination,
            parallelism = i.parallelism,
            wrap = false,
            suggestion = suggestion,
            scope = scope
          )

        IslandEvolution(
          island = steadyState,
          parallelism = parallelism,
          termination = termination,
          sample = i.sample,
          scope = scope
        )


  def stochasticity(objectives: Objectives, stochastic: Option[Stochastic]) =
    (Objectives.onlyExact(objectives), stochastic) match
      case (true, None)     => None
      case (true, Some(s))  => Some(s)
      case (false, Some(s)) => Some(s)
      case (false, None)    => throw new UserBadDataError("Aggregation have been specified for some objective, but no stochastic parameter is provided.")

  def deterministicGA[AG, VA](
    ag:               AG,
    genome:           Genome,
    phenotypeContent: PhenotypeContent,
    validate:         Validate         = Validate.success)(using algorithm: MGOAPI.Integration[AG, VA, Phenotype]): EvolutionWorkflow =
    val _validate = validate
    new EvolutionWorkflow:
      type Integration = algorithm.type

      val integration = algorithm
      def operations = integration.operations(ag)

      def validate = _validate

      def buildIndividual(g: G, context: Context, state: S): I =
        operations.buildIndividual(g, variablesToPhenotype(context), state)

      def inputVals = Genome.toVals(genome)
      def outputVals = PhenotypeContent.toVals(phenotypeContent)

      def genomeToVariables(genome: G): FromContext[Seq[Variable[?]]] =
        operations.genomeToVariables(genome)

      def variablesToPhenotype(context: Context) = Phenotype.fromContext(context, phenotypeContent)


  def stochasticGA[AG, VA](
    ag:               AG,
    genome:           Genome,
    phenotypeContent: PhenotypeContent,
    replication:      Stochastic,
    validate:         Validate         = Validate.success)(using algorithm: MGOAPI.Integration[AG, VA, Phenotype]): EvolutionWorkflow =
    val _validate = validate
    new EvolutionWorkflow:
      type Integration = algorithm.type

      val integration = algorithm
      def operations = integration.operations(ag)

      def validate = _validate

      def buildIndividual(genome: G, context: Context, state: S): I =
        operations.buildIndividual(genome, variablesToPhenotype(context), state)

      def inputVals = Genome.toVals(genome) ++ replication.seed.prototype
      def outputVals = PhenotypeContent.toVals(phenotypeContent)

      def genomeToVariables(g: G): FromContext[Seq[Variable[?]]] =
        FromContext: p =>
          import p.*
          val seeder = replication.seed
          operations.genomeToVariables(g).from(context) ++ seeder(p.random())

      def variablesToPhenotype(context: Context) = Phenotype.fromContext(context, phenotypeContent)


  object OMTermination:
    def toTermination(oMTermination: OMTermination, integration: EvolutionWorkflow) =
      oMTermination match
        case AfterEvaluated(e) => (s: integration.S, population: Vector[integration.I]) => mgo.evolution.stop.afterEvaluated(e, integration.evaluatedLens)(s, population)
        case AfterGeneration(g) => (s: integration.S, population: Vector[integration.I]) => mgo.evolution.stop.afterGeneration(g, integration.generationLens)(s, population)
        case AfterDuration(d) => (s: integration.S, population: Vector[integration.I]) => mgo.evolution.stop.afterDuration(d, integration.startTimeLens)(s, population)

    case class AfterEvaluated(steps: Long) extends OMTermination
    case class AfterGeneration(steps: Long) extends OMTermination
    case class AfterDuration(duration: Time) extends OMTermination

    given Conversion[Long, OMTermination] = AfterEvaluated.apply
    given Conversion[Int, OMTermination] = AfterEvaluated.apply
    given Conversion[Time, OMTermination] = AfterDuration.apply

  sealed trait OMTermination

  sealed trait EvolutionPattern
  case class SteadyState(wrap: Boolean = false) extends EvolutionPattern
  case class Island(termination: OMTermination, sample: OptionalArgument[Int] = None, parallelism: Int = 1) extends EvolutionPattern

  def SteadyStateEvolution[M](
    method:      M,
    evaluation:  DSL,
    termination: OMTermination,
    parallelism: Int                          = 1,
    suggestion:  Genome.SuggestedValues       = Genome.SuggestedValues.empty,
    wrap:        Boolean                      = false,
    scope:       DefinitionScope              = "steady state evolution")(using evolutionMethod: EvolutionMethod[M]) =
    implicit def defScope: DefinitionScope = scope
    val evolution = evolutionMethod(method)

    val wrapped = pattern.wrap(evaluation, evolution.inputVals, evolution.outputVals, wrap)
    val randomGenomes = BreedTask(evolution, parallelism, suggestion) set ((inputs, outputs) += evolution.populationVal)

    val scaleGenome = ScalingGenomeTask(evolution)
    val toOffspring = ToOffspringTask(evolution)
    val elitism = ElitismTask(evolution)
    val terminationTask = TerminationTask(evolution, termination)
    val breed = BreedTask(evolution, 1, Genome.SuggestedValues.empty)

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (evolution.populationVal, evolution.genomeVal, evolution.stateVal),
        (inputs, outputs) ++= evolution.outputVals
      )

    val masterLast =
      EmptyTask() set (
        (inputs, outputs) += (
          evolution.populationVal,
          evolution.stateVal,
          evolution.genomeVal.toArray,
          evolution.terminatedVal)
      )

    val master =
      ((masterFirst -- toOffspring keepAll (Seq(evolution.stateVal, evolution.genomeVal) ++ evolution.outputVals)) -- elitism -- terminationTask -- breed -- masterLast) &
        (masterFirst -- elitism keep evolution.populationVal) &
        (elitism -- breed keep evolution.populationVal) &
        (elitism -- masterLast keep evolution.populationVal) &
        (terminationTask -- masterLast keep (evolution.terminatedVal, evolution.generationVal))

    val masterTask = MoleTask(master) set (exploredOutputs += evolution.genomeVal.toArray)

    val slave = scaleGenome -- Strain(wrapped)
    
    val masterSlave =
      MasterSlave(
        randomGenomes,
        master = masterTask,
        slave = slave,
        state = Seq(evolution.populationVal, evolution.stateVal),
        slaves = parallelism,
        stop = evolution.terminatedVal
      )

    val firstTask = InitialStateTask(evolution)

    val puzzle =
      (Strain(firstTask) -- masterSlave) &
        (firstTask oo wrapped block (evolution.populationVal, evolution.stateVal))

    DSLContainer(
      puzzle,
      output = Some(masterTask),
      method = evolution,
      validate = evolution.validate,
      delegate = Vector(slave)
    )

  def IslandEvolution(
    island:      DSLContainer[EvolutionWorkflow],
    parallelism: Int,
    termination: OMTermination,
    sample:      OptionalArgument[Int]           = None,
    scope:       DefinitionScope                 = "island evolution") =

    implicit def defScope: DefinitionScope = scope

    val t = island.method

    val initialIslandStateVal = t.stateVal.withName("initialIslandState")
    val islandStateVal = t.stateVal.withName("islandState")
    val islandPopulationPrototype = t.populationVal.withName("islandPopulation")

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (t.populationVal, t.offspringPopulationVal, t.stateVal, islandStateVal)
      )

    val masterLast =
      EmptyTask() set (
        (inputs, outputs) += (t.populationVal, t.stateVal, islandPopulationPrototype.toArray, t.terminatedVal)
      )

    val elitism = IslandElitismTask(t, islandStateVal)
    val generateIsland = GenerateIslandTask(t, sample, 1, islandPopulationPrototype)
    val terminationTask = TerminationTask(t, termination)

    val toIsland = ToIslandTask(t, islandPopulationPrototype, initialIslandStateVal)
    val fromIsland = FromIslandTask(t, islandStateVal, initialIslandStateVal)

    val master =
      ((masterFirst -- elitism keep (t.stateVal, t.populationVal, t.offspringPopulationVal, islandStateVal)) -- terminationTask -- masterLast keep (t.terminatedVal, t.stateVal)) &
        (elitism -- generateIsland -- masterLast) &
        (elitism -- masterLast keep t.populationVal)

    val masterTask = MoleTask(master) set (exploredOutputs += islandPopulationPrototype.toArray)

    val generateInitialIslands =
      GenerateIslandTask(t, sample, parallelism, islandPopulationPrototype) set (
        (inputs, outputs) += t.stateVal,
        outputs += t.populationVal
      )

    val islandTask = MoleTask(island)

    val slaveFist = EmptyTask() set ((inputs, outputs) += (t.stateVal, islandPopulationPrototype))
    val slaveLast = EmptyTask() set ((inputs, outputs) += (t.offspringPopulationVal, islandStateVal))

    val slave =
      (slaveFist -- toIsland -- islandTask -- fromIsland -- slaveLast) &
        (toIsland -- fromIsland keep initialIslandStateVal)

    val masterSlave = MasterSlave(
      generateInitialIslands,
      masterTask,
      slave,
      state = Seq(t.populationVal, t.stateVal),
      slaves = parallelism,
      stop = t.terminatedVal
    )

    val first = InitialStateTask(t)

    val puzzle =
      (Strain(first) -- masterSlave) &
        (first oo Funnel(islandTask) block (t.populationVal, t.stateVal))

    DSLContainer(
      puzzle,
      output = Some(masterTask),
      delegate = Vector(islandTask),
      method = t)

end EvolutionWorkflow

trait EvolutionWorkflow:
  type Integration <: MGOAPI.Integration[?, ?, ?]

  val integration: Integration

  export integration.{G, I, S}
  export integration.{startTimeLens, generationLens, evaluatedLens}

  def operations: integration.Ops

  type Pop = Array[I]

  def validate: Validate

  def genomeType = ValType[G]
  def stateType = ValType[S]
  def individualType = ValType[I]
  def populationType: ValType[Pop] = ValType[Pop](using Manifest.arrayType[I](manifest[I]))

  def buildIndividual(genome: G, context: Context, state: S): I

  def inputVals: Seq[Val[?]]
  def outputVals: Seq[Val[?]]

  def genomeToVariables(genome: G): FromContext[Seq[Variable[?]]]

  def genomeVal = Val[G]("genome", GAIntegration.namespace)(using genomeType)
  def individualVal = Val[I]("individual", GAIntegration.namespace)(using individualType)
  def populationVal = Val[Pop]("population", GAIntegration.namespace)(using populationType)
  def offspringPopulationVal = Val[Pop]("offspring", GAIntegration.namespace)(using populationType)
  def stateVal = Val[S]("state", GAIntegration.namespace)(using stateType)
  def generationVal = GAIntegration.generationVal
  def evaluatedVal = GAIntegration.evaluatedVal
  def terminatedVal = Val[Boolean]("terminated", GAIntegration.namespace)


case class Stochastic(
  seed:       SeedVariable = SeedVariable.empty,
  sample:     Int          = 100,
  reevaluate: Double       = 0.2
)

object GAIntegration:

  def namespace = Namespace("evolution")

  def samplesVal = Val[Int]("samples", namespace)
  def generatedVal = Val[Long]("generated", namespace)
  def generationVal = Val[Long]("generation", namespace)
  def evaluatedVal = Val[Long]("evaluated", namespace)
  def archiveVal = Val[Boolean]("archive", namespace)

  def genomeToVariable(
    genome: Genome,
    values: (IArray[Double], IArray[Int]),
    scale:  Boolean) =
    val (continuous, discrete) = values
    Genome.toVariables(genome, continuous, discrete, scale)

  def genomesOfPopulationToVariables[I](
    genome: Genome,
    values: Vector[(Vector[Double], Vector[Int])],
    scale:  Boolean): Vector[Variable[?]] =

    val variables = values.map { (continuous, discrete) => Genome.toVariables(genome, IArray.from(continuous), IArray.from(discrete), scale) }
    genome.zipWithIndex.map { (g, i) => Genome.toArrayVariable(g, variables.map(_(i).value)) }.toVector

  def objectivesOfPopulationToVariables[I](objectives: Objectives, phenotypeValues: Vector[Vector[Double]]): Vector[Variable[?]] =
    Objectives.resultPrototypes(objectives).toVector.zipWithIndex.map: (objective, i) =>
      Variable(
        objective.withType[Array[Double]],
        phenotypeValues.map(_(i)).toArray
      )

  def rejectValue[G](reject: Condition, genome: Genome, continuous: G => IArray[Double], discrete: G => IArray[Int]) =
    FromContext: p =>
      import p.*
      (g: G) =>
        val genomeVariables = GAIntegration.genomeToVariable(genome, (continuous(g), discrete(g)), scale = true)
        reject.from(genomeVariables)


object DeterministicGAIntegration:
  import mgo.evolution.algorithm._

  def migrateToIsland[P](population: Vector[CDGenome.DeterministicIndividual.Individual[P]]) = population.map(_.copy(initial = true))
  def migrateFromIsland[P](population: Vector[CDGenome.DeterministicIndividual.Individual[P]], generation: Long) = population.filter(!_.initial).map(_.copy(generation = generation))

  def outputValues(phenotypeContent: PhenotypeContent, phenotypes: Seq[Phenotype]) =
    val outputs = phenotypes.map { p => Phenotype.outputs(phenotypeContent, p) }
    (phenotypeContent.outputs zip outputs.transpose).map { (v, va) => Variable.unsecure(v.toArray, va) }

object StochasticGAIntegration:
  import mgo.evolution.algorithm._

  def migrateToIsland[P](population: Vector[CDGenome.NoisyIndividual.Individual[P]]) = population.map(_.copy(historyAge = 0, initial = true))

  def migrateFromIsland[P](population: Vector[CDGenome.NoisyIndividual.Individual[P]], generation: Long) =
    def keepIslandHistoryPart(i: CDGenome.NoisyIndividual.Individual[P]) = i.copy(phenotypeHistory = i.phenotypeHistory.takeRight(i.historyAge.toInt))
    population.filter(_.historyAge > 0).map(_.copy(generation = generation, initial = false)).map(keepIslandHistoryPart)

  def outputValues(phenotypeContent: PhenotypeContent, phenotypeHistories: Seq[IArray[Phenotype]]) =
    val outputs = phenotypeHistories.map { _.toArray.map { p => Phenotype.outputs(phenotypeContent, p) }.transpose }
    (phenotypeContent.outputs zip outputs.transpose).map { case (v, va) => Variable.unsecure(v.toArray.toArray, va) }

object MGOAPI:
  import monocle.*

  trait MGOState[IS]:
    type S = _root_.mgo.evolution.algorithm.EvolutionState[IS]

    def startTimeLens = Focus[S](_.startTime)
    def generationLens = Focus[S](_.generation)
    def evaluatedLens = Focus[S](_.evaluated)

  trait Integration[A, V, P]:
    type I
    type G
    type S

    implicit def iManifest: Manifest[I]
    implicit def gManifest: Manifest[G]
    implicit def sManifest: Manifest[S]

    def startTimeLens: monocle.Lens[S, Long]
    def generationLens: monocle.Lens[S, Long]
    def evaluatedLens: monocle.Lens[S, Long]

    def operations(a: A): Ops

    trait Ops:
      def metadata(state: S, data: SaveOption): EvolutionMetadata = EvolutionMetadata.none

      def genomeValues(genome: G): V
      def genomeToVariables(genome: G): FromContext[Vector[Variable[?]]]

      def buildGenome(context: Vector[Variable[?]]): G
      def buildIndividual(genome: G, phenotype: P, state: S): I

      def initialState: S
      def initialGenomes(n: Int, rng: scala.util.Random): FromContext[Vector[G]]

      def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random): FromContext[Vector[G]]
      def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random): FromContext[(S, Vector[I])]

      def mergeIslandState(state: S, islandState: S): S
      def migrateToIsland(i: Vector[I], state: S): (Vector[I], S)
      def migrateFromIsland(population: Vector[I], initialState: S, state: S): (Vector[I], S)

      def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean): FromContext[Seq[Variable[?]]]

  import mgo.evolution.algorithm._

  def paired[G, C, D](continuous: G => C, discrete: G => D) = (g: G) => (continuous(g), discrete(g))


