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
import fr.iscpif.mgo.tools.Lazy

object EpsilonSigmaGA {
  def counter(_steps: Int) = new CounterTermination {
    val steps = _steps
  }

  def timed(_duration: Long) = new TimedTermination {
    val duration = _duration
  }

  trait DiversityMetricBuilder {
    def apply(dominance: Dominance): DiversityMetric { type DIVERSIFIED = MGFitness }
  }

  def crowding = new DiversityMetricBuilder {
    def apply(dominance: Dominance) = new CrowdingDiversity {
      type DIVERSIFIED = MGFitness
    }
  }

  def hypervolume(_referencePoint: Seq[Double]) = new DiversityMetricBuilder {
    def apply(dominance: Dominance) = new HypervolumeDiversity {
      type DIVERSIFIED = MGFitness
      def isDominated(p1: Seq[Double], p2: Seq[Double]) = dominance.isDominated(p1, p2)
      val referencePoint = _referencePoint
    }
  }

}

sealed class EpsilonSigmaGA(
  val distributionIndex: Double,
  val steps: Int,
  val genomeSize: Int,
  val mu: Int,
  val lambda: Int,
  val epsilons: Seq[Double],
  val termination: Termination with TerminationManifest { type G = GAGenomeWithSigma; type F = MGFitness; type MF = Rank with Diversity },
  val diversityMetric: EpsilonSigmaGA.DiversityMetricBuilder) extends NSGAIISigma
    with BinaryTournamentSelection
    with NonDominatedElitism
    with CoEvolvingSigmaValuesMutation
    with SBXBoundedCrossover
    with ParetoRanking
    with EpsilonDominance
    with RankDiversityModifier
    with EvolutionManifest
    with TerminationManifest
    with NoArchive {

  type STATE = termination.STATE

  val gManifest = manifest[G]
  val individualManifest = manifest[Individual[G, F]]
  val populationManifest = manifest[Population[G, F, MF]]
  val aManifest = manifest[A]
  val fManifest = manifest[F]
  val stateManifest = termination.stateManifest

  def initialState(p: Population[G, F, MF]): STATE = termination.initialState(p)
  def terminated(population: Population[G, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
  def diversity(individuals: Seq[DIVERSIFIED], ranks: Seq[Lazy[Int]]): Seq[Lazy[Double]] = diversityMetric(this).diversity(individuals, ranks)
}