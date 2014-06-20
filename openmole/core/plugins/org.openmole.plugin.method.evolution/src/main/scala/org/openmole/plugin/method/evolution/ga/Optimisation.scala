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

object Optimisation {

  def apply(
    mu: Int,
    termination: GATermination { type G >: Optimisation#G; type P >: Optimisation#P; type F >: Optimisation#F; type MF >: Optimisation#MF },
    inputs: Inputs[String],
    objectives: Objectives,
    dominance: Dominance = Strict,
    ranking: GARankingBuilder = Pareto,
    diversityMetric: DiversityMetricBuilder = Crowding,
    cloneProbability: Double = 0.0) = {
    val (_mu, _ranking, _diversityMetric, _cloneProbability, _inputs, _objectives) = (mu, ranking, diversityMetric, cloneProbability, inputs, objectives)
    new Optimisation {
      val inputs = _inputs
      val objectives = _objectives
      val stateManifest: Manifest[STATE] = termination.stateManifest
      val populationManifest: Manifest[Population[G, P, F, MF]] = implicitly
      val individualManifest: Manifest[Individual[G, P, F]] = implicitly
      val aManifest: Manifest[A] = implicitly
      val fManifest: Manifest[F] = implicitly
      val gManifest: Manifest[G] = implicitly

      val genomeSize = inputs.size

      override val cloneProbability: Double = _cloneProbability

      val diversityMetric = _diversityMetric(dominance)
      val ranking = _ranking(dominance)
      val mu = _mu
      def diversity(individuals: Seq[Seq[Double]]) = diversityMetric.diversity(individuals)
      def rank(individuals: Seq[Seq[Double]]) = ranking.rank(individuals)
      type STATE = termination.STATE
      def initialState: STATE = termination.initialState
      def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
    }
  }
}

trait Optimisation extends NoArchive
  with RankDiversityModifier
  with GAAlgorithm
  with NonDominatedElitism
  with BinaryTournamentSelection
  with TournamentOnRankAndDiversity
  with CoEvolvingSigmaValuesMutation
  with SBXBoundedCrossover
  with GAGenomeWithSigma
  with GenomeScalingFromGroovy

