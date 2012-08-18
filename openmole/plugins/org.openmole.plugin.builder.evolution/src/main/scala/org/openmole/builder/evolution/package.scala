/*
 * Copyright (C) 2012 reuillon
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
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.sampling._
import org.openmole.core.model.domain._
import org.openmole.plugin.method.evolution._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.transition._

package object evolution {

  def steadyGA(evolution: GAEvolution with Elitism with Termination with Breeding with EvolutionManifest with TerminationManifest)(
    name: String,
    model: Puzzle,
    populationSize: Int,
    inputs: Iterable[(IPrototype[Double], (Double, Double))],
    objectives: Iterable[(IPrototype[Double], Double)])(implicit plugins: IPluginSet) = {

    require(evolution.genomeSize == inputs.size)

    import evolution._

    val genome = new Prototype[evolution.G](name + "Genome")
    val individual = new Prototype[Individual[evolution.G]](name + "Individual")
    val archive = new Prototype[Population[evolution.G, evolution.MF]](name + "Archive")
    val state = new Prototype[evolution.STATE](name + "State")
    val fitness = new Prototype[Fitness](name + "Fitness")
    val generation = new Prototype[Int](name + "Generation")
    val terminated = new Prototype[Boolean](name + "Terminated")

    val firstTask = EmptyTask(name + "First")
    firstTask addInput (new Data(genome.toArray, optional))
    firstTask addOutput (new Data(genome.toArray, optional))

    val firstCapsule = new StrainerCapsule(firstTask)

    val sampling = GenomeSampling(evolution)(genome, populationSize)
    val exploreSampling = ExplorationTask(name + "GenomeExploration", sampling)
    val explorationCapsule = new Capsule(exploreSampling)

    val scalingTask = ScalingGAGenomeTask(name + "ScalingGenome", genome, inputs.toSeq: _*)
    val scalingCaps = new Capsule(scalingTask)

    val toIndividualTask = ToIndividualArrayTask(name + "ToIndividual", genome, individual)
    objectives.foreach {
      case (o, v) ⇒ toIndividualTask addObjective (o, v)
    }

    val toIndividualCapsule = new Capsule(toIndividualTask)

    val elitismTask = ElitismTask(evolution)(
      name + "ElitismTask",
      individual.toArray,
      archive,
      generation,
      state,
      terminated)

    val elitismCaps = new MasterCapsule(elitismTask, archive, state, generation)

    val scalingArchiveTask = ScalingGAArchiveTask[evolution.type](name + "ScalingArchive", archive, inputs.toSeq: _*)

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

    val breedingTask = SteadyBreedTask(evolution)(
      name + "Breeding",
      archive,
      genome.toArray)

    val breedingCaps = new StrainerCapsule(breedingTask)

    val endCapsule = new StrainerCapsule(EmptyTask(name + "End"))

    firstCapsule --
      explorationCapsule -<
      scalingCaps --
      (model, filtered = Set(genome.name)) --
      toIndividualCapsule --
      elitismCaps --
      scalingArchiveCapsule --
      (breedingCaps, condition = generation.name + " % " + evolution.lambda + " == 0") -<-
      scalingCaps.newSlot

    scalingArchiveCapsule >| (endCapsule, terminated.name + " == true")

    new DataChannel(scalingCaps, toIndividualCapsule)

    new DataChannel(firstCapsule, model.first)
    new DataChannel(firstCapsule, endCapsule)

    val (_state, _generation, _genome, _individual, _archive, _inputs, _objectives) = (state, generation, genome, individual, archive, inputs, objectives)

    new Puzzle(firstCapsule, List(endCapsule), model.selection, model.grouping) {
      def outputCapsule = scalingArchiveCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def size = populationSize
      def individual = _individual
      def archive = _archive
      def inputs = _inputs
      def objectives = _objectives
    }

  }

  def islandGA(evolution: GAEvolution with Elitism with Termination with Breeding with EvolutionManifest with TerminationManifest)(
    name: String,
    model: Puzzle {
      def genome: IPrototype[evolution.G]
      def size: Int
      def individual: IPrototype[Individual[evolution.G]]
      def archive: IPrototype[Population[evolution.G, evolution.MF]]
      def inputs: Iterable[(IPrototype[Double], (Double, Double))]
      def objectives: Iterable[(IPrototype[Double], Double)]
    },
    island: Int)(implicit plugins: IPluginSet) = {
    import evolution._
    val archive = new Prototype[Population[evolution.G, evolution.MF]](name + "Archive")
    val state = new Prototype[evolution.STATE](name + "State")
    val generation = new Prototype[Int](name + "Generation")
    val terminated = new Prototype[Boolean](name + "Terminated")

    val firstCapsule = new StrainerCapsule(EmptyTask(name + "First"))

    val sampling = IslandSampling(evolution)(model.genome.toArray, model.size, island)
    val exploration = ExplorationTask(name + "IslandExploration", sampling)

    val archiveToIndividual = ArchiveToIndividualArrayTask(name + "ArchiveToInidividualArray", model.archive, model.individual)

    val elitismTask = ElitismTask(evolution)(
      name + "ElitismTask",
      model.individual.toArray,
      archive,
      generation,
      state,
      terminated)

    val elitismCaps = new MasterCapsule(elitismTask, archive, state, generation)

    val breedingTask = SteadyBreedTask(evolution)(
      name + "Breeding",
      archive,
      model.genome toArray)

    val endCapsule = new StrainerCapsule(EmptyTask(name + "End"))

    val islandCapsule = new Capsule(MoleTask(name + "MoleTask", model))

    val scalingArchiveTask = ScalingGAArchiveTask[evolution.type](name + "ScalingArchive", archive, model.inputs.toSeq: _*)

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

    firstCapsule --
      exploration -<
      islandCapsule --
      archiveToIndividual --
      elitismCaps --
      scalingArchiveCapsule --
      breedingTask --
      islandCapsule.newSlot

    scalingArchiveCapsule >| (endCapsule, terminated.name + " == true")

    new DataChannel(firstCapsule, islandCapsule)
    new DataChannel(firstCapsule, endCapsule)

    val (_state, _generation, _genome, _individual) = (state, generation, model.genome, model.individual)

    new Puzzle(firstCapsule, List(endCapsule), model.selection, model.grouping) {
      def outputCapsule = scalingArchiveCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def island = islandCapsule
    }
  }

}
