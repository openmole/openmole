/*
 * Copyright (C) 22/11/12 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.builder._
import monocle.macros._
import org.openmole.plugin.task.tools._
import org.openmole.plugin.tool.pattern
import org.openmole.plugin.tool.pattern.MasterSlave
import squants.time.Time

package object evolution {

  val operatorExploration = 0.1

  type Objectives = Seq[Objective[_]]
  type Genome = Seq[Genome.GenomeBound]

  implicit def intToCounterTerminationConverter(n: Long): AfterGeneration = AfterGeneration(n)
  implicit def durationToDurationTerminationConverter(d: Time): AfterDuration = AfterDuration(d)

  object OMTermination {
    def toTermination(oMTermination: OMTermination, integration: EvolutionWorkflow) =
      oMTermination match {
        case AfterGeneration(g) ⇒ (s: integration.S, population: Vector[integration.I]) ⇒ integration.operations.afterGeneration(g, s, population)
        case AfterDuration(d) ⇒ (s: integration.S, population: Vector[integration.I]) ⇒ integration.operations.afterDuration(d, s, population)
      }
  }

  sealed trait OMTermination
  case class AfterGeneration(steps: Long) extends OMTermination
  case class AfterDuration(duration: Time) extends OMTermination

  sealed trait EvolutionPattern

  object EvolutionPattern {
    def build[T](
      algorithm:    T,
      evaluation:   DSL,
      termination:  OMTermination,
      stochastic:   OptionalArgument[Stochastic] = None,
      parallelism:  Int                          = 1,
      distribution: EvolutionPattern             = SteadyState(),
      suggestion:   Seq[Seq[ValueAssignment[_]]],
      scope:        DefinitionScope)(implicit wfi: WorkflowIntegration[T]): DSLContainer[EvolutionWorkflow] =
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

  case class SteadyState(wrap: Boolean = false) extends EvolutionPattern
  case class Island(termination: OMTermination, sample: OptionalArgument[Int] = None) extends EvolutionPattern

  implicit def workflowIntegration = WorkflowIntegration[DSLContainer[EvolutionWorkflow]](_.data)

  implicit class EvolutionMethodContainer(dsl: DSLContainer[EvolutionWorkflow]) extends DSLContainerHook(dsl) {
    def hook[F](output: WritableOutput, frequency: OptionalArgument[Long] = None, last: Boolean = false, format: F = CSVOutputFormat(unrollArray = true))(implicit outputFormat: OutputFormat[F, SavePopulationHook.EvolutionData]): DSLContainer[EvolutionWorkflow] = {
      implicit val defScope = dsl.scope
      dsl.hook(SavePopulationHook(dsl, output, frequency = frequency, last = last, format = format))
    }
  }

  def SteadyStateEvolution[T](algorithm: T, evaluation: DSL, termination: OMTermination, parallelism: Int = 1, suggestion: Seq[Seq[ValueAssignment[_]]] = Seq.empty, wrap: Boolean = false, scope: DefinitionScope = "steady state evolution")(implicit wfi: WorkflowIntegration[T]) = {
    implicit def defScope = scope

    val evolution = wfi(algorithm)

    val wrapped = pattern.wrap(evaluation, evolution.inputPrototypes, evolution.outputPrototypes, wrap)
    val randomGenomes = BreedTask(evolution, parallelism, suggestion) set ((inputs, outputs) += evolution.populationPrototype)

    val scaleGenome = ScalingGenomeTask(evolution)
    val toOffspring = ToOffspringTask(evolution)
    val elitism = ElitismTask(evolution)
    val terminationTask = TerminationTask(evolution, termination)
    val breed = BreedTask(evolution, 1)

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (evolution.populationPrototype, evolution.genomePrototype, evolution.statePrototype),
        (inputs, outputs) += (evolution.outputPrototypes: _*)
      )

    val masterLast =
      EmptyTask() set (
        (inputs, outputs) += (
          evolution.populationPrototype,
          evolution.statePrototype,
          evolution.genomePrototype.toArray,
          evolution.terminatedPrototype,
          evolution.generationPrototype)
      )

    val master =
      ((masterFirst -- toOffspring keep (Seq(evolution.statePrototype, evolution.genomePrototype) ++ evolution.outputPrototypes: _*)) -- elitism -- terminationTask -- breed -- masterLast) &
        (masterFirst -- elitism keep evolution.populationPrototype) &
        (elitism -- breed keep evolution.populationPrototype) &
        (elitism -- masterLast keep evolution.populationPrototype) &
        (terminationTask -- masterLast keep (evolution.terminatedPrototype, evolution.generationPrototype))

    val masterTask = MoleTask(master) set (exploredOutputs += evolution.genomePrototype.toArray)

    val masterSlave =
      MasterSlave(
        randomGenomes,
        master = masterTask,
        slave = scaleGenome -- Strain(wrapped),
        state = Seq(evolution.populationPrototype, evolution.statePrototype),
        slaves = parallelism,
        stop = evolution.terminatedPrototype
      )

    val firstTask = InitialStateTask(evolution)

    val puzzle =
      (Strain(firstTask) -- masterSlave) &
        (firstTask oo wrapped block (evolution.populationPrototype, evolution.statePrototype))

    DSLContainerExtension[EvolutionWorkflow](DSLContainer(puzzle), output = Some(masterTask), delegate = wrapped.delegate, data = evolution)
  }

  def IslandEvolution[T](
    island:      DSLContainer[EvolutionWorkflow],
    parallelism: Int,
    termination: OMTermination,
    sample:      OptionalArgument[Int]           = None,
    scope:       DefinitionScope                 = "island evolution"
  ) = {

    implicit def defScope = scope

    val t = island.data

    val islandPopulationPrototype = t.populationPrototype.withName("islandPopulation")

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (t.populationPrototype, t.offspringPrototype, t.statePrototype)
      )

    val masterLast =
      EmptyTask() set (
        (inputs, outputs) += (t.populationPrototype, t.statePrototype, islandPopulationPrototype.toArray, t.terminatedPrototype, t.generationPrototype)
      )

    val elitism = ElitismTask(t)
    val generateIsland = GenerateIslandTask(t, sample, 1, islandPopulationPrototype)
    val terminationTask = TerminationTask(t, termination)
    val islandPopulationToPopulation = AssignTask(islandPopulationPrototype → t.populationPrototype) set ((inputs, outputs) += t.statePrototype)

    val fromIsland = FromIslandTask(t)

    val populationToOffspring = AssignTask(t.populationPrototype → t.offspringPrototype)

    val master =
      ((masterFirst -- elitism keep (t.statePrototype, t.populationPrototype, t.offspringPrototype)) -- terminationTask -- masterLast keep (t.terminatedPrototype, t.generationPrototype, t.statePrototype)) &
        (elitism -- generateIsland -- masterLast) &
        (elitism -- masterLast keep t.populationPrototype)

    val masterTask = MoleTask(master) set (exploredOutputs += (islandPopulationPrototype.toArray))

    val generateInitialIslands =
      GenerateIslandTask(t, sample, parallelism, islandPopulationPrototype) set (
        (inputs, outputs) += t.statePrototype,
        outputs += t.populationPrototype
      )

    val islandTask = MoleTask(island)

    val slaveFist = EmptyTask() set ((inputs, outputs) += (t.statePrototype, islandPopulationPrototype))

    val slave = slaveFist -- islandPopulationToPopulation -- islandTask -- fromIsland -- populationToOffspring

    val masterSlave = MasterSlave(
      generateInitialIslands,
      masterTask,
      slave,
      state = Seq(t.populationPrototype, t.statePrototype),
      slaves = parallelism,
      stop = t.terminatedPrototype
    )

    val first = InitialStateTask(t)

    val puzzle =
      (Strain(first) -- masterSlave) &
        (first oo islandTask block (t.populationPrototype, t.statePrototype))

    DSLContainerExtension[EvolutionWorkflow](DSLContainer(puzzle), output = Some(masterTask), delegate = Vector(islandTask), data = t)
  }

  // For backward compatibility
  def GenomeProfileEvolution = ProfileEvolution
  def GenomeProfile = Profile

}
