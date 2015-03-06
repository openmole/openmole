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

package org.openmole.plugin.method.modelfamily

import fr.iscpif.mgo._
import org.openmole.core.workflow.data._
import org.openmole.plugin.method.evolution._
import ga._

import scala.util.Random

object ModelFamilyCalibration {

  def apply(modelFamily: ModelFamily)(
    nicheSize: Int,
    termination: GATermination { type G >: ModelFamilyCalibration#G; type P >: ModelFamilyCalibration#P; type F >: ModelFamilyCalibration#F },
    reevaluate: Double = 0.0) = {
    val (_reevaluate, _nicheSize, _modelFamily) = (reevaluate, nicheSize, modelFamily)
    new ModelFamilyCalibration {
      val inputs = Inputs(modelFamily.attributes.map(_.toInput))
      val objectives = modelFamily.objectives
      val stateManifest: Manifest[STATE] = termination.stateManifest
      val populationManifest = implicitly[Manifest[Population[G, P, F]]]
      val individualManifest = implicitly[Manifest[Individual[G, P, F]]]
      val aManifest = implicitly[Manifest[A]]
      val fManifest = implicitly[Manifest[F]]
      val gManifest = implicitly[Manifest[G]]

      val genomeSize = inputs.size
      val nicheSize = _nicheSize
      val models = modelFamily.size

      override val cloneProbability: Double = _reevaluate

      type STATE = termination.STATE
      def initialState: STATE = termination.initialState
      def terminated(population: Population[G, P, F], terminationState: STATE)(implicit rng: Random): (Boolean, STATE) = termination.terminated(population, terminationState)

      override def inputsPrototypes = super.inputsPrototypes ++ Seq(modelFamily.modelIdPrototype)

      override def toVariables(genome: G, context: Context): Seq[Variable[_]] =
        super.toVariables(genome, context) ++ Seq(Variable(modelFamily.modelIdPrototype, modelId.get(genome)))

      override def toVariables(population: Population[G, P, F], context: Context): Seq[Variable[_]] =
        super.toVariables(population, context) ++ Seq(Variable(modelFamily.modelIdPrototype.toArray, population.map(i ⇒ modelId.get(i.genome)).toArray))

      def modelFamily = _modelFamily
    }
  }
}

trait ModelFamilyCalibration extends NoArchive
    with GAAlgorithm
    with ModelFamilyElitism
    with ModelFamilyMutation
    with DynamicGACrossover
    with BinaryTournamentSelection
    with TournamentOnRankAndDiversity
    with GeneticBreeding
    with ParetoRanking
    with FitnessCrowdingDiversity
    with ModelFamilyGenome
    with NonStrictDominance
    with ClampedGenome {
  def modelFamily: ModelFamily
}