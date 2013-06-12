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

object ScalingGAIndividualsTask {

  def apply[G <: GAGenome, P, F <: MGFitness, MF](
    name: String,
    individuals: Prototype[Array[Individual[G, P, F]]],
    scales: (GenomeScaling.Scale)*)(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒

      private var objectives = new ListBuffer[Prototype[Double]]

      def addObjective(p: Prototype[Double]) = {
        objectives += p
        this addOutput p.toArray
        this
      }

      addInput(individuals)
      scales foreach { case (p, _) ⇒ this addOutput p.toArray }

      def toTask = new ScalingGAIndividualsTask(name, individuals, scales) with Built {
        val objectives = builder.objectives.toList
      }
    }

}

sealed abstract class ScalingGAIndividualsTask[G <: GAGenome, P, F <: MGFitness, MF](
    val name: String,
    val individuals: Prototype[Array[Individual[G, P, F]]],
    val scales: Seq[GenomeScaling.Scale]) extends Task with GenomeScaling {

  def objectives: List[Prototype[Double]]

  override def process(context: Context) = {
    val individualsValue = context(individuals)
    val genomeValues =
      individualsValue.toArray.map {
        i ⇒ scaled(i.genome.values, context).map(_.value).toArray
      }.transpose

    (genomeValues zip scales.map(_._1)).map { case (g, p) ⇒ Variable(p.toArray, g) }.toList ++
      objectives.zipWithIndex.map {
        case (p, i) ⇒
          Variable(
            p.toArray,
            individualsValue.map { _.fitness.values(i) }.toArray)
      }
  }

}
