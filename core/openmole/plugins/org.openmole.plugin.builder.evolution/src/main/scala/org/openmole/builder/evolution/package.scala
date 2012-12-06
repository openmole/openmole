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
    val newIndividual = Prototype[Individual[evolution.G, evolution.F]](name + "NewIndividual")
    //val population = Prototype[Population[evolution.G, evolution.F, evolution.MF]](name + "Population")
    val archive = Prototype[evolution.A](name + "Archive")
    val newArchive = Prototype[evolution.A](name + "NewArchive")
    val state = Prototype[evolution.STATE](name + "State")
    val fitness = Prototype[evolution.F](name + "Fitness")
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstTask = EmptyTask(name + "First")
    firstTask addInput (Data(archive, Optional))
    firstTask addInput (Data(individual.toArray, Optional))
    firstTask addOutput (Data(archive, Optional))
    firstTask addOutput (Data(individual.toArray, Optional))

    val breedTask = ExplorationTask(name + "Breed", BreedSampling(evolution)(individual.toArray, archive, genome, evolution.lambda))
    breedTask.addParameter(individual.toArray -> Array.empty[Individual[evolution.G, evolution.F]])
    breedTask.addParameter(archive -> evolution.initialArchive)

    val scalingGenomeTask = ScalingGAGenomeTask(name + "ScalingGenome", genome, inputs.toSeq: _*)

    val toIndividualTask = ToIndividualTask(evolution)(name + "ToIndividual", genome, individual)
    objectives.foreach {
      case (o, v) ⇒ toIndividualTask addObjective (o, v)
    }

    //val renameArchiveTask = RenameTask(name + "RenameIndividuals", archive.toArray -> newArchive.toArray)
    val mergeArchiveTask = UpdateArchiveTask(evolution)(name + "MergeArchive", individual.toArray, archive)

    val elitismTask =
      ElitismTask(evolution)(
        name + "ElitismTask",
        individual.toArray,
        archive,
        generation,
        state,
        terminated)

    val scalingIndividualsTask = ScalingGAIndividualsTask(name + "ScalingIndividuals", individual.toArray, inputs.toSeq: _*)

    objectives.foreach {
      case (o, _) ⇒ scalingIndividualsTask addObjective o
    }

    scalingIndividualsTask addInput state
    scalingIndividualsTask addInput generation
    scalingIndividualsTask addInput terminated
    scalingIndividualsTask addInput archive

    scalingIndividualsTask addOutput state
    scalingIndividualsTask addOutput generation
    scalingIndividualsTask addOutput terminated
    scalingIndividualsTask addOutput individual.toArray
    scalingIndividualsTask addOutput archive

    val renameIndividualsTask = RenameTask(name + "RenameIndividuals", individual.toArray -> newIndividual.toArray)
    val mergeIndividualsTask = FlattenTask(name + "MergeIndividuals", List(individual.toArray, newIndividual.toArray), individual.toArray)

    val terminatedCondition = Condition(terminated.name + " == true")

    val (_evolution, _inputs, _objectives) = (evolution, inputs, objectives)

    def puzzle(puzzle: Puzzle, output: Capsule, elitism: Capsule) =
      new Puzzle(puzzle) with Island {
        val evolution = _evolution

        val archive = components.archive.asInstanceOf[Prototype[evolution.A]]
        val genome = components.genome
        val individual = components.individual

        def inputs = _inputs
        def objectives = _objectives

        def outputCapsule = output
        def elitismCapsule = elitism
        def state = components.state
        def generation = components.generation
      }

  }

  trait Island {
    val evolution: OMGA //EvolutionManifest with Modifier with Selection with Lambda with Elitism

    def archive: Prototype[evolution.A]
    def genome: Prototype[evolution.G]
    def individual: Prototype[Individual[evolution.G, evolution.F]]
    def inputs: Iterable[(Prototype[Double], (Double, Double))]
    def objectives: Iterable[(Prototype[Double], Double)]
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

    breedTask addInput generation
    breedTask addInput state

    breedTask addOutput individual.toArray
    breedTask addOutput archive
    breedTask addOutput generation
    breedTask addOutput state

    breedTask.addParameter(generation -> 0)
    breedTask addParameter (state -> evolution.initialState)

    val breedingCaps = StrainerCapsule(breedTask)

    val breedingCapsItSlot = Slot(breedingCaps)
    val scalingCaps = Capsule(scalingGenomeTask)
    val toIndividualSlot = Slot(Capsule(toIndividualTask))
    val mergeIndividualsSlot = Slot(Capsule(mergeIndividualsTask))
    val mergeArchiveSlot = Slot(Capsule(mergeArchiveTask))
    val elitismCaps = Capsule(elitismTask)
    val elitismSlot = Slot(elitismCaps)
    val scalingIndividualsCapsule = Capsule(scalingIndividualsTask)
    val endSlot = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val exploration = firstCapsule -- breedingCaps -< scalingCaps -- model -- toIndividualSlot >- (mergeArchiveSlot, renameIndividualsTask -- mergeIndividualsSlot)
    val skel = exploration -- elitismSlot -- scalingIndividualsCapsule -- (endSlot, terminatedCondition)

    val loop = elitismSlot -- (breedingCapsItSlot, !terminatedCondition)

    val dataChannels =
      (scalingCaps -- toIndividualSlot) +
        (breedingCaps -- (mergeArchiveSlot, filter = Keep(archive))) +
        (breedingCaps -- (mergeIndividualsSlot, filter = Keep(individual.toArray))) +
        (breedingCaps oo (model.first, filter = Filter(archive, individual.toArray))) +
        (breedingCaps -- (endSlot, filter = Filter(archive, individual.toArray, state, generation, terminated))) +
        (breedingCaps -- (elitismSlot, filter = Filter(archive, individual.toArray)))

    val gaPuzzle = skel + loop + dataChannels

    cs.puzzle(gaPuzzle, scalingIndividualsCapsule, elitismCaps)
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

    val toIndividualArrayCaps = StrainerCapsule(ToArrayTask(name + "IndividualToArray", individual))

    mergeArchiveTask addParameter (archive -> evolution.initialArchive)
    val mergeArchiveCaps = MasterCapsule(mergeArchiveTask, archive)

    mergeIndividualsTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.F]])
    val mergeIndividualsCaps = MasterCapsule(mergeIndividualsTask, individual.toArray)

    elitismTask addParameter (state -> evolution.initialState)
    elitismTask addParameter (generation -> 0)

    val elitismCaps = MasterCapsule(elitismTask, generation, state)

    val scalingIndividualsCapsule = Capsule(scalingIndividualsTask)

    val steadyBreedingCaps = StrainerCapsule(ExplorationTask(name + "Breeding", BreedSampling(evolution)(individual.toArray, archive, genome, 1)))

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val skel =
      firstCapsule --
        breedTask -<
        scalingCaps --
        (model, filter = Filter(genome)) --
        toIndividualSlot --
        toIndividualArrayCaps --
        (mergeArchiveCaps, renameIndividualsTask -- mergeIndividualsCaps) --
        elitismCaps --
        scalingIndividualsCapsule >| (endCapsule, terminatedCondition)

    val loop =
      scalingIndividualsCapsule --
        steadyBreedingCaps -<-
        scalingCaps

    val dataChannels =
      (scalingCaps -- toIndividualSlot) +
        (firstCapsule oo (model.first, filter = Filter(archive, individual.toArray))) +
        (firstCapsule -- (endCapsule, filter = Filter(archive, individual.toArray))) +
        (firstCapsule oo (mergeArchiveCaps, filter = Keep(archive))) +
        (firstCapsule oo (mergeIndividualsCaps, filter = Keep(individual.toArray)))

    val gaPuzzle = skel + loop + dataChannels

    cs.puzzle(gaPuzzle, scalingIndividualsCapsule, elitismCaps)
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
      def elitism(individuals: Seq[Individual[G, F]], archive: A) = evolution.elitism(individuals, archive)

      def initialState = termination.initialState
      def terminated(population: ⇒ Population[G, F, MF], terminationState: STATE) = termination.terminated(population, terminationState)
    }

    val archive = model.archive.asInstanceOf[Prototype[A]]
    //val newArchive = Prototype[A](name + "NewArchive")
    val originalArchive = Prototype[A](name + "OriginalArchive")

    val individual = model.individual.asInstanceOf[Prototype[Individual[G, F]]]
    val newIndividual = Prototype[Individual[evolution.G, evolution.F]](name + "NewIndividual")

    val genome = model.genome.asInstanceOf[Prototype[G]]

    val state = Prototype[islandElitism.STATE](name + "State")(islandElitism.stateManifest)
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstCapsule = StrainerCapsule(EmptyTask(name + "First"))

    val renameOriginalArchiveTask = RenameTask(name + "RenameOriginalArchive", archive -> originalArchive)
    renameOriginalArchiveTask addOutput archive

    val renameOriginalArchiveCapsule = StrainerCapsule(renameOriginalArchiveTask)

    val archiveDiffSlot = Slot(Capsule(ArchiveDiffTask(evolution)(name + "ArchiveDiff", originalArchive, archive)))

    val mergeArchiveTask = UpdateArchiveTask(evolution)(name + "MergeArchive", individual.toArray, archive)
    mergeArchiveTask addParameter (archive -> islandElitism.initialArchive)
    val mergeArchiveSlot = Slot(MasterCapsule(mergeArchiveTask, archive))

    val renameIndividualsTask = RenameTask(name + "RenameIndividuals", individual.toArray -> newIndividual.toArray)
    val mergeIndividualsTask = FlattenTask(name + "MergeIndividuals", List(individual.toArray, newIndividual.toArray), individual.toArray)
    mergeIndividualsTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.F]])
    val mergeIndividualsCaps = MasterCapsule(mergeIndividualsTask, individual.toArray)

    val elitismTask = ElitismTask(islandElitism)(
      name + "ElitismTask",
      individual.toArray,
      archive,
      generation,
      state,
      terminated)

    elitismTask addParameter (state -> islandElitism.initialState)
    elitismTask addParameter (generation -> 0)

    val elitismCaps = MasterCapsule(elitismTask, generation, state)

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val preIslandTask = EmptyTask(name + "PreIsland")
    preIslandTask addInput individual.toArray
    preIslandTask addInput archive
    preIslandTask addOutput individual.toArray
    preIslandTask addOutput archive

    preIslandTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.F]])
    preIslandTask addParameter (archive -> evolution.initialArchive)

    val preIslandCapsule = StrainerCapsule(preIslandTask)

    val islandTask = MoleTask(name + "MoleTask", model)

    val islandSlot = Slot(Capsule(islandTask))

    val scalingIndividualsTask = ScalingGAIndividualsTask(name + "ScalingIndividuals", individual.toArray, model.inputs.toSeq: _*)

    model.objectives.foreach {
      case (o, _) ⇒ scalingIndividualsTask addObjective o
    }

    val scalingIndividualsCapsule = StrainerCapsule(scalingIndividualsTask)

    val selectIndividualsTask = SelectIndividualsTask(evolution)(
      name + "Breeding",
      individual.toArray,
      sampling)

    selectIndividualsTask addInput archive
    selectIndividualsTask addOutput archive

    val skel =
      firstCapsule -<
        (preIslandCapsule, size = number.toString) --
        renameOriginalArchiveCapsule --
        islandSlot --
        (renameIndividualsTask -- mergeIndividualsCaps, archiveDiffSlot -- mergeArchiveSlot) --
        elitismCaps --
        scalingIndividualsCapsule >| (endCapsule, terminated.name + " == true")

    val loop =
      scalingIndividualsCapsule --
        selectIndividualsTask --
        preIslandCapsule

    val dataChannels =
      (firstCapsule oo islandSlot) +
        (firstCapsule -- endCapsule) +
        (renameOriginalArchiveCapsule -- (archiveDiffSlot, filter = Keep(originalArchive))) +
        (islandSlot -- (mergeArchiveSlot, filter = Keep(individual.toArray)))

    val puzzle = skel + loop + dataChannels

    val (_state, _generation, _genome, _individual, _archive) = (state, generation, model.genome, model.individual, archive)

    new Puzzle(puzzle) {
      def outputCapsule = scalingIndividualsCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def island = islandSlot.capsule
      def archive = _archive
      def elitismCapsule = elitismCaps
    }
  }

}
