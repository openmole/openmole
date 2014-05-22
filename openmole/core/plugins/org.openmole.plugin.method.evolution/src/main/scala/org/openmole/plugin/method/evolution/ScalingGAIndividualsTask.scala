/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.model.sampling._
import org.openmole.core.model.domain._
import org.openmole.core.model.task._
import scala.collection.mutable.ListBuffer
import org.openmole.plugin.method.evolution.algorithm.GA.GAType

object ScalingGAIndividualsTask {

  def apply(evolution: GA.GAAlgorithm)(
    name: String,
    individuals: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]])(implicit plugins: PluginSet) = {

    val (_evolution, _name, _individuals) = (evolution, name, individuals)

    new TaskBuilder { builder ⇒

      addInput(individuals)
      evolution.inputs.inputs foreach { i ⇒ this.addOutput(i.prototype.toArray) }

      def toTask = new ScalingGAIndividualsTask with Built {
        val evolution = _evolution
        val name = _name
        val individuals = _individuals.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]]
      }
    }
  }

}

sealed abstract class ScalingGAIndividualsTask extends Task with GenomeScaling {

  val evolution: GA.GAAlgorithm
  val individuals: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]
  def objectives = evolution.objectives.unzip._1
  def scales = evolution.inputs

  override def process(context: Context) = {
    val individualsValue = context(individuals)
    val scaledValues = individualsValue.map(i ⇒ scaled(evolution.values.get(i.genome), context).toIndexedSeq)

    scales.inputs.zipWithIndex.map {
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
            individualsValue.map { iv ⇒ evolution.fitness.get(iv.fitness)(i) }.toArray)
      }
  }

}
