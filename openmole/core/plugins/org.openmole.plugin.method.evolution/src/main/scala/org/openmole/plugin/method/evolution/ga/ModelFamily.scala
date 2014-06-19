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
import fr.iscpif.mgo.modelfamily._
import org.openmole.plugin.method.evolution._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._

object ModelFamily {

  def apply(
    mu: Int,
    lambda: Int,
    models: Int,
    nicheSize: Int,
    termination: GATermination { type G >: ModelFamily#G; type P >: ModelFamily#P; type F >: ModelFamily#F; type MF >: ModelFamily#MF },
    modelId: Prototype[Int],
    inputs: Inputs,
    objectives: Objectives,
    cloneProbability: Double = 0.0) = {
    val (_mu, _cloneProbability, _lambda, _inputs, _objectives, _nicheSize, _models, _modelId) = (mu, cloneProbability, lambda, inputs, objectives, nicheSize, models, modelId)
    new ModelFamily {
      val inputs = _inputs
      val objectives = _objectives
      val stateManifest: Manifest[STATE] = termination.stateManifest
      val populationManifest: Manifest[Population[G, P, F, MF]] = implicitly
      val individualManifest: Manifest[Individual[G, P, F]] = implicitly
      val aManifest: Manifest[A] = implicitly
      val fManifest: Manifest[F] = implicitly
      val gManifest: Manifest[G] = implicitly

      val genomeSize = inputs.size
      val lambda = _lambda
      val nicheSize = _nicheSize
      val models = _models

      override val cloneProbability: Double = _cloneProbability

      val mu = _mu
      type STATE = termination.STATE
      def initialState: STATE = termination.initialState
      def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)

      override def inputsPrototypes = super.inputsPrototypes ++ Seq(_modelId)

      override def toVariables(genome: G, context: Context): Seq[Variable[_]] =
        super.toVariables(genome, context) ++ Seq(Variable(_modelId, modelId.get(genome)))

      override def toVariables(individuals: Seq[Individual[G, P, F]], context: Context): Seq[Variable[_]] =
        super.toVariables(individuals, context) ++ Seq(Variable(_modelId.toArray, individuals.map(i ⇒ modelId.get(i.genome)).toArray))
    }
  }
}

trait ModelFamily extends NoArchive
  with RankModifier
  with GAAlgorithm
  with ModelFamilyElitism
  with ModelFamilyMutation
  with SBXBoundedCrossover
  with MaxAggregation
  with BinaryTournamentSelection
  with TournamentOnRank
  with GeneticBreeding
  with HierarchicalRanking
  with ModelFamilyGenome