/*
 * Copyright (C) 2015 Romain Reuillon
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
import org.openmole.core.workflow.data.PrototypeType

import scala.util.Random

object SMSEMOEA {

  def apply(
    mu: Int,
    termination: GATermination { type G >: SMSEMOEA#G; type P >: SMSEMOEA#P; type F >: SMSEMOEA#F },
    inputs: Inputs,
    objectives: Objectives,
    referencePoint: Seq[Double],
    reevaluate: Double = 0.0) = {
    val (_mu, _reevaluate, _inputs, _objectives, _referencePoint) = (mu, reevaluate, inputs, objectives, referencePoint)
    new SMSEMOEA {
      val inputs = _inputs
      val objectives = _objectives
      val referencePoint = _referencePoint

      val stateType = termination.stateType
      val populationType = PrototypeType[Population[G, P, F]]
      val individualType = PrototypeType[Individual[G, P, F]]
      val aType = PrototypeType[A]
      val fType = PrototypeType[F]
      val gType = PrototypeType[G]

      val genomeSize = inputs.size

      override val cloneProbability: Double = _reevaluate

      val mu = _mu
      type STATE = termination.STATE
      def initialState: STATE = termination.initialState
      def terminated(population: Population[G, P, F], terminationState: STATE)(implicit rng: Random): (Boolean, STATE) = termination.terminated(population, terminationState)
    }
  }
}

trait SMSEMOEA <: GAAlgorithm
  with GAGenomeWithSigma
  with MG
  with BinaryTournamentSelection
  with TournamentOnRank
  with HypervolumeRanking
  with RankElitism
  with DynamicGACrossover
  with DynamicGAMutation
  with FitnessHypervolumeDiversity
  with NoArchive
  with GeneticBreeding
  with ClampedGenome