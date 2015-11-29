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
  type Objective = Prototype[Double]
  type Objectives = Seq[Objective]
  type FitnessAggregation = Seq[TextClosure[Seq[Double], Double]]

  implicit def intToCounterTerminationConverter(n: Long) = AfterGeneration(n)
  implicit def durationToDurationTerminationConverter(d: Duration) = AfterDuration(d)

  object OMTermination {
    def toTermination(oMTermination: OMTermination, integration: EvolutionWorkflow) =
      oMTermination match {
        case AfterGeneration(s) ⇒ afterGeneration[integration.AlgoState](s)(generation[integration.S])
        case AfterDuration(d)   ⇒ afterTime[integration.AlgoState](d)(startTime[integration.S])
      }
  }

  sealed trait OMTermination
  case class AfterGeneration(steps: Long) extends OMTermination
  case class AfterDuration(duration: Duration) extends OMTermination

  def SteadyStateEvolution[T](algorithm: T, evaluation: Puzzle, termination: OMTermination, parallelism: Int = 1)(implicit integration: WorkflowIntegration[T]) = {
    val argAlgo = algorithm
    val wfi = integration(algorithm)
    import wfi._

    val randomGenomes =
      BreedTask(argAlgo, parallelism) set (
        name := "randomGenome",
        outputs += populationPrototype
      )

    val scalingGenomeTask = ScalingGenomeTask(argAlgo) set (
      name := "scalingGenome")

    val toOffspring =
      ToOffspringTask(argAlgo) set (
        name := "toOffspring")

    val elitismTask = ElitismTask(argAlgo) set (name := "elitism")

    val terminationTask = TerminationTask(argAlgo, termination) set (name := "termination")

    val breed = BreedTask(argAlgo, 1) set (name := "breed")

    val scalingIndividualsTask = ScalingPopulationTask(argAlgo) set (name := "scalingIndividuals") set (
      (inputs, outputs) += (generationPrototype, terminatedPrototype, statePrototype),
      outputs += populationPrototype
    )

    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val masterFirst =
      EmptyTask() set (
        name := "masterFirst",
        (inputs, outputs) += (populationPrototype, genomePrototype, statePrototype),
        (inputs, outputs) += (outputPrototypes: _*)
      )

    val masterLast =
      EmptyTask() set (
        name := "masterLast",
        (inputs, outputs) += (populationPrototype, statePrototype, genomePrototype.toArray, terminatedPrototype, generationPrototype)
      )

    val masterFirstCapsule = Capsule(masterFirst)
    val elitismSlot = Slot(elitismTask)
    val masterLastSlot = Slot(masterLast)
    val terminationCapsule = Capsule(terminationTask)
    val breedSlot = Slot(breed)

    val master =
      (masterFirstCapsule --
        (toOffspring keep (Seq(statePrototype, genomePrototype) ++ outputPrototypes: _*)) --
        elitismSlot --
        terminationCapsule --
        breedSlot --
        masterLastSlot) &
        (masterFirstCapsule -- (elitismSlot keep populationPrototype)) &
        (elitismSlot -- (breedSlot keep populationPrototype)) &
        (elitismSlot -- (masterLastSlot keep populationPrototype)) &
        (terminationCapsule -- (masterLastSlot keep (terminatedPrototype, generationPrototype)))

    val masterTask = MoleTask(master) set (exploredOutputs += genomePrototype.toArray)

    val masterSlave = MasterSlave(randomGenomes, masterTask, populationPrototype, statePrototype)(scalingGenomeTask -- Strain(evaluation))

    val firstTask = EmptyTask() set (
      name := "first",
      (inputs, outputs) += (populationPrototype, statePrototype),
      _.setDefault(Default(statePrototype, ctx ⇒ wfi.algorithm.algorithmState(Task.buildRNG(ctx)))),
      populationPrototype := Population.empty)

    val firstCapsule = Capsule(firstTask, strain = true)
    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (statePrototype, populationPrototype)
    )

    val puzzle =
      ((firstCapsule -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true), trigger = terminatedPrototype)) &
        (firstCapsule oo evaluation)

    val gaPuzzle = OutputPuzzleContainer(puzzle, scalingIndividualsSlot.capsule)
    \&/(gaPuzzle, argAlgo)
  }

  def IslandEvolution[T](island: PuzzleContainer \&/ T, parallelism: Int, sample: Int, termination: OMTermination)(implicit workflowIntegration: WorkflowIntegration[T]) = {
    val argAlgo: T = island
    val integration = workflowIntegration(island)
    import integration._

    val islandPopulationPrototype = populationPrototype.withName("islandPopulation")

    val masterFirst =
      EmptyTask() set (
        name := "masterFirst",
        (inputs, outputs) += (populationPrototype, offspringPrototype, statePrototype)
      )

    val masterLast =
      EmptyTask() set (
        name := "masterLast",
        (inputs, outputs) += (populationPrototype, statePrototype, islandPopulationPrototype.toArray, terminatedPrototype, generationPrototype)
      )

    val elitismTask = ElitismTask(argAlgo) set (name := "elitism")

    val generateIsland = SamplePopulationTask(argAlgo, sample, 1, islandPopulationPrototype.name)

    val terminationTask = TerminationTask(argAlgo, termination) set (name := "termination")

    val islandPopulationToPopulation =
      AssignTask(islandPopulationPrototype -> populationPrototype) set (
        name := "islandPopulationToPopulation",
        (inputs, outputs) += statePrototype
      )

    val populationToOffspring =
      AssignTask(populationPrototype -> offspringPrototype) set (
        name := "populationToOffspring"
      )

    val elitismSlot = Slot(elitismTask)
    val terminationCapsule = Capsule(terminationTask)
    val masterLastSlot = Slot(masterLast)

    val master =
      (
        masterFirst --
        (elitismSlot keep (statePrototype, populationPrototype, offspringPrototype)) --
        terminationCapsule --
        (masterLastSlot keep (terminatedPrototype, generationPrototype, statePrototype))
      ) &
        (elitismSlot -- generateIsland -- masterLastSlot) &
        (elitismSlot -- (masterLastSlot keep populationPrototype))

    val masterTask = MoleTask(master) set (
      name := "islandMaster",
      exploredOutputs += islandPopulationPrototype.toArray
    )

    val generateInitialIslands =
      SamplePopulationTask(argAlgo, sample, parallelism, islandPopulationPrototype.name) set (
        name := "generateInitialIslands",
        (inputs, outputs) += statePrototype,
        outputs += populationPrototype
      )

    val islandCapsule = Slot(MoleTask(island))

    val masterSlave = MasterSlave(generateInitialIslands, masterTask, populationPrototype, statePrototype)(islandPopulationToPopulation -- islandCapsule -- populationToOffspring)

    val scalingIndividualsTask = ScalingPopulationTask(argAlgo) set (name := "scalingIndividuals") set (
      (inputs, outputs) += (generationPrototype, terminatedPrototype, statePrototype),
      outputs += populationPrototype
    )

    val firstTask = EmptyTask() set (
      name := "first",
      (inputs, outputs) += (populationPrototype, statePrototype),
      populationPrototype := Population.empty,
      _.setDefault(Default(statePrototype, ctx ⇒ algorithm.algorithmState(Task.buildRNG(ctx))))
    )
    val firstCapsule = Capsule(firstTask, strain = true)
    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (populationPrototype, statePrototype)
    )

    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val puzzle =
      ((firstCapsule -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true), trigger = terminatedPrototype)) &
        (firstCapsule oo (islandCapsule, Block(populationPrototype, statePrototype)))

    val gaPuzzle = OutputEnvironmentPuzzleContainer(puzzle, scalingIndividualsSlot.capsule, islandCapsule)
    \&/(gaPuzzle, argAlgo)
  }

}
