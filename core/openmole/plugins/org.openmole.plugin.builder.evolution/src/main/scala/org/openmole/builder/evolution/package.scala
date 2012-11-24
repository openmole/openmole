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
import org.openmole.plugin.method.evolution.algorithm._
import org.openmole.misc.exception.UserBadDataError

package object evolution {
  //GA with Archive with Elitism with Modifier with Termination with Breeding with EvolutionManifest with MG
  def steadyGA(evolutionBuilder: Int ⇒ SigmaGA)(
    name: String,
    model: Puzzle,
    workers: Int,
    inputs: Iterable[(Prototype[Double], (Double, Double))],
    objectives: Iterable[(Prototype[Double], Double)])(implicit plugins: PluginSet) = {

    val evolution = evolutionBuilder(inputs.size)

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

    val firstCapsule = StrainerCapsule(firstTask)

    val initialBreedTask = ExplorationTask(name + "InitialBreed", BreedSampling(evolution)(population, genome, workers))

    val scalingTask = ScalingGAGenomeTask(name + "ScalingGenome", genome, inputs.toSeq: _*)
    val scalingCaps = Capsule(scalingTask)

    val toIndividualTask = ModelToArchiveIndividualTask(evolution)(name + "ModelToArchiveIndividual", genome, individual, newArchive)
    objectives.foreach {
      case (o, v) ⇒ toIndividualTask addObjective (o, v)
    }

    val toIndividualSlot = Slot(Capsule(toIndividualTask))

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

    val elitismCaps = MasterCapsule(elitismTask, archive, state, generation, population)

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

    val scalingPopulationCapsule = Capsule(scalingPopulationTask)

    val breedingTask = ExplorationTask(name + "Breeding", BreedSampling(evolution)(population, genome, 1))
    val breedingCaps = StrainerCapsule(breedingTask)

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val skel =
      firstCapsule --
        initialBreedTask -<
        scalingCaps --
        (model, filter = Filter(genome)) --
        toIndividualSlot --
        elitismCaps --
        scalingPopulationCapsule >| (endCapsule, terminated.name + " == true")

    val loop =
      scalingPopulationCapsule --
        (breedingCaps, condition = generation.name + " % " + evolution.lambda + " == 0") -<-
        scalingCaps

    val dataChannels =
      (scalingCaps oo toIndividualSlot) +
        (firstCapsule oo (model.first, Filter(archive, population))) +
        (firstCapsule oo (endCapsule, Filter(archive, population))) +
        (firstCapsule oo (elitismCaps, filter = Filter not (archive, population)))

    val puzzle = skel + loop + dataChannels

    val (_state, _generation, _genome, _individual, _population, _archive, _inputs, _objectives, _workers) = (state, generation, genome, individual, population, archive, inputs, objectives, workers)

    new Puzzle(puzzle) {
      def outputCapsule = scalingPopulationCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def individual = _individual
      def population = _population
      def archive = _archive
      def inputs = _inputs
      def objectives = _objectives
    }

  }

  def islandGA(
    island: Island[Evolution with EvolutionManifest with Elitism with Termination with Modifier with Termination with Lambda with Selection { type G <: GAGenome; type F <: MGFitness }])(
      name: String,
      model: Puzzle {
        def population: Prototype[Population[island.evolution.G, island.evolution.F, island.evolution.MF]]
        def archive: Prototype[island.evolution.A]
        def genome: Prototype[island.evolution.G]
        def individual: Prototype[Individual[island.evolution.G, island.evolution.F]]
        def inputs: Iterable[(Prototype[Double], (Double, Double))]
        def objectives: Iterable[(Prototype[Double], Double)]
      },
      number: Int)(implicit plugins: PluginSet) = {

    import island.evolution._

    val population = model.population.asInstanceOf[Prototype[Population[G, F, MF]]]
    val archive = model.archive.asInstanceOf[Prototype[A]]
    val newArchive = Prototype[A](name + "NewArchive")

    val initialPopulation = model.population.withName(name + "InitialPopulation")

    val individual = model.individual.asInstanceOf[Prototype[Individual[G, F]]]

    val genome = model.genome.asInstanceOf[Prototype[G]]

    val state = Prototype[STATE](name + "State")
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstCapsule = StrainerCapsule(EmptyTask(name + "First"))

    val populationToIndividuals = PopulationToIndividualArrayTask(name + "PopulationToInidividualArray", model.population, model.individual)

    val renameArchiveTask = RenameTask(name + "RenameArchive", archive -> newArchive)

    val elitismTask = ElitismTask(island.evolution)(
      name + "ElitismTask",
      individual.toArray,
      population,
      newArchive,
      archive,
      generation,
      state,
      terminated)

    val elitismCaps = MasterCapsule(elitismTask, archive, population, state, generation)

    val breedingTask = SelectPopulationTask(island.evolution)(
      name + "Breeding",
      population,
      archive)

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val preIslandTask = EmptyTask(name + "PreIsland")
    preIslandTask addInput population
    preIslandTask addInput archive
    preIslandTask addOutput population
    preIslandTask addOutput archive
    preIslandTask addParameter (population -> Population.empty)
    preIslandTask addParameter (archive -> island.evolution.initialArchive)

    val preIslandCapsule = StrainerCapsule(preIslandTask)

    val islandSlot = Slot(Capsule(MoleTask(name + "MoleTask", model)))

    val renamePopulationCapsule = Capsule(RenameTask(name + "RenamePopulation", population -> initialPopulation))

    val filterPopulationCapsule =
      Slot(
        FilterPopulationTask(
          name + "FilterPopulationTask",
          population,
          initialPopulation))

    val scalingPopulationTask = ScalingGAPopulationTask(name + "ScalingPopulation", population, model.inputs.toSeq: _*)

    model.objectives.foreach {
      case (o, _) ⇒ scalingPopulationTask addObjective o
    }

    scalingPopulationTask addInput state
    scalingPopulationTask addInput generation
    scalingPopulationTask addInput terminated

    scalingPopulationTask addOutput state
    scalingPopulationTask addOutput generation
    scalingPopulationTask addOutput terminated

    val scalingPopulationCapsule = Capsule(scalingPopulationTask)

    val skel =
      firstCapsule -<
        (preIslandCapsule, size = number.toString) --
        islandSlot --
        (populationToIndividuals -- renameArchiveTask) --
        elitismCaps --
        scalingPopulationCapsule >| (endCapsule, terminated.name + " == true")

    val loop =
      scalingPopulationCapsule --
        filterPopulationCapsule --
        breedingTask --
        preIslandCapsule

    val populationPath =
      preIslandCapsule --
        (renamePopulationCapsule, filter = Filter not population) --
        filterPopulationCapsule

    val dataChannels =
      (firstCapsule oo islandSlot) +
        (firstCapsule oo endCapsule)

    val puzzle = skel + loop + populationPath + dataChannels

    val (_state, _generation, _genome, _individual) = (state, generation, model.genome, model.individual)

    new Puzzle(puzzle) {
      def outputCapsule = scalingPopulationCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def island = islandSlot.capsule
    }
  }

}
