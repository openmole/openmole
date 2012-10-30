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

object ScalingGAArchiveTask {

  def apply[G <: GAGenome, MF](
    name: String,
    archive: Prototype[Population[G, MF]],
    modelInputs: (Prototype[Double], (Double, Double))*)(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒

      private var objectives = new ListBuffer[Prototype[Double]]

      def addObjective(p: Prototype[Double]) = {
        objectives += p
        this addOutput p.toArray
        this
      }

      addInput(archive)
      modelInputs foreach { case (p, _) ⇒ this addOutput p.toArray }

      def toTask = new ScalingGAArchiveTask(name, archive, modelInputs: _*) {
        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
        val objectives = builder.objectives.toList
      }
    }

}

sealed abstract class ScalingGAArchiveTask[G <: GAGenome, MF](
    val name: String,
    archive: Prototype[Population[G, MF]],
    modelInputs: (Prototype[Double], (Double, Double))*)(implicit val plugins: PluginSet) extends Task {

  def objectives: List[Prototype[Double]]

  override def process(context: Context) = {
    val archiveValue = context.valueOrException(archive)

    (
      modelInputs.zipWithIndex.map {
        case ((prototype, (min, max)), i) ⇒
          Variable(
            prototype.toArray,
            archiveValue.map {
              _.genome.values(i).scale(min, max)
            }.toArray)
      } ++
      objectives.zipWithIndex.map {
        case (p, i) ⇒
          Variable(
            p.toArray,
            archiveValue.map { _.fitness.values(i) }.toArray)
      }).toContext
  }

}
