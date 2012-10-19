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

package object evolution {

  def steadyGA(evolution: GAEvolution with Elitism with Termination with Breeding with EvolutionManifest with TerminationManifest)(
    name: String,
    model: Puzzle,
    populationSize: Int,
    inputs: Iterable[(Prototype[Double], (Double, Double))],
    objectives: Iterable[(Prototype[Double], Double)])(implicit plugins: PluginSet) = {

    require(evolution.genomeSize == inputs.size)

    import evolution._

    val genome = Prototype[evolution.G](name + "Genome")
    val individual = Prototype[Individual[evolution.G]](name + "Individual")
    val archive = Prototype[Population[evolution.G, evolution.MF]](name + "Archive")
    val state = Prototype[evolution.STATE](name + "State")
    val fitness = Prototype[Fitness](name + "Fitness")
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstTask = EmptyTask(name + "First")
    firstTask addInput (Data(archive, Optional))
    firstTask addOutput (Data(archive, Optional))

    val firstCapsule = new StrainerCapsule(firstTask)

    val initialBreedTask = BreedTask.sized(evolution)(name + "InitialBreed", archive, genome, Some(populationSize))

    val scalingTask = ScalingGAGenomeTask(name + "ScalingGenome", genome, inputs.toSeq: _*)
    val scalingCaps = new Capsule(scalingTask)

    val toIndividualTask = ToIndividualArrayTask(name + "ToIndividual", genome, individual)
    objectives.foreach {
      case (o, v) ⇒ toIndividualTask addObjective (o, v)
    }

    val toIndividualSlot = Slot(Capsule(toIndividualTask))

    val elitismTask = ElitismTask(evolution)(
      name + "ElitismTask",
      individual.toArray,
      archive,
      generation,
      state,
      terminated)

    val elitismCaps = new MasterCapsule(elitismTask, archive, state, generation)

    val scalingArchiveTask = ScalingGAArchiveTask(name + "ScalingArchive", archive, inputs.toSeq: _*)

    objectives.foreach {
      case (o, _) ⇒ scalingArchiveTask addObjective o
    }

    scalingArchiveTask addInput state
    scalingArchiveTask addInput generation
    scalingArchiveTask addInput terminated

    scalingArchiveTask addOutput state
    scalingArchiveTask addOutput generation
    scalingArchiveTask addOutput terminated
    scalingArchiveTask addOutput archive

    val scalingArchiveCapsule = new Capsule(scalingArchiveTask)

    val breedingTask = BreedTask(evolution)(
      name + "Breeding",
      archive,
      genome)

    val breedingCaps = new StrainerCapsule(breedingTask)

    val endCapsule = Slot(new StrainerCapsule(EmptyTask(name + "End")))

    val skel =
      firstCapsule --
        initialBreedTask -<
        scalingCaps --
        (model, filter = Filter(genome)) --
        toIndividualSlot --
        elitismCaps --
        scalingArchiveCapsule >| (endCapsule, terminated.name + " == true")

    val loop =
      scalingArchiveCapsule --
        (breedingCaps, condition = generation.name + " % " + evolution.lambda + " == 0") -<-
        scalingCaps

    val dataChannels =
      (scalingCaps oo toIndividualSlot) +
        (firstCapsule oo (model.first, Filter(archive))) +
        (firstCapsule oo (endCapsule, Filter(archive))) +
        (firstCapsule oo (elitismCaps, filter = Filter not archive))

    val puzzle = skel + loop + dataChannels

    val (_state, _generation, _genome, _individual, _archive, _inputs, _objectives, _populationSize) = (state, generation, genome, individual, archive, inputs, objectives, populationSize)

    new Puzzle(puzzle) {
      def outputCapsule = scalingArchiveCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def populationSize = _populationSize
      def individual = _individual
      def archive = _archive
      def inputs = _inputs
      def objectives = _objectives
    }

  }

  def islandGA(islandEvolution: Elitism with Termination with TerminationManifest with GManifest with GenomeFactory with Modifier with GAG with Lambda with Selection)(
    name: String,
    model: Puzzle {
      def archive: Prototype[Population[islandEvolution.G, islandEvolution.MF]]
      def genome: Prototype[islandEvolution.G]
      def populationSize: Int
      def individual: Prototype[Individual[islandEvolution.G]]
      def inputs: Iterable[(Prototype[Double], (Double, Double))]
      def objectives: Iterable[(Prototype[Double], Double)]
    },
    island: Int)(implicit plugins: PluginSet) = {

    import islandEvolution._

    val archive = model.archive.asInstanceOf[Prototype[Population[islandEvolution.G, islandEvolution.MF]]]
    val initialArchive = model.archive.withName(name + "InitialArchive")

    val individual = model.individual.asInstanceOf[Prototype[Individual[islandEvolution.G]]]
    val genome = model.genome.asInstanceOf[Prototype[islandEvolution.G]]

    val state = Prototype[islandEvolution.STATE](name + "State")
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstCapsule = new StrainerCapsule(EmptyTask(name + "First"))

    val archiveToIndividual = ArchiveToIndividualArrayTask(name + "ArchiveToInidividualArray", model.archive, model.individual)

    val elitismTask = ElitismTask(islandEvolution)(
      name + "ElitismTask",
      individual.toArray,
      archive,
      generation,
      state,
      terminated)

    val elitismCaps = new MasterCapsule(elitismTask, archive, state, generation)

    val breedingTask = SelectPopulationTask(islandEvolution)(
      name + "Breeding",
      archive)

    val endCapsule = Slot(new StrainerCapsule(EmptyTask(name + "End")))

    val preIslandTask = EmptyTask(name + "PreIsland")
    preIslandTask addInput archive
    preIslandTask addOutput archive
    preIslandTask addParameter (archive -> Population.empty)

    val preIslandCapsule = new StrainerCapsule(preIslandTask)

    val islandSlot = Slot(new Capsule(MoleTask(name + "MoleTask", model)))

    val renameArchiveCapsule = new Capsule(RenameTask(name + "RenameArchive", archive, initialArchive))

    val filterPopulationTask = FilterPopulationTask(name + "FilterPopulationTask", archive, initialArchive)
    val filterPopulationCapsule = Slot(new StrainerCapsule(filterPopulationTask))

    val scalingArchiveTask = ScalingGAArchiveTask(name + "ScalingArchive", archive, model.inputs.toSeq: _*)

    model.objectives.foreach {
      case (o, _) ⇒ scalingArchiveTask addObjective o
    }

    scalingArchiveTask addInput state
    scalingArchiveTask addInput generation
    scalingArchiveTask addInput terminated

    scalingArchiveTask addOutput state
    scalingArchiveTask addOutput generation
    scalingArchiveTask addOutput terminated
    scalingArchiveTask addOutput archive

    val scalingArchiveCapsule = new Capsule(scalingArchiveTask)

    val skel =
      firstCapsule -<
        (preIslandCapsule, size = island.toString) --
        islandSlot --
        archiveToIndividual --
        elitismCaps --
        scalingArchiveCapsule >| (endCapsule, terminated.name + " == true")

    val loop =
      scalingArchiveCapsule --
        filterPopulationCapsule --
        breedingTask --
        preIslandCapsule

    val archivePath =
      preIslandCapsule --
        (renameArchiveCapsule, filter = Filter not archive) --
        filterPopulationCapsule

    val dataChannels =
      (firstCapsule oo islandSlot) +
        (firstCapsule oo endCapsule)

    val puzzle = skel + loop + archivePath + dataChannels

    val (_state, _generation, _genome, _individual) = (state, generation, model.genome, model.individual)

    new Puzzle(puzzle) {
      def outputCapsule = scalingArchiveCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def island = islandSlot.capsule
    }
  }

}
