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
import org.openmole.plugin.method.evolution.algorithm._
import org.openmole.plugin.method.evolution._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.plugin.method.evolution._

trait GAAlgorithm extends Archive
    with EvolutionManifest
    with G with P with F with MF with MG with GA with DoubleSeqPhenotype with MGFitness
    with Modifier
    with Elitism
    with Selection
    with Termination
    with Mutation
    with CrossOver
    with GeneticBreeding
    with CloneRemoval
    with InputsConverter {
  def objectives: Objectives
  def inputsPrototypes = inputs.inputs.map(_.prototype)
  def outputPrototypes = objectives
  def toVariables(genome: G, context: Context): Seq[Variable[_]] = scaled(values.get(genome), context)
  def toVariables(individuals: Seq[Individual[G, P, F]], context: Context): Seq[Variable[_]] = {
    val scaledValues = individuals.map(i ⇒ scaled(values.get(i.genome), context).toIndexedSeq)

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
            individuals.map { iv ⇒ fitness.get(iv.fitness)(i) }.toArray)
      }
  }
}