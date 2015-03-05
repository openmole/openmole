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

object GenomeMap {

  def apply(
    x: Int,
    nX: Int,
    y: Int,
    nY: Int,
    termination: GATermination { type G >: GenomeMap#G; type P >: GenomeMap#P; type F >: GenomeMap#F },
    inputs: Inputs,
    objectives: Objectives,
    reevaluate: Double = 0.0) = {
    val (_x, _nX, _y, _nY, _reevaluate, _inputs, _objectives) = (x, nX, y, nY, reevaluate, inputs, objectives)
    new GenomeMap {
      val inputs = _inputs
      val objectives = _objectives

      val stateManifest: Manifest[STATE] = termination.stateManifest
      val populationManifest = implicitly[Manifest[Population[G, P, F]]]
      val individualManifest = implicitly[Manifest[Individual[G, P, F]]]
      val aManifest = implicitly[Manifest[A]]
      val fManifest = implicitly[Manifest[F]]
      val gManifest = implicitly[Manifest[G]]

      val genomeSize = inputs.size
      override val cloneProbability: Double = _reevaluate

      val x = _x
      val y = _y
      val nX = _nX
      val nY = _nY

      type STATE = termination.STATE

      def initialState: STATE = termination.initialState
      def terminated(population: Population[G, P, F], terminationState: STATE)(implicit rng: Random): (Boolean, STATE) = termination.terminated(population, terminationState)

    }

  }

}

trait GenomeMap extends GAAlgorithm
    with DynamicGACrossover
    with DynamicGAMutation
    with BestAggregatedNicheElitism
    with MapNiche
    with MapGenomePlotter
    with NoArchive
    with NoRanking
    with MapSelection
    with MaxAggregation
    with GeneticBreeding
    with ClampedGenome
    with GAGenomeWithSigma {

  def x: Int
  def y: Int
}