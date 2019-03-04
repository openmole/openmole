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
import org.openmole.core.workflow.builder._
import org.openmole.plugin.task.tools._
import org.openmole.plugin.tool.pattern
import org.openmole.plugin.tool.pattern.MasterSlave
import squants.time.Time

package object evolution {

  implicit def scope = DefinitionScope.Internal

  val operatorExploration = 0.1

  type Objectives = Seq[Objective[_]]
  type Genome = Seq[Genome.GenomeBound]

  case class Aggregate[A, B](value: A, aggregate: B)

  implicit class AggregateDecorator[A, B](a: A) {
    def aggregate[B, C](b: Vector[B] ⇒ C) = Aggregate(a, b)
  }

  implicit def intToCounterTerminationConverter(n: Long): AfterGeneration = AfterGeneration(n)
  implicit def durationToDurationTerminationConverter(d: Time): AfterDuration = AfterDuration(d)

  object OMTermination {
    def toTermination(oMTermination: OMTermination, integration: EvolutionWorkflow) =
      oMTermination match {
        case AfterGeneration(s) ⇒ (population: Vector[integration.I]) ⇒ integration.operations.afterGeneration(s, population)
        case AfterDuration(d) ⇒ (population: Vector[integration.I]) ⇒ integration.operations.afterDuration(d, population)
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
      suggestion:   Seq[Seq[ValueAssignment[_]]])(implicit wfi: WorkflowIntegration[T]) =
      distribution match {
        case s: SteadyState ⇒
          SteadyStateEvolution(
            algorithm = algorithm,
            evaluation = evaluation,
            parallelism = parallelism,
            termination = termination,
            wrap = s.wrap,
            suggestion = suggestion
          )
        case i: Island ⇒
          val steadyState =
            SteadyStateEvolution(
              algorithm = algorithm,
              evaluation = evaluation,
              termination = i.termination,
              wrap = false,
              suggestion = suggestion
            )

          IslandEvolution(
            island = steadyState,
            parallelism = parallelism,
            termination = termination,
            sample = i.sample
          )
      }

  }

  case class SteadyState(wrap: Boolean = false) extends EvolutionPattern
  case class Island(termination: OMTermination, sample: OptionalArgument[Int] = None) extends EvolutionPattern

  import shapeless._

  def SteadyStateEvolution[T](algorithm: T, evaluation: DSL, termination: OMTermination, parallelism: Int = 1, suggestion: Seq[Seq[ValueAssignment[_]]] = Seq.empty, wrap: Boolean = false)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    val wrapped = pattern.wrap(evaluation, t.inputPrototypes, t.objectivePrototypes, wrap)
    val randomGenomes = BreedTask[T](algorithm, parallelism, suggestion) set ((inputs, outputs) += t.populationPrototype)

    val scaleGenome = ScalingGenomeTask(algorithm)
    val toOffspring = ToOffspringTask(algorithm)
    val elitism = ElitismTask(algorithm)
    val terminationTask = TerminationTask(algorithm, termination)
    val breed = BreedTask(algorithm, 1)

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (t.populationPrototype, t.genomePrototype, t.statePrototype),
        (inputs, outputs) += (t.objectivePrototypes: _*)
      )

    val masterLast =
      EmptyTask() set (
        (inputs, outputs) += (t.populationPrototype, t.statePrototype, t.genomePrototype.toArray, t.terminatedPrototype, t.generationPrototype)
      )

    val master =
      ((masterFirst -- toOffspring keep (Seq(t.statePrototype, t.genomePrototype) ++ t.objectivePrototypes: _*)) -- elitism -- terminationTask -- breed -- masterLast) &
        (masterFirst -- elitism keep t.populationPrototype) &
        (elitism -- breed keep t.populationPrototype) &
        (elitism -- masterLast keep t.populationPrototype) &
        (terminationTask -- masterLast keep (t.terminatedPrototype, t.generationPrototype))

    val masterTask = MoleTask(master) set (exploredOutputs += t.genomePrototype.toArray)

    val masterSlave = MasterSlave(
      randomGenomes,
      masterTask,
      scaleGenome -- Strain(wrapped),
      state = Seq(t.populationPrototype, t.statePrototype),
      slaves = parallelism
    )

    val firstTask = InitialStateTask(algorithm)

    val last = EmptyTask() set ((inputs, outputs) += (t.statePrototype, t.populationPrototype))

    val puzzle =
      (Strain(firstTask) -- (masterSlave >| Strain(last) when t.terminatedPrototype)) &
        (firstTask oo wrapped block (t.populationPrototype, t.statePrototype))

    val gaDSL = DSLContainer(puzzle, output = Some(masterTask), delegate = wrapped.delegate)

    gaDSL :: algorithm :: HNil
  }

  def IslandEvolution[HL <: HList, T](
    island:      HL,
    parallelism: Int,
    termination: OMTermination,
    sample:      OptionalArgument[Int] = None
  )(implicit
    wfi: WorkflowIntegrationSelector[HL, T],
    selectDSL: DSLSelector[HL]) = {
    val algorithm: T = wfi(island)
    implicit val wi = wfi.selected
    val t = wi(algorithm)

    val islandPopulationPrototype = t.populationPrototype.withName("islandPopulation")

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (t.populationPrototype, t.offspringPrototype, t.statePrototype)
      )

    val masterLast =
      EmptyTask() set (
        (inputs, outputs) += (t.populationPrototype, t.statePrototype, islandPopulationPrototype.toArray, t.terminatedPrototype, t.generationPrototype)
      )

    val elitism = ElitismTask(algorithm)
    val generateIsland = GenerateIslandTask(algorithm, sample, 1, islandPopulationPrototype)
    val terminationTask = TerminationTask(algorithm, termination)
    val islandPopulationToPopulation = AssignTask(islandPopulationPrototype → t.populationPrototype)
    val reassingRNG = ReassignStateRNGTask(algorithm)

    val fromIsland = FromIslandTask(algorithm)

    val populationToOffspring = AssignTask(t.populationPrototype → t.offspringPrototype)

    val master =
      ((masterFirst -- elitism keep (t.statePrototype, t.populationPrototype, t.offspringPrototype)) -- terminationTask -- masterLast keep (t.terminatedPrototype, t.generationPrototype, t.statePrototype)) &
        (elitism -- generateIsland -- masterLast) &
        (elitism -- masterLast keep t.populationPrototype)

    val masterTask = MoleTask(master) set (exploredOutputs += (islandPopulationPrototype.toArray))

    val generateInitialIslands =
      GenerateIslandTask(algorithm, sample, parallelism, islandPopulationPrototype) set (
        (inputs, outputs) += t.statePrototype,
        outputs += t.populationPrototype
      )

    val islandTask = MoleTask(selectDSL(island))

    val slaveFist = EmptyTask() set ((inputs, outputs) += (t.statePrototype, islandPopulationPrototype))

    val slave = slaveFist -- (islandPopulationToPopulation, reassingRNG) -- islandTask -- fromIsland -- populationToOffspring

    val masterSlave = MasterSlave(
      generateInitialIslands,
      masterTask,
      slave,
      state = Seq(t.populationPrototype, t.statePrototype),
      slaves = parallelism
    )

    val first = InitialStateTask(algorithm)

    val last = EmptyTask() set ((inputs, outputs) += (t.populationPrototype, t.statePrototype))

    val puzzle =
      (Strain(first) -- masterSlave >| Strain(last) when t.terminatedPrototype) &
        (first oo islandTask block (t.populationPrototype, t.statePrototype))

    val gaPuzzle = DSLContainer(puzzle, output = Some(masterTask), delegate = Vector(islandTask))

    gaPuzzle :: algorithm :: HNil
  }

}
