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

import scala.util.Random

object GenomeProfile {

  def apply(
    x: Int,
    nX: Int,
    termination: GATermination { type G >: GenomeProfile#G; type P >: GenomeProfile#P; type F >: GenomeProfile#F },
    inputs: Inputs,
    objective: Objective,
    reevaluate: Double = 0.0) = {
    val (_x, _nX, _reevaluate, _inputs) = (x, nX, reevaluate, inputs)
    new GenomeProfile {
      val inputs = _inputs
      val objectives = Seq(objective)

      val stateManifest: Manifest[STATE] = termination.stateManifest
      val populationManifest = implicitly[Manifest[Population[G, P, F]]]
      val individualManifest = implicitly[Manifest[Individual[G, P, F]]]
      val aManifest = implicitly[Manifest[A]]
      val fManifest = implicitly[Manifest[F]]
      val gManifest = implicitly[Manifest[G]]

      val genomeSize = inputs.size
      override val cloneProbability: Double = _reevaluate

      val x = _x
      val nX = _nX
      type STATE = termination.STATE
      def initialState: STATE = termination.initialState
      def terminated(population: Population[G, P, F], terminationState: STATE)(implicit rng: Random): (Boolean, STATE) = termination.terminated(population, terminationState)
    }

  }

}

trait GenomeProfile extends GAAlgorithm
    with DynamicGACrossover
    with DynamicGAMutation
    with ProfileRanking
    with BestAggregatedNicheElitism
    with ProfileNiche
    with NoArchive
    with NoDiversity
    with ProfileGenomePlotter
    with BinaryTournamentSelection
    with selection.ProportionalNumberOfRound
    with TournamentOnRank
    with GeneticBreeding
    with MGFitness
    with MaxAggregation
    with ClampedGenome
    with GAGenomeWithSigma {
  def x: Int
}