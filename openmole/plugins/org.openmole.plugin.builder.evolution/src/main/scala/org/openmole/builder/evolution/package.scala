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
import org.openmole.plugin.method.evolution.ToIndividualArrayTask
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

    //val nsga = new NSGA2Sigma(distributionIndex, steadySince, archiveSize, inputs.size)

    val firstCapsule = new StrainerCapsule(EmptyTask(name + "First"))

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

    scalingArchiveTask addOutput state
    scalingArchiveTask addOutput generation

    val scalingArchiveCapsule = new Capsule(scalingArchiveTask)

    val breedingTask = SteadyBreedTask(evolution)(
      name + "Breeding",
      archive,
      genome.toArray)

    val breedingCaps = new StrainerCapsule(breedingTask)

    val endTask = EmptyTask(name + "End")
    val endCapsule = new StrainerCapsule(endTask)

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

    /*new Transition(firstCapsule, explorationCapsule)
    new ExplorationTransition(explorationCapsule, scalingCaps)
    new Transition(scalingCaps, model.first)
    new Transition(model.lasts, toIndividualCapsule, filtered = Set(genome.name))
    new Transition(toIndividualCapsule, elitismCaps)
    new Transition(elitismCaps, scalingArchiveCapsule)
    new Transition(scalingArchiveCapsule, breedingCaps, generation.name + " % " + offspring + " == 0")
    new SlaveTransition(breedingCaps, new Slot(scalingCaps))*/

    //new EndExplorationTransition(scalingArchiveCapsule, endCapsule, terminated.name)

    new DataChannel(scalingCaps, toIndividualCapsule)
    new DataChannel(elitismCaps, breedingCaps)

    new DataChannel(firstCapsule, model.first)
    new DataChannel(explorationCapsule, endCapsule)

    val (_state, _generation, _genome) = (state, generation, genome)

    new Puzzle(firstCapsule, List(endCapsule), model.selection, model.grouping) {
      def outputCapsule = scalingArchiveCapsule
      def state = _state
      def generation = _generation
      def genome = _genome
    }

  }

}
