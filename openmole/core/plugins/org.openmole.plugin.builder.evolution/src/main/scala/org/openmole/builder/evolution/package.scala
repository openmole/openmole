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

  type Inputs = Seq[(Prototype[Double], (String, String))]
  type Objectives = Seq[(Prototype[Double], String)]

  private def components(
    name: String,
    evolution: OMGA,
    inputs: Inputs,
    objectives: Objectives)(implicit plugins: PluginSet) = new { components ⇒
    import evolution._

    val genome = Prototype[evolution.G](name + "Genome")
    val individual = Prototype[Individual[evolution.G, evolution.P, evolution.F]](name + "Individual")
    val newIndividual = Prototype[Individual[evolution.G, evolution.P, evolution.F]](name + "NewIndividual")
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
    breedTask.addParameter(individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])
    breedTask.addParameter(archive -> evolution.initialArchive)

    val scalingGenomeTask = ScalingGAGenomeTask(name + "ScalingGenome", genome, inputs.toSeq: _*)

    val toIndividualTask = ToIndividualTask(evolution)(name + "ToIndividual", genome, individual, objectives)

    /*objectives.foreach {
      case (o, v) ⇒ toIndividualTask addObjective (o, v)
    } */

    inputs.foreach {
      case (i, _) ⇒ toIndividualTask addInput (i)
    }

    val mergeArchiveTask = UpdateArchiveTask(evolution)(name + "MergeArchive", individual.toArray, archive)

    val elitismTask =
      ElitismTask(evolution)(
        name + "ElitismTask",
        individual.toArray,
        archive)

    val terminationTask = TerminationTask(evolution)(
      name + "TerminationTask",
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

    def puzzle(puzzle: Puzzle, output: ICapsule) =
      new Puzzle(puzzle) with Island {
        val evolution = _evolution

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

  trait Island {
    val evolution: OMGA //EvolutionManifest with Modifier with Selection with Lambda with Elitism

    def archive: Prototype[evolution.A]
    def genome: Prototype[evolution.G]
    def individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]]
    def inputs: Inputs
    def objectives: Objectives
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

    breedTask addParameter (generation -> 0)
    breedTask addParameter Parameter.delayed(state, evolution.initialState)

    val breedingCaps = Capsule(breedTask)
    val breedingCapsItSlot = Slot(breedingCaps)

    val scalingGenomeCaps = Capsule(scalingGenomeTask)
    val toIndividualSlot = Slot(InputStrainerCapsule(toIndividualTask))
    val mergeIndividualsSlot = Slot(Capsule(mergeIndividualsTask))
    val mergeArchiveSlot = Slot(Capsule(mergeArchiveTask))
    val elitismSlot = Slot(elitismTask)

    terminationTask addOutput archive
    terminationTask addOutput individual.toArray

    val terminationSlot = Slot(Capsule(terminationTask))
    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))
    val endSlot = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val exploration = firstCapsule -- breedingCaps -< scalingGenomeCaps -- (model, filter = Block(genome)) -- toIndividualSlot >- (mergeArchiveSlot, renameIndividualsTask -- mergeIndividualsSlot)
    val skel = exploration -- elitismSlot -- terminationSlot -- scalingIndividualsSlot -- (endSlot, terminatedCondition, filter = Keep(individual.toArray))

    val loop = terminationSlot -- (breedingCapsItSlot, !terminatedCondition)

    val dataChannels =
      (scalingGenomeCaps -- (toIndividualSlot, filter = Keep(genome))) +
        (breedingCaps -- (mergeArchiveSlot, filter = Keep(archive))) +
        (breedingCaps -- (mergeIndividualsSlot, filter = Keep(individual.toArray))) +
        (breedingCaps oo (model.first, filter = Block(archive, individual.toArray, genome.toArray))) +
        (breedingCaps -- (endSlot, filter = Block(archive, individual.toArray, state, generation, terminated, genome.toArray))) +
        (breedingCaps -- (terminationSlot, filter = Block(archive, individual.toArray))) +
        (mergeArchiveSlot -- (terminationSlot, filter = Keep(archive)))

    val gaPuzzle = skel + loop + dataChannels

    cs.puzzle(gaPuzzle, scalingIndividualsSlot.capsule)
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

    val toIndividualSlot = Slot(InputStrainerCapsule(toIndividualTask))

    val toIndividualArrayCaps = StrainerCapsule(ToArrayTask(name + "IndividualToArray", individual))

    mergeArchiveTask addParameter (archive -> evolution.initialArchive)
    val mergeArchiveCaps = MasterCapsule(mergeArchiveTask, archive)

    mergeIndividualsTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])
    val mergeIndividualsCaps = MasterCapsule(mergeIndividualsTask, individual.toArray)

    val elitismCaps = Capsule(elitismTask)

    terminationTask addParameter Parameter.delayed(state, evolution.initialState)
    terminationTask addParameter (generation -> 0)
    terminationTask addOutput archive
    terminationTask addOutput individual.toArray

    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))

    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))

    val steadyBreedingTask = ExplorationTask(name + "Breeding", BreedSampling(evolution)(individual.toArray, archive, genome, 1))
    val steadyBreedingCaps = Capsule(steadyBreedingTask)

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val skel =
      firstCapsule --
        breedTask -<
        scalingCaps --
        (model, filter = Block(genome)) --
        (toIndividualSlot, filter = Block(inputs.map(_._1).map(_.name).toSeq: _*)) --
        toIndividualArrayCaps --
        (mergeArchiveCaps, renameIndividualsTask -- mergeIndividualsCaps) --
        elitismCaps --
        terminationSlot --
        scalingIndividualsSlot >| (endCapsule, terminatedCondition, filter = Keep(individual.toArray))

    val loop =
      scalingIndividualsSlot --
        steadyBreedingCaps -<-
        scalingCaps

    val dataChannels =
      (scalingCaps -- (toIndividualSlot, filter = Keep(genome))) +
        (firstCapsule oo (model.first, filter = Block(archive, individual.toArray))) +
        (firstCapsule -- (endCapsule, filter = Block(archive, individual.toArray))) +
        (firstCapsule oo (mergeArchiveCaps, filter = Keep(archive))) +
        (firstCapsule oo (mergeIndividualsCaps, filter = Keep(individual.toArray))) +
        (mergeArchiveCaps -- (terminationSlot, filter = Keep(archive)))

    val gaPuzzle = skel + loop + dataChannels

    cs.puzzle(gaPuzzle, scalingIndividualsSlot.capsule)
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
      type P = model.evolution.P
      type A = model.evolution.A
      type MF = model.evolution.MF
      type F = model.evolution.F
      type STATE = termination.STATE

      val stateManifest = termination.stateManifest

      def initialArchive = evolution.initialArchive
      def combine(a1: A, a2: A) = evolution.combine(a1, a2)
      def diff(a1: A, a2: A) = evolution.diff(a1, a2)
      def toArchive(individuals: Seq[Individual[G, P, F]]) = evolution.toArchive(individuals)
      def modify(individuals: Seq[Individual[G, P, F]], archive: A) = evolution.modify(individuals, archive)
      def elitism(individuals: Seq[Individual[G, P, F]], archive: A) = evolution.elitism(individuals, archive)

      def initialState = termination.initialState
      def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE) = termination.terminated(population, terminationState)
    }

    val archive = model.archive.asInstanceOf[Prototype[A]]
    val originalArchive = Prototype[A](name + "OriginalArchive")

    val individual = model.individual.asInstanceOf[Prototype[Individual[G, P, F]]]
    val newIndividual = Prototype[Individual[G, P, F]](name + "NewIndividual")

    val state = Prototype[islandElitism.STATE](name + "State")(islandElitism.stateManifest)
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstCapsule = StrainerCapsule(EmptyTask(name + "First"))

    val renameOriginalArchiveTask = RenameTask(name + "RenameOriginalArchive", archive -> originalArchive)
    renameOriginalArchiveTask addOutput archive

    val renameOriginalArchiveCapsule = Capsule(renameOriginalArchiveTask)

    val archiveDiffSlot = Slot(Capsule(ArchiveDiffTask(evolution)(name + "ArchiveDiff", originalArchive, archive)))

    val mergeArchiveTask = UpdateArchiveTask(evolution)(name + "MergeArchive", individual.toArray, archive)
    mergeArchiveTask addParameter (archive -> islandElitism.initialArchive)
    val mergeArchiveSlot = Slot(MasterCapsule(mergeArchiveTask, archive))

    val renameIndividualsTask = RenameTask(name + "RenameIndividuals", individual.toArray -> newIndividual.toArray)

    val mergeIndividualsTask = FlattenTask(name + "MergeIndividuals", List(individual.toArray, newIndividual.toArray), individual.toArray)
    mergeIndividualsTask addInput archive
    mergeIndividualsTask addOutput archive

    val elitismTask = ElitismTask(islandElitism)(
      name + "ElitismTask",
      individual.toArray,
      archive)

    val moleElitismTask = MoleTask(name + "MoleElitism", mergeIndividualsTask -- elitismTask)
    moleElitismTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])

    val moleElitismCaps = MasterCapsule(moleElitismTask, individual.toArray)

    val terminationTask = TerminationTask(islandElitism)(
      name + "TerminationTask",
      individual.toArray,
      archive,
      generation,
      state,
      terminated)

    terminationTask addParameter Parameter.delayed(state, islandElitism.initialState)
    terminationTask addParameter (generation -> 0)

    terminationTask addOutput archive
    terminationTask addOutput individual.toArray
    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val preIslandTask = EmptyTask(name + "PreIsland")
    preIslandTask addInput individual.toArray
    preIslandTask addInput archive
    preIslandTask addOutput individual.toArray
    preIslandTask addOutput archive

    preIslandTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])
    preIslandTask addParameter (archive -> evolution.initialArchive)

    val preIslandCapsule = Capsule(preIslandTask)

    val islandSlot = Slot(MoleTask(name + "MoleTask", model))

    val scalingIndividualsTask = ScalingGAIndividualsTask(name + "ScalingIndividuals", individual.toArray, model.inputs.toSeq: _*)

    model.objectives.foreach {
      case (o, _) ⇒ scalingIndividualsTask addObjective o
    }

    scalingIndividualsTask addInput archive
    scalingIndividualsTask addInput terminated
    scalingIndividualsTask addInput state
    scalingIndividualsTask addInput generation
    scalingIndividualsTask addOutput archive
    scalingIndividualsTask addOutput individual.toArray
    scalingIndividualsTask addOutput terminated
    scalingIndividualsTask addOutput state
    scalingIndividualsTask addOutput generation
    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val selectIndividualsTask = SelectIndividualsTask(evolution)(
      name + "Breeding",
      individual.toArray,
      sampling)

    selectIndividualsTask addInput archive
    selectIndividualsTask addOutput archive

    val skel =
      firstCapsule -<
        (preIslandCapsule, size = number.toString) --
        islandSlot --
        (renameIndividualsTask, archiveDiffSlot -- mergeArchiveSlot) --
        moleElitismCaps --
        terminationSlot --
        scalingIndividualsSlot >| (endCapsule, terminated.name + " == true")

    val archivePath =
      (preIslandCapsule -- renameOriginalArchiveCapsule -- (archiveDiffSlot, filter = Keep(originalArchive)))

    val loop =
      scalingIndividualsSlot --
        selectIndividualsTask --
        preIslandCapsule

    val dataChannels =
      (firstCapsule oo islandSlot) +
        (firstCapsule -- endCapsule) +
        (islandSlot -- (mergeArchiveSlot, filter = Keep(individual.toArray))) +
        (mergeArchiveSlot -- (terminationSlot, filter = Keep(archive)))

    val puzzle = skel + loop + dataChannels + archivePath

    val (_state, _generation, _genome, _individual, _archive) = (state, generation, model.genome, model.individual, archive)

    new Puzzle(puzzle) {
      def outputCapsule = scalingIndividualsSlot.capsule
      def state = _state
      def generation = _generation
      def genome = _genome
      def island = islandSlot.capsule
      def archive = _archive
      def individual = _individual
    }
  }

}
