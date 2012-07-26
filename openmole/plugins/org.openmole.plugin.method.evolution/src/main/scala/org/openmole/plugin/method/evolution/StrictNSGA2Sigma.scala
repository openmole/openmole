/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

sealed class StrictNSGA2Sigma(
  val distributionIndex: Double,
  val windowSize: Int,
  val deviationEpsilon: Double,
  val genomeSize: Int,
  val mu: Int,
  val lambda: Int) extends NSGAIISigma
    with MGBinaryTournamentSelection
    with CrowdingStabilityTermination
    with NonDominatedSortingElitism
    with CoEvolvingSigmaValuesMutation
    with SBXBoundedCrossover
    with Crowding
    with ParetoRanking
    with StrictDominance
    with RankDiversityModifier
    with EvolutionManifest
    with TerminationManifest {

  val genomeManifest = manifest[G]
  val individualManifest = manifest[Individual[G]]
  val populationManifest = manifest[Population[G, MF]]
  val stateManifest = manifest[STATE]
}
