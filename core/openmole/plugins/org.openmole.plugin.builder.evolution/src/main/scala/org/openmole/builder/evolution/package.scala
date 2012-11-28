/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.builder

import fr.iscpif.mgo._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.sampling._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.sampling._
import org.openmole.core.model.domain._
import org.openmole.core.model.transition._
import org.openmole.plugin.method.evolution._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.tools._
import org.openmole.plugin.method.evolution.algorithm.{ EvolutionManifest, TerminationManifest, GA ⇒ OMGA }
import org.openmole.misc.exception.UserBadDataError

package object evolution {

  type Inputs = Iterable[(Prototype[Double], (Double, Double))]
  type Objectives = Iterable[(Prototype[Double], Double)]

  private def components(
    name: String,
    evolution: OMGA,
    inputs: Inputs,
    objectives: Objectives)(implicit plugins: PluginSet) = new { components ⇒
    import evolution._

    val genome = Prototype[evolution.G](name + "Genome")
    val individual = Prototype[Individual[evolution.G, evolution.F]](name + "Individual")
    val population = Prototype[Population[evolution.G, evolution.F, evolution.MF]](name + "Population")
    val archive = Prototype[evolution.A](name + "Archive")
    val newArchive = Prototype[evolution.A](name + "NewArchive")
    val state = Prototype[evolution.STATE](name + "State")
    val fitness = Prototype[evolution.F](name + "Fitness")
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstTask = EmptyTask(name + "First")
    firstTask addInput (Data(archive, Optional))
    firstTask addInput (Data(population, Optional))
    firstTask addOutput (Data(archive, Optional))
    firstTask addOutput (Data(population, Optional))

    val breedTask = ExplorationTask(name + "InitialBreed", BreedSampling(evolution)(population, genome, evolution.lambda))

    val scalingGenomeTask = ScalingGAGenomeTask(name + "ScalingGenome", genome, inputs.toSeq: _*)

    val toIndividualTask = ModelToArchiveIndividualTask(evolution)(name + "ModelToArchiveIndividual", genome, individual, newArchive)
    objectives.foreach {
      case (o, v) ⇒ toIndividualTask addObjective (o, v)
    }

    val elitismTask =
      ElitismTask(evolution)(
        name + "ElitismTask",
        individual.toArray,
        population,
        newArchive,
        archive,
        generation,
        state,
        terminated)

    val scalingPopulationTask = ScalingGAPopulationTask(name + "ScalingPopulation", population, inputs.toSeq: _*)

    objectives.foreach {
      case (o, _) ⇒ scalingPopulationTask addObjective o
    }

    scalingPopulationTask addInput state
    scalingPopulationTask addInput generation
    scalingPopulationTask addInput terminated
    scalingPopulationTask addInput archive

    scalingPopulationTask addOutput state
    scalingPopulationTask addOutput generation
    scalingPopulationTask addOutput terminated
    scalingPopulationTask addOutput population
    scalingPopulationTask addOutput archive

    val terminatedCondition = Condition(terminated.name + " == true")

    val (_evolution, _inputs, _objectives) = (evolution, inputs, objectives)

    def puzzle(puzzle: Puzzle, output: Capsule) =
      new Puzzle(puzzle) with Island {
        val evolution = _evolution

        val population = components.population
        val archive = components.archive.asInstanceOf[Prototype[evolution.A]]
        val genome = components.genome
        val individual = components.individual

        def inputs = _inputs
        def objectives = _objectives

        def outputCapsule = output
        def state = components.state
        def generation = components.generation
      }

  }

  def generationalGA(evolutionBuilder: Int ⇒ OMGA)(
    name: String,
    model: Puzzle,
    inputs: Inputs,
    objectives: Objectives)(implicit plugins: PluginSet) = {

    val evolution = evolutionBuilder(inputs.size)
    val cs = components(name, evolution, inputs, objectives)
    import cs._

    val firstCapsule = StrainerCapsule(firstTask)
    val breedingCaps = StrainerCapsule(breedTask)
    val breedingCapsItSlot = Slot(StrainerCapsule(breedTask))
    val scalingCaps = Capsule(scalingGenomeTask)
    val toIndividualSlot = Slot(Capsule(toIndividualTask))
    val elitismSlot = Slot(elitismTask)
    val scalingPopulationCapsule = Capsule(scalingPopulationTask)
    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val skel =
      firstCapsule -- breedingCaps -< scalingCaps -- model -- toIndividualSlot >- elitismSlot -- scalingPopulationCapsule -- (endCapsule, terminatedCondition)

    val loop =
      scalingPopulationCapsule -- (breedingCapsItSlot, !terminatedCondition)

    val dataChannels =
      (scalingCaps -- toIndividualSlot) +
        (firstCapsule oo (model.first, filter = Filter(archive, population))) +
        (firstCapsule -- (endCapsule, filter = Filter(archive, population))) +
        (breedingCaps -- (elitismSlot, filter = Keep(archive, population))) +
        (breedingCaps -- (breedingCapsItSlot, filter = Keep(archive, population)))

    val gaPuzzle = skel + loop + dataChannels

    cs.puzzle(gaPuzzle, scalingPopulationCapsule)
  }

  def steadyGA(evolutionBuilder: Int ⇒ OMGA)(
    name: String,
    model: Puzzle,
    inputs: Inputs,
    objectives: Objectives)(implicit plugins: PluginSet) = {

    val evolution = evolutionBuilder(inputs.size)
    val cs = components(name, evolution, inputs, objectives)
    import cs._

    val firstCapsule = StrainerCapsule(firstTask)
    val scalingCaps = Capsule(scalingGenomeTask)
    val toIndividualSlot = Slot(Capsule(toIndividualTask))
    val elitismCaps = MasterCapsule(elitismTask, archive, state, generation, population)
    val scalingPopulationCapsule = Capsule(scalingPopulationTask)
    val steadyBreedingCaps = StrainerCapsule(ExplorationTask(name + "Breeding", BreedSampling(evolution)(population, genome, 1)))
    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val skel =
      firstCapsule --
        breedTask -<
        scalingCaps --
        (model, filter = Filter(genome)) --
        toIndividualSlot --
        elitismCaps --
        scalingPopulationCapsule >| (endCapsule, terminatedCondition)

    val loop =
      scalingPopulationCapsule --
        steadyBreedingCaps -<-
        scalingCaps

    val dataChannels =
      (scalingCaps -- toIndividualSlot) +
        (firstCapsule oo (model.first, Filter(archive, population))) +
        (firstCapsule oo (endCapsule, Filter(archive, population))) +
        (firstCapsule oo (elitismCaps, Keep(archive, population)))

    val gaPuzzle = skel + loop + dataChannels

    cs.puzzle(gaPuzzle, scalingPopulationCapsule)
  }

  trait Island {
    val evolution: OMGA //EvolutionManifest with Modifier with Selection with Lambda with Elitism

    def population: Prototype[Population[evolution.G, evolution.F, evolution.MF]]
    def archive: Prototype[evolution.A]
    def genome: Prototype[evolution.G]
    def individual: Prototype[Individual[evolution.G, evolution.F]]
    def inputs: Iterable[(Prototype[Double], (Double, Double))]
    def objectives: Iterable[(Prototype[Double], Double)]
  }

  def islandGA(model: Puzzle with Island)(
    name: String,
    number: Int,
    termination: GA.GATermination,
    sampling: Int = model.evolution.lambda)(implicit plugins: PluginSet) = {

    import model.evolution
    import evolution._

    val islandElitism = new Elitism with Termination with Modifier with Archive with TerminationManifest {
      type G = model.evolution.G
      type A = model.evolution.A
      type MF = model.evolution.MF
      type F = model.evolution.F
      type STATE = termination.STATE

      val stateManifest = termination.stateManifest

      def initialArchive = evolution.initialArchive
      def combine(a1: A, a2: A) = evolution.combine(a1, a2)
      def diff(a1: A, a2: A) = evolution.diff(a1, a2)
      def toArchive(individuals: Seq[Individual[G, F]]) = evolution.toArchive(individuals)
      def modify(individuals: Seq[Individual[G, F]], archive: A) = evolution.modify(individuals, archive)
      def elitism(population: Population[G, F, MF]) = evolution.elitism(population)

      def initialState(p: Population[G, F, MF]) = termination.initialState(p)
      def terminated(population: Population[G, F, MF], terminationState: STATE) = termination.terminated(population, terminationState)
    }

    val population = model.population.asInstanceOf[Prototype[Population[G, F, MF]]]
    val archive = model.archive.asInstanceOf[Prototype[A]]
    val newArchive = Prototype[A](name + "NewArchive")
    val originalArchive = Prototype[A](name + "OriginalArchive")

    val initialPopulation = model.population.withName(name + "InitialPopulation")

    val individual = model.individual.asInstanceOf[Prototype[Individual[G, F]]]

    val genome = model.genome.asInstanceOf[Prototype[G]]

    val state = Prototype[islandElitism.STATE](name + "State")(islandElitism.stateManifest)
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstCapsule = StrainerCapsule(EmptyTask(name + "First"))

    val populationToIndividuals = PopulationToIndividualArrayTask(name + "PopulationToInidividualArray", model.population, model.individual)

    val renameArchiveTask = RenameTask(name + "RenameNewArchive", archive -> newArchive)

    val renameOriginalArchiveTask = RenameTask(name + "RenameOriginalArchive", archive -> originalArchive)
    val archiveDiffSlot = Slot(ArchiveDiffTask(evolution)(name + "ArchiveDiff", originalArchive, newArchive))

    val elitismTask = ElitismTask(islandElitism)(
      name + "ElitismTask",
      individual.toArray,
      population,
      newArchive,
      archive,
      generation,
      state,
      terminated)

    val elitismCaps = MasterCapsule(elitismTask, archive, population, state, generation)

    val breedingTask = SelectPopulationTask(evolution)(
      name + "Breeding",
      population,
      archive,
      sampling)

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val preIslandTask = EmptyTask(name + "PreIsland")
    preIslandTask addInput population
    preIslandTask addInput archive
    preIslandTask addOutput population
    preIslandTask addOutput archive
    preIslandTask addParameter (population -> Population.empty)
    preIslandTask addParameter (archive -> evolution.initialArchive)

    val preIslandCapsule = StrainerCapsule(preIslandTask)

    val islandSlot = Slot(Capsule(MoleTask(name + "MoleTask", model)))

    val scalingPopulationTask = ScalingGAPopulationTask(name + "ScalingPopulation", population, model.inputs.toSeq: _*)

    model.objectives.foreach {
      case (o, _) ⇒ scalingPopulationTask addObjective o
    }

    val scalingPopulationCapsule = StrainerCapsule(scalingPopulationTask)

    val skel =
      firstCapsule -<
        (preIslandCapsule, size = number.toString) --
        islandSlot --
        (populationToIndividuals, renameArchiveTask -- archiveDiffSlot) --
        elitismCaps --
        scalingPopulationCapsule >| (endCapsule, terminated.name + " == true")

    val archiveDiff = preIslandCapsule -- renameOriginalArchiveTask -- archiveDiffSlot

    val loop =
      scalingPopulationCapsule --
        breedingTask --
        preIslandCapsule

    val dataChannels =
      (firstCapsule oo islandSlot) +
        (firstCapsule oo endCapsule)

    val puzzle = skel + loop + dataChannels + archiveDiff

    val (_state, _generation, _genome, _individual, _archive) = (state, generation, model.genome, model.individual, archive)

    new Puzzle(puzzle) {
      def outputCapsule = scalingPopulationCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def island = islandSlot.capsule
      def archive = _archive
      def elitismCapsule = elitismCaps
    }
  }

}
