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

import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.plugin.task.tools._
import org.openmole.plugin.tool.pattern._
import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.tool.types._
import shapeless._
import squants.time.Time
import mgo.double2Scalable
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.tools._

import scala.annotation.tailrec

package object evolution {

  val operatorExploration = 0.1

  object Genome {
    def apply(inputs: ScalarOrSequence[_]*) = UniqueGenome(inputs)
  }

  object Objective {
    implicit def valToObjective[T](v: Val[T])(implicit td: ToDouble[T]) = Objective(v, context ⇒ td(context(v)))
  }

  case class Objective(prototype: Val[_], fromContext: Context ⇒ Double)

  type Objectives = Seq[Objective]
  type FitnessAggregation = Seq[Double] ⇒ Double
  type Genome = Seq[ScalarOrSequence[_]]

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

  def SteadyStateEvolution[T](algorithm: T, evaluation: Puzzle, termination: OMTermination, parallelism: Int = 1)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    val evaluationCapsule = Slot(MoleTask(evaluation))

    val randomGenomes =
      BreedTask(algorithm, parallelism) set (
        name := "randomGenome",
        outputs += t.populationPrototype
      )

    val scalingGenomeTask = ScalingGenomeTask(algorithm) set (
      name := "scalingGenome"
    )

    val toOffspring =
      ToOffspringTask(algorithm) set (
        name := "toOffspring"
      )

    val elitismTask = ElitismTask(algorithm) set (name := "elitism")

    val terminationTask = TerminationTask(algorithm, termination) set (name := "termination")

    val breed = BreedTask(algorithm, 1) set (name := "breed")

    val scalingIndividualsTask = ScalingPopulationTask(algorithm) set (name := "scalingIndividuals") set (
      (inputs, outputs) += (t.generationPrototype, t.terminatedPrototype, t.statePrototype),
      outputs += t.populationPrototype
    )

    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val masterFirst =
      EmptyTask() set (
        name := "masterFirst",
        (inputs, outputs) += (t.populationPrototype, t.genomePrototype, t.statePrototype),
        (inputs, outputs) += (t.objectives.map(_.prototype): _*)
      )

    val masterLast =
      EmptyTask() set (
        name := "masterLast",
        (inputs, outputs) += (t.populationPrototype, t.statePrototype, t.genomePrototype.toArray, t.terminatedPrototype, t.generationPrototype)
      )

    val masterFirstCapsule = Capsule(masterFirst)
    val elitismSlot = Slot(elitismTask)
    val masterLastSlot = Slot(masterLast)
    val terminationCapsule = Capsule(terminationTask)
    val breedSlot = Slot(breed)

    val master =
      (masterFirstCapsule --
        (toOffspring keep (Seq(t.statePrototype, t.genomePrototype) ++ t.objectives.map(_.prototype): _*)) --
        elitismSlot --
        terminationCapsule --
        breedSlot --
        masterLastSlot) &
        (masterFirstCapsule -- (elitismSlot keep t.populationPrototype)) &
        (elitismSlot -- (breedSlot keep t.populationPrototype)) &
        (elitismSlot -- (masterLastSlot keep t.populationPrototype)) &
        (terminationCapsule -- (masterLastSlot keep (t.terminatedPrototype, t.generationPrototype)))

    val masterTask = MoleTask(master) set (exploredOutputs += t.genomePrototype.toArray)

    val masterSlave = MasterSlave(
      randomGenomes,
      masterTask,
      scalingGenomeTask -- Strain(evaluationCapsule),
      t.populationPrototype, t.statePrototype
    )

    val firstTask = InitialStateTask(algorithm) set (name := "first")

    val firstCapsule = Capsule(firstTask, strain = true)

    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (t.statePrototype, t.populationPrototype)
    )

    val puzzle =
      ((firstCapsule -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true) when t.terminatedPrototype)) &
        (firstCapsule oo evaluationCapsule)

    val gaPuzzle =
      new OutputEnvironmentPuzzleContainer(puzzle, scalingIndividualsSlot.capsule, evaluationCapsule) {
        def generation = t.generationPrototype
        def population = t.populationPrototype
        def state = t.statePrototype
      }

    gaPuzzle :: algorithm :: HNil
  }

  def IslandEvolution[HL <: HList, T](
    island:      HL,
    parallelism: Int,
    termination: OMTermination,
    sample:      OptionalArgument[Int] = None
  )(implicit
    wfi: WorkflowIntegrationSelector[HL, T],
    selectPuzzle: ops.hlist.Selector[HL, _ <: PuzzleContainer]) = {
    val algorithm: T = wfi(island)
    implicit val wi = wfi.wi
    val t = wi(algorithm)

    val islandPopulationPrototype = t.populationPrototype.withName("islandPopulation")

    val masterFirst =
      EmptyTask() set (
        name := "masterFirst",
        (inputs, outputs) += (t.populationPrototype, t.offspringPrototype, t.statePrototype)
      )

    val masterLast =
      EmptyTask() set (
        name := "masterLast",
        (inputs, outputs) += (t.populationPrototype, t.statePrototype, islandPopulationPrototype.toArray, t.terminatedPrototype, t.generationPrototype)
      )

    val elitismTask = ElitismTask(algorithm) set (name := "elitism")

    val generateIsland = GenerateIslandTask(algorithm, sample, 1, islandPopulationPrototype)

    val terminationTask = TerminationTask(algorithm, termination) set (name := "termination")

    val islandPopulationToPopulation =
      AssignTask(islandPopulationPrototype → t.populationPrototype) set (
        name := "islandPopulationToPopulation"
      )

    val reassingRNGTask = ReassignStateRNGTask(algorithm)

    val fromIsland = FromIslandTask(algorithm)

    val populationToOffspring =
      AssignTask(t.populationPrototype → t.offspringPrototype) set (
        name := "populationToOffspring"
      )

    val elitismSlot = Slot(elitismTask)
    val terminationCapsule = Capsule(terminationTask)
    val masterLastSlot = Slot(masterLast)

    val master =
      (
        masterFirst --
        (elitismSlot keep (t.statePrototype, t.populationPrototype, t.offspringPrototype)) --
        terminationCapsule --
        (masterLastSlot keep (t.terminatedPrototype, t.generationPrototype, t.statePrototype))
      ) &
        (elitismSlot -- generateIsland -- masterLastSlot) &
        (elitismSlot -- (masterLastSlot keep t.populationPrototype))

    val masterTask = MoleTask(master) set (
      name := "islandMaster",
      exploredOutputs += islandPopulationPrototype.toArray
    )

    val generateInitialIslands =
      GenerateIslandTask(algorithm, sample, parallelism, islandPopulationPrototype) set (
        name := "generateInitialIslands",
        (inputs, outputs) += t.statePrototype,
        outputs += t.populationPrototype
      )

    val islandCapsule = Slot(MoleTask(selectPuzzle(island)))

    val slaveFist = EmptyTask() set (
      name := "slaveFirst",
      (inputs, outputs) += (t.statePrototype, islandPopulationPrototype)
    )

    val slave = slaveFist -- (islandPopulationToPopulation, reassingRNGTask) -- islandCapsule -- fromIsland -- populationToOffspring

    val masterSlave = MasterSlave(
      generateInitialIslands,
      masterTask,
      slave,
      t.populationPrototype, t.statePrototype
    )

    val scalingIndividualsTask = ScalingPopulationTask(algorithm) set (name := "scalingIndividuals") set (
      (inputs, outputs) += (t.generationPrototype, t.terminatedPrototype, t.statePrototype),
      outputs += t.populationPrototype
    )

    val firstTask = InitialStateTask(algorithm) set (name := "first")

    val firstCapsule = Capsule(firstTask, strain = true)

    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (t.populationPrototype, t.statePrototype)
    )

    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val puzzle =
      ((firstCapsule -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true) when t.terminatedPrototype)) &
        (firstCapsule oo (islandCapsule, Block(t.populationPrototype, t.statePrototype)))

    val gaPuzzle =
      new OutputEnvironmentPuzzleContainer(puzzle, scalingIndividualsSlot.capsule, islandCapsule) {
        def generation = t.generationPrototype
        def population = t.populationPrototype
        def state = t.statePrototype
      }

    gaPuzzle :: algorithm :: HNil
  }

}
