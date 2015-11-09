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
import org.openmole.core.workflow.tools.Condition
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.transition._
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.task.tools._
import org.openmole.plugin.tool.pattern._

import scala.concurrent.duration.Duration
import scala.util.Random

package object evolution {
  type Objective = Prototype[Double]
  type Objectives = Seq[Objective]

  //  implicit def durationToTerminationConverter(d: Duration) = Timed(d)
  implicit def intToCounterTerminationConverter(n: Long) = AfterGeneration(n)

  implicit def seqOfDoubleTuplesToInputsConversion(s: Seq[(Prototype[Double], (Double, Double))]) =
    Inputs(s.map { case (p, (min, max)) ⇒ Scalar(p, min, max) })

  implicit def seqOfStringTuplesToInputsConversion(s: Seq[(Prototype[Double], (String, String))]) =
    Inputs(s.map { case (p, (min, max)) ⇒ Scalar(p, min, max) })

  implicit def seqToInputsConversion[T](s: Seq[Input]) = Inputs(s)

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
      genome: Prototype[algorithm.G],
      individual: Prototype[algorithm.Ind],
      population: Prototype[algorithm.Pop],
      generation: Prototype[Long]) = {
      val _algorithm = algorithm
      val _state = state
      val _genome = genome
      val _individual = individual
      val _population = population
      val _generation = generation

      new Parameters[ALGO] {
        val algorithm = _algorithm
        val state = _state.asInstanceOf[Prototype[algorithm.AlgorithmState]]
        val genome = _genome.asInstanceOf[Prototype[algorithm.G]]
        val individual = _individual.asInstanceOf[Prototype[algorithm.Ind]]
        val population = _population.asInstanceOf[Prototype[algorithm.Pop]]
        val generation = _generation
      }
    }
  }

  trait Parameters[+ALGO <: Algorithm] {
    val algorithm: ALGO

    def state: Prototype[algorithm.AlgorithmState]
    def genome: Prototype[algorithm.G]
    def individual: Prototype[algorithm.Ind]
    def population: Prototype[algorithm.Pop]
    def generation: Prototype[Long]
  }

  def SteadyGA[ALG <: GAAlgorithm](algorithm: ALG, evaluation: Puzzle, parallelism: Int, termination: OMTermination) = {

    val genome = Prototype[algorithm.G]("genome")
    val individual = Prototype[algorithm.Ind]("individual")
    val population = Prototype[algorithm.Pop]("population")
    val offspring = Prototype[algorithm.Pop]("offspring")
    val state = Prototype[algorithm.AlgorithmState]("state")
    val generation = Prototype[Long]("generation")
    val terminated = Prototype[Boolean]("terminated")

    val randomGenomes = RandomGenomesTask(algorithm)(
      parallelism,
      genome,
      state,
      algorithm.randomGenome(algorithm.inputs.size)
    ) set( name := "randomGenome" )


    val scalingGenomeTask = ScalingGAGenomeTask(algorithm)(genome) set (
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

    val breed = BreedTask(algorithm)(1, population, state, genome) set ( name := "breed")

    val scalingIndividualsTask = ScalingGAPopulationTask(algorithm)(population) set ( name := "scalingIndividuals" )

    val masterFirst =
      EmptyTask() set (
        (inputs, outputs) += (population, genome, state),
        (inputs, outputs) += (algorithm.outputPrototypes: _*)
      )

    val masterLast = EmptyTask() set (
      name := "masterLast",
      (inputs, outputs) += (population, state, genome.toArray, terminated))

    val masterFirstCapsule = Capsule(masterFirst)
    val elitismSlot = Slot(elitismTask)
    val masterLastSlot = Slot(masterLast)
    val terminationCapsule = Capsule(terminationTask)
    val breedSlot = Slot(breed)

    val master =
      (masterFirstCapsule --
        (toOffspring keep (Seq(state, genome) ++ algorithm.outputPrototypes: _*)) --
        elitismSlot --
        terminationCapsule --
        breedSlot --
        masterLastSlot) +
          (masterFirstCapsule -- (elitismSlot keep (population, state))) +
          (elitismSlot -- (breedSlot keep population)) +
          (elitismSlot -- (masterLastSlot keep population)) +
          (terminationCapsule -- (masterLastSlot keep (terminated, generation)))

    val masterTask = MoleTask(master) set (
      population := Population.empty,
      exploredOutputs += genome.toArray
      )

    val masterSlave = MasterSlave(randomGenomes, masterTask, population, state)(scalingGenomeTask -- evaluation)

    val firstTask = EmptyTask() set ( name := "first" )
    val firstCapsule = Capsule(firstTask, strain = true)
    val last = EmptyTask() set (name := "last")
    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val puzzle =
      ((firstCapsule -- masterSlave -- scalingIndividualsSlot) >| (Capsule(last, strain = true), trigger = s"${terminated.name} == true")) +
        (firstCapsule oo evaluation)

    val parameters =
      Parameters(algorithm)(
        state,
        genome,
        individual,
        population,
        generation
      )

    val gaPuzzle = OutputPuzzleContainer(puzzle, scalingIndividualsSlot.capsule)
    (gaPuzzle, parameters)

//      scalingIndividualsTask addInput state
//      scalingIndividualsTask addInput generation
//      scalingIndividualsTask addInput terminated
//      scalingIndividualsTask addInput archive
//
//      scalingIndividualsTask addOutput state
//      scalingIndividualsTask addOutput generation
//      scalingIndividualsTask addOutput terminated
//      scalingIndividualsTask addOutput population
//      scalingIndividualsTask addOutput archive
//
//      val terminatedCondition = Condition(terminated.name + " == true")
//
//      def gaParameters =
//        GAParameters[ALG](evolution)(
//          archive.asInstanceOf[Prototype[evolution.A]],
//          genome.asInstanceOf[Prototype[evolution.G]],
//          individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.P, evolution.F]]],
//          population.asInstanceOf[Prototype[Population[evolution.G, evolution.P, evolution.F]]],
//          components.generation
//        )

    }

  def IslandGA[ALG <: Algorithm](parameters: Parameters[ALG]) = {

  }

  //  def IslandGA[AG <: GAAlgorithm](parameters: GAParameters[AG])(
  //    fitness: Puzzle,
  //    island: Int,
  //    termination: GATermination { type G >: parameters.evolution.G; type P >: parameters.evolution.P; type F >: parameters.evolution.F },
  //    sample: Int) = {
  //
  //    val name = "islandGA"
  //
  //    import parameters.evolution
  //    import parameters._
  //
  //    val islandElitism = new Elitism with Termination with Archive with TerminationType {
  //      type G = evolution.G
  //      type P = evolution.P
  //      type A = evolution.A
  //      type F = evolution.F
  //
  //      type STATE = termination.STATE
  //
  //      implicit val stateType = termination.stateType
  //
  //      def initialArchive(implicit rng: Random) = evolution.initialArchive
  //      def archive(a: A, population: Population[G, P, F], offspring: Population[G, P, F])(implicit rng: Random) = evolution.archive(a, population, offspring)
  //      def computeElitism(population: Population[G, P, F], offspring: Population[G, P, F], archive: A)(implicit rng: Random) = evolution.computeElitism(population, offspring, archive)
  //
  //      def initialState = termination.initialState
  //      def terminated(population: Population[G, P, F], terminationState: STATE)(implicit rng: Random) = termination.terminated(population, terminationState)
  //    }
  //
  //    val originalArchive = Prototype[A](name + "OriginalArchive")
  //    val state = Prototype[islandElitism.STATE](name + "State")(islandElitism.stateType)
  //    val generation = Prototype[Int](name + "Generation")
  //    val terminated = Prototype[Boolean](name + "Terminated")
  //
  //    val firstCapsule = StrainerCapsule(EmptyTask() set (_.setName(name + "First")))
  //
  //    val toInidividualsTask = PopulationToIndividualsTask(evolution)(population, individual.toArray) set (_.setName(name + "PopulationToIndividualTask"))
  //
  //    val elitismTask = ElitismTask(islandElitism)(
  //      parameters.population,
  //      parameters.individual.toArray,
  //      parameters.archive) set (_.setName(name + "ElitismTask"))
  //
  //    elitismTask setDefault (population, Population.empty[evolution.G, evolution.P, evolution.F])
  //    elitismTask setDefault (archive, islandElitism.initialArchive(Workspace.rng))
  //    val elitismCaps = MasterCapsule(elitismTask, population, archive)
  //
  //    val terminationTask = TerminationTask(islandElitism)(
  //      population,
  //      archive,
  //      generation,
  //      state,
  //      terminated) set (_.setName(name + "TerminationTask"))
  //
  //    terminationTask setDefault Default.delayed(state, islandElitism.initialState)
  //    terminationTask setDefault (generation, 0)
  //
  //    terminationTask addOutput archive
  //    terminationTask addOutput population
  //    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))
  //
  //    val endCapsule = Slot(StrainerCapsule(EmptyTask() set (_.setName(name + "End"))))
  //
  //    val preIslandTask = EmptyTask() set (_.setName(name + "PreIsland"))
  //    preIslandTask addInput population
  //    preIslandTask addInput archive
  //    preIslandTask addOutput population
  //    preIslandTask addOutput archive
  //
  //    preIslandTask setDefault (population, Population.empty[evolution.G, evolution.P, evolution.F])
  //    preIslandTask setDefault (archive, evolution.initialArchive(Workspace.rng))
  //
  //    val preIslandCapsule = Capsule(preIslandTask)
  //
  //    //val islandTask = MoleTask(name + "MoleTask", model)
  //    //val islandSlot = Slot(model)
  //
  //    val scalingIndividualsTask = ScalingGAPopulationTask(evolution)(population) set { _.setName(name + "ScalingIndividuals") }
  //
  //    scalingIndividualsTask addInput archive
  //    scalingIndividualsTask addInput terminated
  //    scalingIndividualsTask addInput state
  //    scalingIndividualsTask addInput generation
  //    scalingIndividualsTask addOutput archive
  //    scalingIndividualsTask addOutput population
  //    scalingIndividualsTask addOutput terminated
  //    scalingIndividualsTask addOutput state
  //    scalingIndividualsTask addOutput generation
  //    val scalingIndividualsSlot = Slot(scalingIndividualsTask)
  //
  //    val selectIndividualsTask =
  //      SamplePopulationTask(evolution)(
  //        population,
  //        sample) set { _.setName(name + "Breeding") }
  //
  //    selectIndividualsTask addInput archive
  //    selectIndividualsTask addOutput archive
  //
  //    val skel =
  //      firstCapsule -<
  //        (preIslandCapsule, size = Some(island.toString)) --
  //        fitness -- toInidividualsTask --
  //        elitismCaps --
  //        terminationSlot --
  //        scalingIndividualsSlot >| (endCapsule, terminated.name + " == true")
  //
  //    val loop =
  //      scalingIndividualsSlot --
  //        selectIndividualsTask --
  //        preIslandCapsule
  //
  //    val dataChannels =
  //      (firstCapsule oo fitness) +
  //        (firstCapsule -- endCapsule)
  //
  //    val islandParameters =
  //      GAParameters(parameters.evolution)(
  //        parameters.archive,
  //        parameters.genome,
  //        parameters.individual,
  //        parameters.population,
  //        generation
  //      )
  //
  //    val puzzle = OutputPuzzleContainer(skel + loop + dataChannels, scalingIndividualsSlot.capsule)
  //    (puzzle, islandParameters)
  //  }

  //  def GenerationalGA[ALG <: GAAlgorithm](algorithm: ALG)(
  //    fitness: Puzzle,
  //    lambda: Int) = {
  //
  //    val name = "generationalGA"
  //
  //    val cs = components[ALG](name, algorithm)
  //    import cs._
  //
  //    val firstCapsule = StrainerCapsule(firstTask)
  //
  //    val breedTask = ExplorationTask(BreedSampling(algorithm)(population, archive, genome, lambda)) set (_.setName(name + "Breed"))
  //    breedTask.setDefault(population, Population.empty[algorithm.G, algorithm.P, algorithm.F])
  //    breedTask.setDefault(archive, algorithm.initialArchive(Workspace.rng))
  //
  //    breedTask addInput generation
  //    breedTask addInput state
  //
  //    breedTask addOutput population
  //    breedTask addOutput archive
  //    breedTask addOutput generation
  //    breedTask addOutput state
  //
  //    breedTask setDefault (generation, 0)
  //    breedTask setDefault Default.delayed(state, algorithm.initialState)
  //
  //    val breedingCaps = Capsule(breedTask)
  //    val breedingCapsItSlot = Slot(breedingCaps)
  //
  //    val scalingGenomeCaps = Capsule(scalingGenomeTask)
  //    val toIndividualSlot = Slot(toIndividualTask)
  //    val elitismSlot = Slot(elitismTask)
  //
  //    terminationTask addOutput archive
  //    terminationTask addOutput population
  //
  //    val terminationSlot = Slot(StrainerCapsule(terminationTask))
  //    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))
  //    val endSlot = Slot(StrainerCapsule(EmptyTask() set (_.setName(name + "End"))))
  //
  //    val exploration = firstCapsule -- breedingCaps -< scalingGenomeCaps -- (fitness, filter = Block(genome)) -- toIndividualSlot >- elitismSlot -- terminationSlot -- scalingIndividualsSlot -- (endSlot, terminatedCondition, filter = Keep(individual.toArray))
  //
  //    val loop = terminationSlot -- (breedingCapsItSlot, !terminatedCondition)
  //
  //    val dataChannels =
  //      (scalingGenomeCaps -- (toIndividualSlot, filter = Keep(genome))) +
  //        (breedingCaps -- (elitismSlot, filter = Keep(population, archive))) +
  //        (breedingCaps oo (fitness.firstSlot, filter = Block(archive, population, genome.toArray))) +
  //        (breedingCaps -- (endSlot, filter = Block(archive, population, state, generation, terminated, genome.toArray))) +
  //        (breedingCaps -- (terminationSlot, filter = Block(archive, population, genome.toArray)))
  //
  //    val gaPuzzle = OutputPuzzleContainer(exploration + loop + dataChannels, scalingIndividualsSlot.capsule)
  //    (gaPuzzle, cs.gaParameters)
  //  }
  //
  //  def SteadyGA[ALG <: GAAlgorithm](algorithm: ALG)(
  //    fitness: Puzzle,
  //    lambda: Int) = {
  //
  //    val name = "steadyGA"
  //
  //    val cs = components[ALG](name, algorithm)
  //    import cs._
  //
  //    val breedTask = ExplorationTask(BreedSampling(algorithm)(population, archive, genome, lambda)) set (_.setName(name + "Breed"))
  //    breedTask.setDefault(population, Population.empty[algorithm.G, algorithm.P, algorithm.F])
  //    breedTask.setDefault(archive, algorithm.initialArchive(Workspace.rng))
  //
  //    val firstCapsule = StrainerCapsule(firstTask)
  //    val scalingCaps = Capsule(scalingGenomeTask)
  //
  //    val toIndividualSlot = Slot(toIndividualTask)
  //
  //    val toIndividualArrayCaps = StrainerCapsule(ToArrayTask(individual) set (_.setName(name + "IndividualToArray")))
  //
  //    //mergeArchiveTask addParameter (archive -> evolution.initialArchive)
  //    //val mergeArchiveCaps = MasterCapsule(mergeArchiveTask, archive)
  //
  //    elitismTask setDefault (population, Population.empty[algorithm.G, algorithm.P, algorithm.F])
  //    elitismTask setDefault (archive, algorithm.initialArchive(Workspace.rng))
  //    val elitismSlot = Slot(MasterCapsule(elitismTask, population, archive))
  //
  //    terminationTask setDefault Default.delayed(state, algorithm.initialState)
  //    terminationTask setDefault (generation, 0)
  //    terminationTask addOutput archive
  //    terminationTask addOutput population
  //
  //    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))
  //
  //    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))
  //
  //    val steadyBreedingTask = ExplorationTask(BreedSampling(algorithm)(population, archive, genome, 1)) set (_.setName(name + "Breeding"))
  //    val steadyBreedingCaps = Capsule(steadyBreedingTask)
  //
  //    val endCapsule = Slot(StrainerCapsule(EmptyTask() set (_.setName(name + "End"))))
  //
  //    val skel =
  //      firstCapsule --
  //        breedTask -<
  //        scalingCaps --
  //        (fitness, filter = Block(genome)) --
  //        (toIndividualSlot, filter = Keep(algorithm.objectives.map(_.name).toSeq: _*)) --
  //        toIndividualArrayCaps --
  //        elitismSlot --
  //        terminationSlot --
  //        scalingIndividualsSlot >| (endCapsule, terminatedCondition, Block(archive))
  //
  //    val loop =
  //      scalingIndividualsSlot --
  //        steadyBreedingCaps -<-
  //        scalingCaps
  //
  //    val dataChannels =
  //      (scalingCaps -- (toIndividualSlot, filter = Keep(genome))) +
  //        (firstCapsule oo (fitness.firstSlot, filter = Block(archive, population))) +
  //        (firstCapsule -- (endCapsule, filter = Block(archive, population))) +
  //        (firstCapsule oo (elitismSlot, filter = Keep(population, archive)))
  //
  //    val gaPuzzle = OutputPuzzleContainer(skel + loop + dataChannels, scalingIndividualsSlot.capsule)
  //    (gaPuzzle, cs.gaParameters)
  //  }
  //
  //  def IslandSteadyGA[ALG <: GAAlgorithm](algorithm: ALG)(
  //    fitness: Puzzle,
  //    island: Int,
  //    termination: GATermination { type G >: algorithm.G; type P >: algorithm.P; type F >: algorithm.F },
  //    sample: Int) = {
  //
  //    val name = "islandSteadyGA"
  //
  //    val (gaPuzzle, parameters) = SteadyGA[ALG](algorithm)(fitness, 1)
  //    val mt = Capsule(MoleTask(gaPuzzle) set (_.setName(s"${name}IslandTask")))
  //
  //    val (puzzle, islandGA) = IslandGA[ALG](parameters)(
  //      mt,
  //      island = island,
  //      termination = termination.asInstanceOf[GATermination { type G = parameters.evolution.G; type P = parameters.evolution.P; type F = parameters.evolution.F }],
  //      sample = sample)
  //
  //    val islandSteadyPuzzle = OutputEnvironmentPuzzleContainer(puzzle, puzzle.output, mt)
  //    (islandSteadyPuzzle, islandGA)
  //  }
  //

}
