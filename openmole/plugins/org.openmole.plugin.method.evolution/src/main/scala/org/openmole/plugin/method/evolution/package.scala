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

import scala.concurrent.duration.Duration
import scala.util.Random
import scalaz._
import Scalaz._

package object evolution {
  type Objective = Prototype[Double]
  type Objectives = Seq[Objective]

  implicit def intToCounterTerminationConverter(n: Long) = AfterGeneration(n)

  object OMTermination {
    def toTermination(algorithm: Algorithm)(oMTermination: OMTermination) =
      oMTermination match {
        case AfterGeneration(s) ⇒ afterGeneration[algorithm.AlgorithmState](s)(algorithm.generation)
      }
  }

  sealed trait OMTermination
  case class AfterGeneration(steps: Long) extends OMTermination

  object Parameters {
    def apply[ALGO <: Algorithm](algorithm: ALGO)(
      state: Prototype[algorithm.AlgorithmState],
      population: Prototype[algorithm.Pop],
      generation: Prototype[Long]) = {
      val _algorithm = algorithm
      val _state = state
      val _population = population
      val _generation = generation

      new Parameters[ALGO] {
        val algorithm = _algorithm
        val state = _state.asInstanceOf[Prototype[algorithm.AlgorithmState]]
        val population = _population.asInstanceOf[Prototype[algorithm.Pop]]
        val generation = _generation
      }
    }
  }

  trait Parameters[+ALGO <: Algorithm] {
    val algorithm: ALGO

    def state: Prototype[algorithm.AlgorithmState]
    def population: Prototype[algorithm.Pop]
    def generation: Prototype[Long]
  }

  def SteadyStateEvolution[ALG <: Algorithm](algorithm: ALG, evaluation: Puzzle, parallelism: Int, termination: OMTermination)(implicit toVariable: WorkflowIntegration[ALG]) = {
    val genome = Prototype[algorithm.G]("genome")(toVariable.genomeType(algorithm))
    val individual = Prototype[algorithm.Ind]("individual")(toVariable.individualType(algorithm))
    val population = Prototype[algorithm.Pop]("population")(toVariable.populationType(algorithm))
    val offspring = Prototype[algorithm.Pop]("offspring")(toVariable.populationType(algorithm))
    val state = Prototype[algorithm.AlgorithmState]("state")
    val generation = Prototype[Long]("generation")
    val terminated = Prototype[Boolean]("terminated")

    def randomGenomeGenerator = toVariable.randomGenome(algorithm)

    val randomGenomes = BreedTask(algorithm)(
      parallelism,
      randomGenomeGenerator,
      population,
      state,
      genome) set (
        name := "randomGenome",
        outputs += population
      )

    val scalingGenomeTask = ScalingGenomeTask(algorithm)(genome) set (
      name := "scalingGenome")

    val toOffspring =
      ToOffspringTask(algorithm)(genome, offspring, state) set (
       name := "toOffspring")

    val elitismTask =
      ElitismTask(algorithm)(
        population,
        offspring,
        state) set (name := "elitism")

    val terminationTask = TerminationTask(algorithm)(
      OMTermination.toTermination(algorithm)(termination),
      state,
      generation,
      terminated) set ( name := "termination")

    val breed = BreedTask(algorithm)(1, randomGenomeGenerator, population, state, genome) set ( name := "breed" )

    val scalingIndividualsTask = ScalingPopulationTask(algorithm)(population) set ( name := "scalingIndividuals" ) set (
        (inputs, outputs) += (generation, terminated, state),
        outputs += population
      )

    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val masterFirst =
      EmptyTask() set (
        name := "masterFirst",
        (inputs, outputs) += (population, genome, state),
        (inputs, outputs) += (toVariable.outputPrototypes(algorithm): _*)
      )

    val masterLast =
      EmptyTask() set (
        name := "masterLast",
        (inputs, outputs) += (population, state, genome.toArray, terminated, generation)
      )

    val masterFirstCapsule = Capsule(masterFirst)
    val elitismSlot = Slot(elitismTask)
    val masterLastSlot = Slot(masterLast)
    val terminationCapsule = Capsule(terminationTask)
    val breedSlot = Slot(breed)

    val master =
      (masterFirstCapsule --
        (toOffspring keep (Seq(state, genome) ++ toVariable.outputPrototypes(algorithm): _*)) --
        elitismSlot --
        terminationCapsule --
        breedSlot --
        masterLastSlot) &
          (masterFirstCapsule -- (elitismSlot keep population)) &
          (elitismSlot -- (breedSlot keep population)) &
          (elitismSlot -- (masterLastSlot keep population)) &
          (terminationCapsule -- (masterLastSlot keep (terminated, generation)))

    val masterTask = MoleTask(master) set ( exploredOutputs += genome.toArray )

    val masterSlave = MasterSlave(randomGenomes, masterTask, population, state)(scalingGenomeTask -- Strain(evaluation))

    val firstTask = EmptyTask() set (
      name := "first",
      (inputs, outputs) += (population, state),
      _.setDefault(Default(state, ctx ⇒ algorithm.algorithmState(Task.buildRNG(ctx)))),
      population := Population.empty)

    val firstCapsule = Capsule(firstTask, strain = true)
    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (state, population)
      )

    val puzzle =
      ((firstCapsule -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true), trigger = terminated)) &
        (firstCapsule oo evaluation)

    val parameters =
      Parameters(algorithm)(
        state,
        population,
        generation
      )

    val gaPuzzle = OutputPuzzleContainer(puzzle, scalingIndividualsSlot.capsule)
    \&/(gaPuzzle, parameters)
  }

  def IslandEvolution[ALG <: Algorithm](island: PuzzleContainer \&/ Parameters[ALG], parallelism: Int, sample: Int, termination: OMTermination)(implicit toVariable: WorkflowIntegration[ALG]) = {
    val algorithm = island.algorithm

    val islandPopulation = Prototype[algorithm.Pop]("islandPopulation")(toVariable.populationType(algorithm))
    val population = Prototype[algorithm.Pop]("population")(toVariable.populationType(algorithm))
    val state = Prototype[algorithm.AlgorithmState]("state")

    val terminated = Prototype[Boolean]("terminated")
    val generation = Prototype[Long]("generation")

    val masterFirst =
      EmptyTask() set (
        name := "masterFirst",
        (inputs, outputs) += (population, islandPopulation, state)
      )

    val masterLast =
      EmptyTask() set (
        name := "masterLast",
        (inputs, outputs) += (population, state, islandPopulation.toArray, terminated, generation)
      )

    val elitismTask =
      ElitismTask(algorithm)(
        population,
        islandPopulation,
        state) set (name := "elitism")

    val generateIsland = SamplePopulationTask(algorithm)(islandPopulation, sample, 1)

    val terminationTask = TerminationTask(algorithm)(
      OMTermination.toTermination(algorithm)(termination),
      state,
      generation,
      terminated) set ( name := "termination")

    val populationToIslandPopulation =
      AssignTask(island.population.asInstanceOf[Prototype[algorithm.Pop]] -> islandPopulation) set (
        name := "populationToIslandPopulation"
        )

    val preIslandIslandPopulationToPopulation =
      AssignTask(islandPopulation -> island.population.asInstanceOf[Prototype[algorithm.Pop]]) set (
        name := "preIslandIslandPopulationToPopulation",
        (inputs, outputs) += state
        )

    val elitismSlot = Slot(elitismTask)
    val terminationCapsule = Capsule(terminationTask)
    val masterLastSlot = Slot(masterLast)
    val generateIslandSlot = Slot(generateIsland)

    val master =
      (masterFirst --
        (elitismSlot keep (state, population, islandPopulation)) --
        terminationCapsule --
        generateIslandSlot --
        masterLastSlot) &
          (elitismSlot -- populationToIslandPopulation -- (generateIslandSlot keep islandPopulation)) &
          (elitismSlot -- (masterLastSlot keep population)) &
          (terminationCapsule -- (masterLastSlot keep (terminated, generation, state)))

    val masterTask = MoleTask(master) set (
      name := "islandMaster",
      exploredOutputs += islandPopulation.toArray
    )

    val initialIslands =
      Slot(
        SamplePopulationTask(algorithm)(islandPopulation, sample, parallelism) set (
          name := "initialIslands",
          (inputs, outputs) += (state, population)
        )
      )

    val islandCapsule = Slot(MoleTask(island))

    val masterSlave = MasterSlave(initialIslands, masterTask, population, state)(preIslandIslandPopulationToPopulation -- islandCapsule -- populationToIslandPopulation)

    val scalingIndividualsTask = ScalingPopulationTask(algorithm)(population) set ( name := "scalingIndividuals" ) set (
      (inputs, outputs) += (generation, terminated, state),
      outputs += population
    )

    val firstTask = EmptyTask() set (
      name := "first",
      (inputs, outputs) += (population, state),
      population := Population.empty,
      _.setDefault(Default(state, ctx ⇒ algorithm.algorithmState(Task.buildRNG(ctx))))
    )
    val firstCapsule = Capsule(firstTask, strain = true)
    val last = EmptyTask() set (
      name := "last",
      (inputs, outputs) += (population, state)
      )

    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val initialPopulationToIslandPopulation =
      AssignTask(island.population.asInstanceOf[Prototype[algorithm.Pop]] -> islandPopulation) set (
        name := "initialPopulationToIslandPopulation",
        (inputs, outputs) += state
        )

    val puzzle =
      ((firstCapsule -- initialPopulationToIslandPopulation -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true), trigger = terminated)) &
        (firstCapsule -- (initialIslands keep population)) &
        (firstCapsule oo (islandCapsule, Block(population, state)))


    val parameters =
      Parameters(algorithm)(
        state,
        population,
        generation
      )

    val gaPuzzle = OutputEnvironmentPuzzleContainer(puzzle, scalingIndividualsSlot.capsule, islandCapsule)
    \&/(gaPuzzle, parameters)
  }

}
