/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.method.evolution.algorithm._
import org.openmole.plugin.task.tools._

import scala.concurrent.duration.Duration
import scala.util.Random

package object ga {

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

  case class GAPuzzle[+ALG <: GAAlgorithm](parameters: GAParameters[ALG], puzzle: Puzzle, output: ICapsule) {
    def map(f: Puzzle ⇒ Puzzle) = GAPuzzle[ALG](parameters, f(puzzle), output)
  }

  private def components[ALG <: GAAlgorithm](
    name: String,
    evolution: ALG)(implicit plugins: PluginSet) = new { components ⇒
    import evolution._

    val genome = Prototype[evolution.G](name + "Genome")(gManifest)
    val individual = Prototype[Individual[evolution.G, evolution.P, evolution.F]](name + "Individual")
    //val newIndividual = Prototype[Individual[evolution.G, evolution.P, evolution.F]](name + "NewIndividual")
    val population = Prototype[Population[evolution.G, evolution.P, evolution.F]](name + "Population")
    val archive = Prototype[evolution.A](name + "Archive")
    //val newArchive = Prototype[evolution.A](name + "NewArchive")
    val state = Prototype[evolution.STATE](name + "State")
    val fitness = Prototype[evolution.F](name + "Fitness")
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstTask = EmptyTask(name + "First")
    firstTask addInput (Data(archive, Optional))
    firstTask addInput (Data(population, Optional))
    firstTask addOutput (Data(archive, Optional))
    firstTask addOutput (Data(population, Optional))

    val scalingGenomeTask = ScalingGAGenomeTask(evolution)(name + "ScalingGenome", genome)

    val toIndividualTask = ToIndividualTask(evolution)(name + "ToIndividual", genome, individual)

    val elitismTask =
      ElitismTask(evolution)(
        name + "ElitismTask",
        population,
        individual.toArray,
        archive)

    val terminationTask = TerminationTask(evolution)(
      name + "TerminationTask",
      population,
      archive,
      generation,
      state,
      terminated)

    val scalingIndividualsTask = ScalingGAPopulationTask(evolution)(name + "ScalingIndividuals", population)

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

    def gaPuzzle(puzzle: Puzzle, output: ICapsule) =
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

  def generationalGA[ALG <: GAAlgorithm](evolution: ALG)(
    name: String,
    model: Puzzle,
    lambda: Int)(implicit plugins: PluginSet) = {

    val cs = components[ALG](name, evolution)
    import cs._

    val firstCapsule = StrainerCapsule(firstTask)

    val breedTask = ExplorationTask(name + "Breed", BreedSampling(evolution)(population, archive, genome, lambda))
    breedTask.addParameter(population -> Population.empty[evolution.G, evolution.P, evolution.F])
    breedTask.addParameter(archive -> evolution.initialArchive(Workspace.rng))

    breedTask addInput generation
    breedTask addInput state

    breedTask addOutput population
    breedTask addOutput archive
    breedTask addOutput generation
    breedTask addOutput state

    breedTask addParameter (generation -> 0)
    breedTask addParameter Parameter.delayed(state, evolution.initialState)

    val breedingCaps = Capsule(breedTask)
    val breedingCapsItSlot = Slot(breedingCaps)

    val scalingGenomeCaps = Capsule(scalingGenomeTask)
    val toIndividualSlot = Slot(InputStrainerCapsule(toIndividualTask))
    val elitismSlot = Slot(elitismTask)

    terminationTask addOutput archive
    terminationTask addOutput population

    val terminationSlot = Slot(StrainerCapsule(terminationTask))
    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))
    val endSlot = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val exploration = firstCapsule -- breedingCaps -< scalingGenomeCaps -- (model, filter = Block(genome)) -- toIndividualSlot >- elitismSlot -- terminationSlot -- scalingIndividualsSlot -- (endSlot, terminatedCondition, filter = Keep(individual.toArray))

    val loop = terminationSlot -- (breedingCapsItSlot, !terminatedCondition)

    val dataChannels =
      (scalingGenomeCaps -- (toIndividualSlot, filter = Keep(genome))) +
        (breedingCaps -- (elitismSlot, filter = Keep(population, archive))) +
        (breedingCaps oo (model.first, filter = Block(archive, population, genome.toArray))) +
        (breedingCaps -- (endSlot, filter = Block(archive, population, state, generation, terminated, genome.toArray))) +
        (breedingCaps -- (terminationSlot, filter = Block(archive, population, genome.toArray)))

    val gaPuzzle = exploration + loop + dataChannels

    cs.gaPuzzle(gaPuzzle, scalingIndividualsSlot.capsule)
  }

  def steadyGA[ALG <: GAAlgorithm](evolution: ALG)(
    name: String,
    model: Puzzle,
    lambda: Int = 1)(implicit plugins: PluginSet) = {

    val cs = components[ALG](name, evolution)
    import cs._

    val breedTask = ExplorationTask(name + "Breed", BreedSampling(evolution)(population, archive, genome, lambda))
    breedTask.addParameter(population -> Population.empty[evolution.G, evolution.P, evolution.F])
    breedTask.addParameter(archive -> evolution.initialArchive(Workspace.rng))

    val firstCapsule = StrainerCapsule(firstTask)
    val scalingCaps = Capsule(scalingGenomeTask)

    val toIndividualSlot = Slot(InputStrainerCapsule(toIndividualTask))

    val toIndividualArrayCaps = StrainerCapsule(ToArrayTask(name + "IndividualToArray", individual))

    //mergeArchiveTask addParameter (archive -> evolution.initialArchive)
    //val mergeArchiveCaps = MasterCapsule(mergeArchiveTask, archive)

    elitismTask addParameter (population -> Population.empty[evolution.G, evolution.P, evolution.F])
    elitismTask addParameter (archive -> evolution.initialArchive(Workspace.rng))
    val elitismSlot = Slot(MasterCapsule(elitismTask, population, archive))

    terminationTask addParameter Parameter.delayed(state, evolution.initialState)
    terminationTask addParameter generation -> 0
    terminationTask addOutput archive
    terminationTask addOutput population

    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))

    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))

    val steadyBreedingTask = ExplorationTask(name + "Breeding", BreedSampling(evolution)(population, archive, genome, 1))
    val steadyBreedingCaps = Capsule(steadyBreedingTask)

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val skel =
      firstCapsule --
        breedTask -<
        scalingCaps --
        (model, filter = Block(genome)) --
        (toIndividualSlot, filter = Keep(evolution.objectives.map(_.name).toSeq: _*)) --
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
        (firstCapsule oo (model.first, filter = Block(archive, population))) +
        (firstCapsule -- (endCapsule, filter = Block(archive, population))) +
        (firstCapsule oo (elitismSlot, filter = Keep(population, archive)))

    val gaPuzzle = skel + loop + dataChannels

    cs.gaPuzzle(gaPuzzle, scalingIndividualsSlot.capsule)
  }

  def islandSteadyGA[ALG <: GAAlgorithm](evolution: ALG, model: Puzzle)(
    name: String,
    number: Int,
    termination: GATermination { type G >: evolution.G; type P >: evolution.P; type F >: evolution.F },
    sampling: Int)(implicit plugins: PluginSet) = {

    val gaPuzzle: GAPuzzle[ALG] =
      steadyGA[ALG](evolution)(
        s"${name}Island",
        model
      ).map(p ⇒ Capsule(MoleTask(s"${name}IslandTask", p)))

    islandGA[ALG](gaPuzzle)(
      name,
      number,
      termination.asInstanceOf[GATermination { type G = gaPuzzle.parameters.evolution.G; type P = gaPuzzle.parameters.evolution.P; type F = gaPuzzle.parameters.evolution.F }],
      sampling) -> gaPuzzle.puzzle

  }

  def islandGA[AG <: GAAlgorithm](model: GAPuzzle[AG])(
    name: String,
    number: Int,
    termination: GATermination { type G >: model.parameters.evolution.G; type P >: model.parameters.evolution.P; type F >: model.parameters.evolution.F },
    sampling: Int)(implicit plugins: PluginSet) = {

    import model.parameters
    import model.parameters.evolution
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

    val firstCapsule = StrainerCapsule(EmptyTask(name + "First"))

    val toInidividualsTask = PopulationToIndividualsTask(evolution)(name + "PopulationToIndividualTask", population, individual.toArray)
    //val renameIndividualsTask = AssignTask(name + "RenameIndividuals")
    //renameIndividualsTask.assign(individual.toArray, newIndividual.toArray)

    val elitismTask = ElitismTask(islandElitism)(
      name + "ElitismTask",
      parameters.population,
      parameters.individual.toArray,
      parameters.archive)

    elitismTask addParameter (population -> Population.empty[evolution.G, evolution.P, evolution.F])
    elitismTask addParameter (archive -> islandElitism.initialArchive(Workspace.rng))
    val elitismCaps = MasterCapsule(elitismTask, population, archive)

    val terminationTask = TerminationTask(islandElitism)(
      name + "TerminationTask",
      population,
      archive,
      generation,
      state,
      terminated)

    terminationTask addParameter Parameter.delayed(state, islandElitism.initialState)
    terminationTask addParameter (generation -> 0)

    terminationTask addOutput archive
    terminationTask addOutput population
    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val preIslandTask = EmptyTask(name + "PreIsland")
    preIslandTask addInput population
    preIslandTask addInput archive
    preIslandTask addOutput population
    preIslandTask addOutput archive

    preIslandTask addParameter (population -> Population.empty[evolution.G, evolution.P, evolution.F])
    preIslandTask addParameter (archive -> evolution.initialArchive(Workspace.rng))

    val preIslandCapsule = Capsule(preIslandTask)

    //val islandTask = MoleTask(name + "MoleTask", model)
    //val islandSlot = Slot(model)

    val scalingIndividualsTask = ScalingGAPopulationTask(evolution)(name + "ScalingIndividuals", population)

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

    val selectIndividualsTask = SamplePopulationTask(evolution)(
      name + "Breeding",
      population,
      sampling)

    selectIndividualsTask addInput archive
    selectIndividualsTask addOutput archive

    val skel =
      firstCapsule -<
        (preIslandCapsule, size = Some(number.toString)) --
        model.puzzle -- toInidividualsTask --
        elitismCaps --
        terminationSlot --
        scalingIndividualsSlot >| (endCapsule, terminated.name + " == true")

    val loop =
      scalingIndividualsSlot --
        selectIndividualsTask --
        preIslandCapsule

    val dataChannels =
      (firstCapsule oo model.puzzle) +
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
