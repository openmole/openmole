/*
 * Copyright (C) 28/11/12 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo._
import org.openmole.core.model.task._
import org.openmole.core.model.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.data._

object ToArchiveTask {

  def apply(evolution: G with F with MG with Archive)(
    name: String,
    individuals: Prototype[Array[Individual[evolution.G, evolution.F]]],
    archive: Prototype[evolution.A])(implicit plugins: PluginSet) = {

    val (_individuals, _archive) = (individuals, archive)

    new TaskBuilder { builder ⇒

      addInput(individuals)
      addOutput(archive)

      def toTask = new ToArchiveTask(evolution)(name) {
        val individuals = _individuals.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.F]]]]
        val archive = _archive.asInstanceOf[Prototype[evolution.A]]
        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }

    }
  }

}

sealed abstract class ToArchiveTask(val evolution: G with F with MG with Archive)(
    val name: String)(implicit val plugins: PluginSet) extends Task { task ⇒

  def individuals: Prototype[Array[Individual[evolution.G, evolution.F]]]
  def archive: Prototype[evolution.A]

  override def process(context: Context) = {
    Context(
      Variable(archive, evolution.toArchive(context.valueOrException(individuals))))
  }
}