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

package org.openmole.plugin.method.evolution.ga

import fr.iscpif.mgo._
import org.openmole.core.model.data.Prototype
import org.openmole.plugin.method.evolution._

import scala.util.Random

object BehaviourSearch {

  def apply(
    termination: GATermination { type G >: BehaviourSearch#G; type P >: BehaviourSearch#P; type F >: BehaviourSearch#F },
    inputs: Inputs[Double],
    observables: Objectives,
    gridSize: Seq[Double],
    reevaluate: Double = 0.0) = {
    val _inputs = inputs
    val _gridSize = gridSize
    val _objectives = observables
    val _reevaluate = reevaluate
    new BehaviourSearch {
      val inputs = _inputs
      val objectives = _objectives
      val stateManifest: Manifest[STATE] = termination.stateManifest
      val populationManifest: Manifest[Population[G, P, F]] = implicitly
      val individualManifest: Manifest[Individual[G, P, F]] = implicitly
      val aManifest: Manifest[A] = implicitly
      val fManifest: Manifest[F] = implicitly
      val gManifest: Manifest[G] = implicitly

      val genomeSize = inputs.size

      val gridSize = _gridSize

      override val cloneProbability: Double = _reevaluate

      type STATE = termination.STATE
      def initialState: STATE = termination.initialState
      def terminated(population: Population[G, P, F], terminationState: STATE)(implicit rng: Random): (Boolean, STATE) = termination.terminated(population, terminationState)
    }
  }

}

trait BehaviourSearch extends GAAlgorithm
    with HitMapArchive
    with GeneticBreeding
    with BinaryTournamentSelection with selection.ProportionalNumberOfRound
    with HierarchicalRanking
    with TournamentOnHitCount
    with dynamic.DynamicApplicationGA
    with RandomNicheElitism
    with PhenotypeGridNiche
    with ClampedGenome {
  type INPUT = Double
  def inputConverter = implicitly
}