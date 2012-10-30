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
import org.openmole.core.model.task._

object ArchiveToIndividualArrayTask {

  def apply[G <: Genome, MF](
    name: String,
    archive: Prototype[Population[G, MF]],
    individual: Prototype[Individual[G]])(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒

      addInput(archive)
      addOutput(individual.toArray)

      def toTask = new ArchiveToIndividualArrayTask(name, archive, individual) {
        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }
    }
}

sealed abstract class ArchiveToIndividualArrayTask[G <: Genome, MF](
    val name: String,
    archive: Prototype[Population[G, MF]],
    individual: Prototype[Individual[G]])(implicit val plugins: PluginSet) extends Task { task ⇒

  override def process(context: Context) =
    context + Variable(
      individual.toArray,
      context.valueOrException(archive).toIndividuals.toArray)

}