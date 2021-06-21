package org.openmole.plugin.method.evolution

/*
 * Copyright (C) 2021 Romain Reuillon
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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import squants.time._
import org.openmole.plugin.tool.pattern
import org.openmole.plugin.tool.pattern._
import org.openmole.plugin.method.evolution.data._
import org.openmole.plugin.task.tools.AssignTask

object EvolutionDSL {

  object EvolutionPatternContainer {
    implicit def by[T, B](implicit isContainer: EvolutionPatternContainer[T]): EvolutionPatternContainer[By[T, B]] = () ⇒ By.value[T, B] composeLens isContainer()
    implicit def on[T, B](implicit isContainer: EvolutionPatternContainer[T]): EvolutionPatternContainer[On[T, B]] = () ⇒ On.value[T, B] composeLens isContainer()
  }

  trait EvolutionPatternContainer[T] {
    def apply(): monocle.Lens[T, EvolutionPattern]
  }

  object HookContainer {
    implicit def by[T, B](implicit isContainer: HookContainer[T]): HookContainer[By[T, B]] = () ⇒ By.value[T, B] composeLens isContainer()
    implicit def on[T, B](implicit isContainer: HookContainer[T]): HookContainer[On[T, B]] = () ⇒ On.value[T, B] composeLens isContainer()
  }

  trait HookContainer[T] {
    def apply(): monocle.Lens[T, Seq[SavePopulationHook.Parameters[_]]]
  }

  object OMTermination {
    def toTermination(oMTermination: OMTermination, integration: EvolutionWorkflow) =
      oMTermination match {
        case AfterEvaluated(e) ⇒ (s: integration.S, population: Vector[integration.I]) ⇒ integration.operations.afterEvaluated(e, s, population)
        case AfterGeneration(g) ⇒ (s: integration.S, population: Vector[integration.I]) ⇒ integration.operations.afterGeneration(g, s, population)
        case AfterDuration(d) ⇒ (s: integration.S, population: Vector[integration.I]) ⇒ integration.operations.afterDuration(d, s, population)
      }
  }

  sealed trait OMTermination
  case class AfterEvaluated(steps: Long) extends OMTermination
  case class AfterGeneration(steps: Long) extends OMTermination
  case class AfterDuration(duration: Time) extends OMTermination

  object EvolutionPattern {
    def build(
      algorithm:    EvolutionWorkflow,
      evaluation:   DSL,
      termination:  OMTermination,
      parallelism:  Int                          = 1,
      distribution: EvolutionPattern             = SteadyState(),
      suggestion:   Seq[Seq[ValueAssignment[_]]],
      scope:        DefinitionScope): DSLContainer[EvolutionWorkflow] =
      distribution match {
        case s: SteadyState ⇒
          SteadyStateEvolution(
            algorithm = algorithm,
            evaluation = evaluation,
            parallelism = parallelism,
            termination = termination,
            wrap = s.wrap,
            suggestion = suggestion,
            scope = scope
          )
        case i: Island ⇒
          val steadyState =
            SteadyStateEvolution(
              algorithm = algorithm,
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
      }
  }

  sealed trait EvolutionPattern
  case class SteadyState(wrap: Boolean = false) extends EvolutionPattern
  case class Island(termination: OMTermination, sample: OptionalArgument[Int] = None, parallelism: Int = 1) extends EvolutionPattern

  implicit class EvolutionMethodContainer(dsl: DSLContainer[EvolutionWorkflow]) extends DSLContainerHook(dsl) {
    def hook[F](
      output:         WritableOutput,
      frequency:      OptionalArgument[Long] = None,
      last:           Boolean                = false,
      keepAll:        Boolean                = false,
      includeOutputs: Boolean                = true,
      filter:         Seq[Val[_]]            = Vector.empty,
      format:         F                      = CSVOutputFormat(unrollArray = true))(implicit outputFormat: OutputFormat[F, EvolutionMetadata]): DSLContainer[EvolutionWorkflow] = {
      implicit val defScope = dsl.scope
      dsl.hook(SavePopulationHook(dsl.method, output, frequency = frequency, last = last, keepAll = keepAll, includeOutputs = includeOutputs, filter = filter, format = format))
    }
  }

  def SteadyStateEvolution(
    algorithm:   EvolutionWorkflow,
    evaluation:  DSL,
    termination: OMTermination,
    parallelism: Int                          = 1,
    suggestion:  Seq[Seq[ValueAssignment[_]]] = Seq.empty,
    wrap:        Boolean                      = false,
    scope:       DefinitionScope              = "steady state evolution") = {
    implicit def defScope = scope
    val evolution = algorithm

    val wrapped = pattern.wrap(evaluation, evolution.inputVals, evolution.outputVals, wrap)
    val randomGenomes = BreedTask(evolution, parallelism, suggestion) set ((inputs, outputs) += evolution.populationVal)

    val scaleGenome = ScalingGenomeTask(evolution)
    val toOffspring = ToOffspringTask(evolution)
    val elitism = ElitismTask(evolution, evolution.evaluatedVal) set (evolution.evaluatedVal := 1)
    val terminationTask = TerminationTask(evolution, termination)
    val breed = BreedTask(evolution, 1)

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (evolution.populationVal, evolution.genomeVal, evolution.stateVal),
        (inputs, outputs) += (evolution.outputVals: _*)
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
      ((masterFirst -- toOffspring keep (Seq(evolution.stateVal, evolution.genomeVal) ++ evolution.outputVals: _*)) -- elitism -- terminationTask -- breed -- masterLast) &
        (masterFirst -- elitism keep evolution.populationVal) &
        (elitism -- breed keep evolution.populationVal) &
        (elitism -- masterLast keep evolution.populationVal) &
        (terminationTask -- masterLast keep (evolution.terminatedVal, evolution.generationVal))

    val masterTask = MoleTask(master) set (exploredOutputs += evolution.genomeVal.toArray)

    val masterSlave =
      MasterSlave(
        randomGenomes,
        master = masterTask,
        slave = scaleGenome -- Strain(wrapped),
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
      delegate = wrapped.delegate,
      method = evolution,
      validate = evolution.validate)
  }

  def IslandEvolution(
    island:      DSLContainer[EvolutionWorkflow],
    parallelism: Int,
    termination: OMTermination,
    sample:      OptionalArgument[Int]           = None,
    scope:       DefinitionScope                 = "island evolution"
  ) = {

    implicit def defScope = scope

    val t = island.method

    val islandEvaluatedVal = t.generationVal.withName("islandEvaluated")
    val islandPopulationPrototype = t.populationVal.withName("islandPopulation")

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (t.populationVal, t.offspringPopulationVal, t.stateVal, islandEvaluatedVal)
      )

    val masterLast =
      EmptyTask() set (
        (inputs, outputs) += (t.populationVal, t.stateVal, islandPopulationPrototype.toArray, t.terminatedVal)
      )

    val elitism = ElitismTask(t, islandEvaluatedVal)
    val generateIsland = GenerateIslandTask(t, sample, 1, islandPopulationPrototype)
    val terminationTask = TerminationTask(t, termination)
    val islandPopulationToPopulation = AssignTask(islandPopulationPrototype → t.populationVal) set ((inputs, outputs) += t.stateVal)

    val fromIsland = FromIslandTask(t)

    val populationToOffspring = AssignTask(t.populationVal → t.offspringPopulationVal, t.evaluatedVal -> islandEvaluatedVal)

    val master =
      ((masterFirst -- elitism keep (t.stateVal, t.populationVal, t.offspringPopulationVal, islandEvaluatedVal)) -- terminationTask -- masterLast keep (t.terminatedVal, t.stateVal)) &
        (elitism -- generateIsland -- masterLast) &
        (elitism -- masterLast keep t.populationVal)

    val masterTask = MoleTask(master) set (exploredOutputs += (islandPopulationPrototype.toArray))

    val generateInitialIslands =
      GenerateIslandTask(t, sample, parallelism, islandPopulationPrototype) set (
        (inputs, outputs) += t.stateVal,
        outputs += t.populationVal
      )

    val islandTask = MoleTask(island)

    val slaveFist = EmptyTask() set ((inputs, outputs) += (t.stateVal, islandPopulationPrototype))
    val slaveLast = EmptyTask() set ((inputs, outputs) += (t.offspringPopulationVal, islandEvaluatedVal))

    val slave =
      (slaveFist -- islandPopulationToPopulation -- islandTask -- fromIsland -- populationToOffspring -- slaveLast)

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
  }
}
