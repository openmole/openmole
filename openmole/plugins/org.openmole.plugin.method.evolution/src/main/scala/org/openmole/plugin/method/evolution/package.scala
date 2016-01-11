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

import fr.iscpif.mgo._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.tools._
import org.openmole.core.workspace.Workspace

import org.openmole.plugin.task.tools._
import org.openmole.plugin.tool.pattern._
import org.openmole.tool.types._

import scala.concurrent.duration.Duration
import scala.util.Random
import scalaz._
import Scalaz._

package object evolution {

  val operatorExploration = 0.1

  type Objective = Prototype[Double]
  type Objectives = Seq[Objective]
  type FitnessAggregation = TextClosure[Seq[Double], Double]
  type Genome = Seq[Input]

  implicit def intToCounterTerminationConverter(n: Long) = AfterGeneration(n)
  implicit def durationToDurationTerminationConverter(d: Duration) = AfterDuration(d)

  object OMTermination {
    def toTermination(oMTermination: OMTermination, integration: EvolutionWorkflow) =
      oMTermination match {
        case AfterGeneration(s) ⇒ integration.integration.afterGeneration(s)
        case AfterDuration(d)   ⇒ integration.integration.afterDuration(d)
      }
  }

  sealed trait OMTermination
  case class AfterGeneration(steps: Long) extends OMTermination
  case class AfterDuration(duration: Duration) extends OMTermination

  def SteadyStateEvolution[T](algorithm: T, evaluation: Puzzle, termination: OMTermination, parallelism: Int = 1)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    val randomGenomes =
      BreedTask(algorithm, parallelism) set (
        name := "randomGenome",
        outputs += t.populationPrototype
      )

    val scalingGenomeTask = ScalingGenomeTask(algorithm) set (
      name := "scalingGenome")

    val toOffspring =
      ToOffspringTask(algorithm) set (
        name := "toOffspring")

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
        (inputs, outputs) += (t.outputPrototypes: _*)
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
        (toOffspring keep (Seq(t.statePrototype, t.genomePrototype) ++ t.outputPrototypes: _*)) --
        elitismSlot --
        terminationCapsule --
        breedSlot --
        masterLastSlot) &
        (masterFirstCapsule -- (elitismSlot keep t.populationPrototype)) &
        (elitismSlot -- (breedSlot keep t.populationPrototype)) &
        (elitismSlot -- (masterLastSlot keep t.populationPrototype)) &
        (terminationCapsule -- (masterLastSlot keep (t.terminatedPrototype, t.generationPrototype)))

    val masterTask = MoleTask(master) set (exploredOutputs += t.genomePrototype.toArray)

    val masterSlave = MasterSlave(randomGenomes, masterTask, t.populationPrototype, t.statePrototype)(scalingGenomeTask -- Strain(evaluation))

    val firstTask = EmptyTask() set (
      name := "first",
      (inputs, outputs) += (t.populationPrototype, t.statePrototype),
      _.setDefault(Default(t.statePrototype, ctx ⇒ t.operations.initialState(Task.buildRNG(ctx)))),
      t.populationPrototype := Vector.empty)

    val firstCapsule = Capsule(firstTask, strain = true)
    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (t.statePrototype, t.populationPrototype)
    )

    val puzzle =
      ((firstCapsule -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true), trigger = t.terminatedPrototype)) &
        (firstCapsule oo evaluation)

    val gaPuzzle =
      new OutputPuzzleContainer(puzzle, scalingIndividualsSlot.capsule) {
        def generation = t.generationPrototype
        def population = t.populationPrototype
        def state = t.statePrototype
      }

    \&/(gaPuzzle, algorithm)
  }

  def IslandEvolution[T](island: PuzzleContainer \&/ T, parallelism: Int, termination: OMTermination, sample: Option[Int] = None)(implicit wfi: WorkflowIntegration[T]) = {
    val algorithm: T = island
    val t = wfi(algorithm)

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

    val generateIsland = GenerateIslandTask(algorithm, sample, 1, islandPopulationPrototype.name)

    val terminationTask = TerminationTask(algorithm, termination) set (name := "termination")

    val islandPopulationToPopulation =
      AssignTask(islandPopulationPrototype -> t.populationPrototype) set (
        name := "islandPopulationToPopulation"
      )

    val reassingRNGTask = ReassignStateRNGTask(algorithm)

    val populationToOffspring =
      AssignTask(t.populationPrototype -> t.offspringPrototype) set (
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
      GenerateIslandTask(algorithm, sample, parallelism, islandPopulationPrototype.name) set (
        name := "generateInitialIslands",
        (inputs, outputs) += t.statePrototype,
        outputs += t.populationPrototype
      )

    val islandCapsule = Slot(MoleTask(island))

    val slaveFist = EmptyTask() set (
      (inputs, outputs) += (t.statePrototype, islandPopulationPrototype)
    )

    val slave = slaveFist -- (islandPopulationToPopulation, reassingRNGTask) -- islandCapsule -- populationToOffspring

    val masterSlave = MasterSlave(generateInitialIslands, masterTask, t.populationPrototype, t.statePrototype)(slave)

    val scalingIndividualsTask = ScalingPopulationTask(algorithm) set (name := "scalingIndividuals") set (
      (inputs, outputs) += (t.generationPrototype, t.terminatedPrototype, t.statePrototype),
      outputs += t.populationPrototype
    )

    val firstTask = EmptyTask() set (
      name := "first",
      (inputs, outputs) += (t.populationPrototype, t.statePrototype),
      t.populationPrototype := Vector.empty,
      _.setDefault(Default(t.statePrototype, ctx ⇒ t.operations.initialState(Task.buildRNG(ctx))))
    )

    val firstCapsule = Capsule(firstTask, strain = true)

    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (t.populationPrototype, t.statePrototype)
    )

    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val puzzle =
      ((firstCapsule -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true), trigger = t.terminatedPrototype)) &
        (firstCapsule oo (islandCapsule, Block(t.populationPrototype, t.statePrototype)))

    val gaPuzzle =
      new OutputEnvironmentPuzzleContainer(puzzle, scalingIndividualsSlot.capsule, islandCapsule) {
        def generation = t.generationPrototype
        def population = t.populationPrototype
        def state = t.statePrototype
      }

    \&/(gaPuzzle, algorithm)
  }

}
