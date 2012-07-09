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
import fr.iscpif.mgo.crossover._
import fr.iscpif.mgo.elitism._
import fr.iscpif.mgo.mutation._
import fr.iscpif.mgo.modifier._
import fr.iscpif.mgo.ranking._
import fr.iscpif.mgo.diversity._
import fr.iscpif.mgo.selection._
import fr.iscpif.mgo.dominance._
import fr.iscpif.mgo.termination._
import fr.iscpif.mgo.tools.Math
import fr.iscpif.mgo.ga._
import fr.iscpif.mgo.algorithm.ga._

sealed class NSGA2Sigma(
  val distributionIndex: Double,
  val steadySince: Int,
  val archiveSize: Int)
    extends NSGAIISigma
    with MGBinaryTournamentSelection
    with FirstRankedSteadyTermination
    with NonDominatedSortingElitism
    with CoEvolvingSigmaValuesMutation
    with SBXBoundedCrossover
    with CrowdingDistance
    with ParetoRanking
    with StrictDominance
    with RankDiversityModifier
