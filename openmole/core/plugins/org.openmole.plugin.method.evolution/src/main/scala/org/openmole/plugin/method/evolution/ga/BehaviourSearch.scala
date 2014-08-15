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
import org.openmole.plugin.method.evolution._

object BehaviourSearch {

  def apply(
    termination: GATermination { type G >: BehaviourSearch#G; type P >: BehaviourSearch#P; type F >: BehaviourSearch#F; type MF >: BehaviourSearch#MF },
    inputs: Inputs[String],
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
      val populationManifest: Manifest[Population[G, P, F, MF]] = implicitly
      val individualManifest: Manifest[Individual[G, P, F]] = implicitly
      val aManifest: Manifest[A] = implicitly
      val fManifest: Manifest[F] = implicitly
      val gManifest: Manifest[G] = implicitly

      val genomeSize = inputs.size

      val gridSize = _gridSize

      override val cloneProbability: Double = _reevaluate

      type STATE = termination.STATE
      def initialState: STATE = termination.initialState
      def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
    }
  }

}

trait BehaviourSearch extends GAAlgorithm
    with HitMapArchive
    with GeneticBreeding
    with SortedTournamentSelection
    with SBXBoundedCrossover
    with TournamentOnRank
    with RankModifier
    with HierarchicalRanking
    with HitCountModifiedFitness
    with CoEvolvingSigmaValuesMutation
    with GAGenomeWithSigma
    with RandomNicheElitism
    with PhenotypeGridNiche {
  type INPUT = String
  def inputConverter = implicitly
}