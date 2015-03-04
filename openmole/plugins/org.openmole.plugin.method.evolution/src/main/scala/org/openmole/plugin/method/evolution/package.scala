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
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.task.tools._

import scala.concurrent.duration.Duration
import scala.util.Random

package object evolution {
  type Objective = Prototype[Double]
  type Objectives = Seq[Objective]

  implicit def durationToTerminationConverter(d: Duration) = Timed(d)
  implicit def intToCounterTerminationConverter(n: Int) = Counter(n)

  implicit def seqOfDoubleTuplesToInputsConversion(s: Seq[(Prototype[Double], (Double, Double))]) =
    Inputs(s.map { case (p, (min, max)) ⇒ Scalar(p, min, max) })

  implicit def seqOfStringTuplesToInputsConversion(s: Seq[(Prototype[Double], (String, String))]) =
    Inputs(s.map { case (p, (min, max)) ⇒ Scalar(p, min, max) })

  implicit def seqToInputsConversion[T](s: Seq[Input]) = Inputs(s)

  implicit def gaPuzzleToGAParameters[ALG <: GAAlgorithm](ga: GAPuzzle[ALG]) = ga.parameters

  object GAParameters {
    def apply[ALG <: GAAlgorithm](evolution: ALG)(
      archive: Prototype[evolution.A],
      genome: Prototype[evolution.G],
      individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]],
      population: Prototype[Population[evolution.G, evolution.P, evolution.F]],
      generation: Prototype[Int]) = {
      val _evolution = evolution
      val _archive = archive
      val _genome = genome
      val _individual = individual
      val _population = population
      val _generation = generation

      new GAParameters[ALG] {
        val evolution = _evolution
        val archive = _archive.asInstanceOf[Prototype[evolution.A]]
        val genome = _genome.asInstanceOf[Prototype[evolution.G]]
        val individual = _individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.P, evolution.F]]]
        val population = _population.asInstanceOf[Prototype[Population[evolution.G, evolution.P, evolution.F]]]
        val generation = _generation
      }
    }
  }

  trait GAParameters[+ALG <: GAAlgorithm] {
    val evolution: ALG

    def archive: Prototype[evolution.A]
    def genome: Prototype[evolution.G]
    def individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]]
    def population: Prototype[Population[evolution.G, evolution.P, evolution.F]]
    def generation: Prototype[Int]
  }

  case class GAPuzzle[+ALG <: GAAlgorithm](parameters: GAParameters[ALG], puzzle: Puzzle, output: Capsule) {
    def map(f: Puzzle ⇒ Puzzle) = GAPuzzle[ALG](parameters, f(puzzle), output)
  }

  private def components[ALG <: GAAlgorithm](
    name: String,
    evolution: ALG)(implicit plugins: PluginSet) = new { components ⇒
    import evolution._

    val genome = Prototype[evolution.G](name + "Genome")(gManifest)
    val individual = Prototype[Individual[evolution.G, evolution.P, evolution.F]](name + "Individual")
    val population = Prototype[Population[evolution.G, evolution.P, evolution.F]](name + "Population")
    val archive = Prototype[evolution.A](name + "Archive")
    val state = Prototype[evolution.STATE](name + "State")
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstTask = EmptyTask() set (_.setName(name + "First"))
    firstTask addInput (Data(archive, Optional))
    firstTask addInput (Data(population, Optional))
    firstTask addOutput (Data(archive, Optional))
    firstTask addOutput (Data(population, Optional))

    val scalingGenomeTask = ScalingGAGenomeTask(evolution)(genome) set (_.setName(name + "ScalingGenome"))

    val toIndividualTask = ToIndividualTask(evolution)(genome, individual) set (_.setName(name + "ToIndividual"))

    val elitismTask =
      ElitismTask(evolution)(
        population,
        individual.toArray,
        archive) set (_.setName(name + "ElitismTask"))

    val terminationTask = TerminationTask(evolution)(
      population,
      archive,
      generation,
      state,
      terminated) set (_.setName(name + "TerminationTask"))

    val scalingIndividualsTask = ScalingGAPopulationTask(evolution)(population) set (_.setName(name + "ScalingIndividuals"))

    scalingIndividualsTask addInput state
    scalingIndividualsTask addInput generation
    scalingIndividualsTask addInput terminated
    scalingIndividualsTask addInput archive

    scalingIndividualsTask addOutput state
    scalingIndividualsTask addOutput generation
    scalingIndividualsTask addOutput terminated
    scalingIndividualsTask addOutput population
    scalingIndividualsTask addOutput archive

    val terminatedCondition = Condition(terminated.name + " == true")

    def gaPuzzle(puzzle: Puzzle, output: Capsule) =
      GAPuzzle(
        GAParameters[ALG](evolution)(
          archive.asInstanceOf[Prototype[evolution.A]],
          genome.asInstanceOf[Prototype[evolution.G]],
          individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.P, evolution.F]]],
          population.asInstanceOf[Prototype[Population[evolution.G, evolution.P, evolution.F]]],
          //state,
          components.generation
        ),
        puzzle,
        output
      )

  }

  def GenerationalGA[ALG <: GAAlgorithm](algorithm: ALG)(
    fitness: Puzzle,
    lambda: Int)(implicit plugins: PluginSet) = {

    val name = "generationalGA"

    val cs = components[ALG](name, algorithm)
    import cs._

    val firstCapsule = StrainerCapsule(firstTask)

    val breedTask = ExplorationTask(BreedSampling(algorithm)(population, archive, genome, lambda)) set (_.setName(name + "Breed"))
    breedTask.setDefault(population, Population.empty[algorithm.G, algorithm.P, algorithm.F])
    breedTask.setDefault(archive, algorithm.initialArchive(Workspace.rng))

    breedTask addInput generation
    breedTask addInput state

    breedTask addOutput population
    breedTask addOutput archive
    breedTask addOutput generation
    breedTask addOutput state

    breedTask setDefault (generation, 0)
    breedTask setDefault Default.delayed(state, algorithm.initialState)

    val breedingCaps = Capsule(breedTask)
    val breedingCapsItSlot = Slot(breedingCaps)

    val scalingGenomeCaps = Capsule(scalingGenomeTask)
    val toIndividualSlot = Slot(toIndividualTask)
    val elitismSlot = Slot(elitismTask)

    terminationTask addOutput archive
    terminationTask addOutput population

    val terminationSlot = Slot(StrainerCapsule(terminationTask))
    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))
    val endSlot = Slot(StrainerCapsule(EmptyTask() set (_.setName(name + "End"))))

    val exploration = firstCapsule -- breedingCaps -< scalingGenomeCaps -- (fitness, filter = Block(genome)) -- toIndividualSlot >- elitismSlot -- terminationSlot -- scalingIndividualsSlot -- (endSlot, terminatedCondition, filter = Keep(individual.toArray))

    val loop = terminationSlot -- (breedingCapsItSlot, !terminatedCondition)

    val dataChannels =
      (scalingGenomeCaps -- (toIndividualSlot, filter = Keep(genome))) +
        (breedingCaps -- (elitismSlot, filter = Keep(population, archive))) +
        (breedingCaps oo (fitness.first, filter = Block(archive, population, genome.toArray))) +
        (breedingCaps -- (endSlot, filter = Block(archive, population, state, generation, terminated, genome.toArray))) +
        (breedingCaps -- (terminationSlot, filter = Block(archive, population, genome.toArray)))

    val gaPuzzle = exploration + loop + dataChannels

    cs.gaPuzzle(gaPuzzle, scalingIndividualsSlot.capsule)
  }

  def SteadyGA[ALG <: GAAlgorithm](algorithm: ALG)(
    fitness: Puzzle,
    lambda: Int = 1)(implicit plugins: PluginSet) = {

    val name = "steadyGA"

    val cs = components[ALG](name, algorithm)
    import cs._

    val breedTask = ExplorationTask(BreedSampling(algorithm)(population, archive, genome, lambda)) set (_.setName(name + "Breed"))
    breedTask.setDefault(population, Population.empty[algorithm.G, algorithm.P, algorithm.F])
    breedTask.setDefault(archive, algorithm.initialArchive(Workspace.rng))

    val firstCapsule = StrainerCapsule(firstTask)
    val scalingCaps = Capsule(scalingGenomeTask)

    val toIndividualSlot = Slot(toIndividualTask)

    val toIndividualArrayCaps = StrainerCapsule(ToArrayTask(individual) set (_.setName(name + "IndividualToArray")))

    //mergeArchiveTask addParameter (archive -> evolution.initialArchive)
    //val mergeArchiveCaps = MasterCapsule(mergeArchiveTask, archive)

    elitismTask setDefault (population, Population.empty[algorithm.G, algorithm.P, algorithm.F])
    elitismTask setDefault (archive, algorithm.initialArchive(Workspace.rng))
    val elitismSlot = Slot(MasterCapsule(elitismTask, population, archive))

    terminationTask setDefault Default.delayed(state, algorithm.initialState)
    terminationTask setDefault (generation, 0)
    terminationTask addOutput archive
    terminationTask addOutput population

    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))

    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))

    val steadyBreedingTask = ExplorationTask(BreedSampling(algorithm)(population, archive, genome, 1)) set (_.setName(name + "Breeding"))
    val steadyBreedingCaps = Capsule(steadyBreedingTask)

    val endCapsule = Slot(StrainerCapsule(EmptyTask() set (_.setName(name + "End"))))

    val skel =
      firstCapsule --
        breedTask -<
        scalingCaps --
        (fitness, filter = Block(genome)) --
        (toIndividualSlot, filter = Keep(algorithm.objectives.map(_.name).toSeq: _*)) --
        toIndividualArrayCaps --
        elitismSlot --
        terminationSlot --
        scalingIndividualsSlot >| (endCapsule, terminatedCondition, Block(archive))

    val loop =
      scalingIndividualsSlot --
        steadyBreedingCaps -<-
        scalingCaps

    val dataChannels =
      (scalingCaps -- (toIndividualSlot, filter = Keep(genome))) +
        (firstCapsule oo (fitness.first, filter = Block(archive, population))) +
        (firstCapsule -- (endCapsule, filter = Block(archive, population))) +
        (firstCapsule oo (elitismSlot, filter = Keep(population, archive)))

    val gaPuzzle = skel + loop + dataChannels

    cs.gaPuzzle(gaPuzzle, scalingIndividualsSlot.capsule)
  }

  def IslandSteadyGA[ALG <: GAAlgorithm](algorithm: ALG, fitness: Puzzle)(
    number: Int,
    termination: GATermination { type G >: algorithm.G; type P >: algorithm.P; type F >: algorithm.F },
    samples: Int)(implicit plugins: PluginSet) = {

    val name = "islandSteadyGA"

    val gaPuzzle: GAPuzzle[ALG] =
      SteadyGA[ALG](algorithm)(
        fitness = fitness
      ).map(
          p ⇒ Capsule(MoleTask(p) set (_.setName(s"${name}IslandTask")))
        )

    IslandGA[ALG](gaPuzzle)(
      number = number,
      termination = termination.asInstanceOf[GATermination { type G = gaPuzzle.parameters.evolution.G; type P = gaPuzzle.parameters.evolution.P; type F = gaPuzzle.parameters.evolution.F }],
      samples = samples) -> gaPuzzle.puzzle.last
  }

  def IslandGA[AG <: GAAlgorithm](fitness: GAPuzzle[AG])(
    number: Int,
    termination: GATermination { type G >: fitness.parameters.evolution.G; type P >: fitness.parameters.evolution.P; type F >: fitness.parameters.evolution.F },
    samples: Int)(implicit plugins: PluginSet) = {

    val name = "islandGA"

    import fitness.parameters
    import fitness.parameters.evolution
    import parameters._

    val islandElitism = new Elitism with Termination with Archive with TerminationManifest {
      type G = evolution.G
      type P = evolution.P
      type A = evolution.A
      type F = evolution.F

      type STATE = termination.STATE

      implicit val stateManifest = termination.stateManifest

      def initialArchive(implicit rng: Random) = evolution.initialArchive
      def archive(a: A, population: Population[G, P, F], offspring: Population[G, P, F])(implicit rng: Random) = evolution.archive(a, population, offspring)
      def computeElitism(population: Population[G, P, F], offspring: Population[G, P, F], archive: A)(implicit rng: Random) = evolution.computeElitism(population, offspring, archive)

      def initialState = termination.initialState
      def terminated(population: Population[G, P, F], terminationState: STATE)(implicit rng: Random) = termination.terminated(population, terminationState)
    }

    //val archive = parameters.archive.asInstanceOf[Prototype[A]]
    val originalArchive = Prototype[A](name + "OriginalArchive")

    //val individual = parameters.individual.asInstanceOf[Prototype[Individual[G, P, F]]]
    //val population = parameters.population //Prototype[Population[G, P, F]](name + "Population")

    val state = Prototype[islandElitism.STATE](name + "State")(islandElitism.stateManifest)
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstCapsule = StrainerCapsule(EmptyTask() set (_.setName(name + "First")))

    val toInidividualsTask = PopulationToIndividualsTask(evolution)(population, individual.toArray) set (_.setName(name + "PopulationToIndividualTask"))
    //val renameIndividualsTask = AssignTask(name + "RenameIndividuals")
    //renameIndividualsTask.assign(individual.toArray, newIndividual.toArray)

    val elitismTask = ElitismTask(islandElitism)(
      parameters.population,
      parameters.individual.toArray,
      parameters.archive) set (_.setName(name + "ElitismTask"))

    elitismTask setDefault (population, Population.empty[evolution.G, evolution.P, evolution.F])
    elitismTask setDefault (archive, islandElitism.initialArchive(Workspace.rng))
    val elitismCaps = MasterCapsule(elitismTask, population, archive)

    val terminationTask = TerminationTask(islandElitism)(
      population,
      archive,
      generation,
      state,
      terminated) set (_.setName(name + "TerminationTask"))

    terminationTask setDefault Default.delayed(state, islandElitism.initialState)
    terminationTask setDefault (generation, 0)

    terminationTask addOutput archive
    terminationTask addOutput population
    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))

    val endCapsule = Slot(StrainerCapsule(EmptyTask() set (_.setName(name + "End"))))

    val preIslandTask = EmptyTask() set (_.setName(name + "PreIsland"))
    preIslandTask addInput population
    preIslandTask addInput archive
    preIslandTask addOutput population
    preIslandTask addOutput archive

    preIslandTask setDefault (population, Population.empty[evolution.G, evolution.P, evolution.F])
    preIslandTask setDefault (archive, evolution.initialArchive(Workspace.rng))

    val preIslandCapsule = Capsule(preIslandTask)

    //val islandTask = MoleTask(name + "MoleTask", model)
    //val islandSlot = Slot(model)

    val scalingIndividualsTask = ScalingGAPopulationTask(evolution)(population) set { _.setName(name + "ScalingIndividuals") }

    scalingIndividualsTask addInput archive
    scalingIndividualsTask addInput terminated
    scalingIndividualsTask addInput state
    scalingIndividualsTask addInput generation
    scalingIndividualsTask addOutput archive
    scalingIndividualsTask addOutput population
    scalingIndividualsTask addOutput terminated
    scalingIndividualsTask addOutput state
    scalingIndividualsTask addOutput generation
    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val selectIndividualsTask =
      SamplePopulationTask(evolution)(
        population,
        samples) set { _.setName(name + "Breeding") }

    selectIndividualsTask addInput archive
    selectIndividualsTask addOutput archive

    val skel =
      firstCapsule -<
        (preIslandCapsule, size = Some(number.toString)) --
        fitness.puzzle -- toInidividualsTask --
        elitismCaps --
        terminationSlot --
        scalingIndividualsSlot >| (endCapsule, terminated.name + " == true")

    val loop =
      scalingIndividualsSlot --
        selectIndividualsTask --
        preIslandCapsule

    val dataChannels =
      (firstCapsule oo fitness.puzzle) +
        (firstCapsule -- endCapsule)

    val puzzle = skel + loop + dataChannels

    GAPuzzle(
      GAParameters(parameters.evolution)(
        parameters.archive,
        parameters.genome,
        parameters.individual,
        parameters.population,
        generation
      ),
      puzzle,
      scalingIndividualsSlot.capsule
    )
  }
}
