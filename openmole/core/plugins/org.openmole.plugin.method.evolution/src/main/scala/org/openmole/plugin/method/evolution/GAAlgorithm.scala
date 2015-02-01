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
import org.openmole.core.workflow.data._

trait GAAlgorithm extends Archive
    with EvolutionManifest
    with G with P with F with GA with DoubleSeqPhenotype with MGFitness
    with Elitism
    with Termination
    with Breeding
    with CloneRemoval
    with InputsConverter {
  def objectives: Objectives
  def inputsPrototypes = inputs.inputs.map(_.prototype)
  def outputPrototypes = objectives
  def toVariables(genome: G, context: Context): Seq[Variable[_]] = scaled(values.get(genome), context)
  def toVariables(population: Population[G, P, F], context: Context): Seq[Variable[_]] = {
    val scaledValues = population.map(i ⇒ scaled(values.get(i.genome), context).toIndexedSeq)

    inputs.inputs.zipWithIndex.map {
      case (input, i) ⇒
        input match {
          case Scalar(prototype, _, _)      ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Double]).toArray[Double])
          case Sequence(prototype, _, _, _) ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Array[Double]]).toArray[Array[Double]])
        }
    }.toList ++
      objectives.zipWithIndex.map {
        case (p, i) ⇒
          Variable(
            p.toArray,
            population.map { iv ⇒ fitness(iv.toIndividual)(i) }.toArray)
      }
  }
}