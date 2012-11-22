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

package org.openmole.plugin.method.evolution.algorithm

import fr.iscpif.mgo._

sealed class Island[E](val evolution: E)(
  val mu: Int,
  val steps: Int) /*
    extends BinaryTournamentSelection
    with GManifest
    with NonDominatedElitism
    with CounterTermination
    with TerminationManifest
    with DiversityMetric
    with ParetoRanking
    with RankDiversityModifier
    with GenomeFactory
    with Lambda
    with CloneRemoval {

  type G = evolution.G
  type F = evolution.F
  type DIVERSIFIED = evolution.DIVERSIFIED

  val gManifest = evolution.gManifest
  val stateManifest = manifest[STATE]

  val genomeFactory = evolution.genomeFactory

  def lambda = evolution.mu
  def isDominated(p1: Seq[Double], p2: Seq[Double]) = evolution.isDominated(p1, p2)
  def diversity(individuals: Seq[DIVERSIFIED], ranks: Seq[Lazy[Int]]): IndexedSeq[Lazy[Double]] = evolution.diversity(individuals, ranks)
}       */
