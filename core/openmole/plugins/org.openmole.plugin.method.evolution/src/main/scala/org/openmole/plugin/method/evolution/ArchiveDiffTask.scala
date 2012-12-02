/*
 * Copyright (C) 26/11/12 Romain Reuillon
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
import org.openmole.core.model.task.PluginSet
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._

object ArchiveDiffTask {

  def apply(evolution: Archive)(
    name: String,
    originalArchive: Prototype[evolution.A],
    newArchive: Prototype[evolution.A])(implicit plugins: PluginSet) = {

    val (_originalArchive, _newArchive) = (originalArchive, newArchive)

    new TaskBuilder { builder ⇒

      addInput(originalArchive)
      addInput(newArchive)
      addOutput(newArchive)

      def toTask =
        new ArchiveDiffTask(evolution)(name) {
          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters

          val originalArchive = _originalArchive.asInstanceOf[Prototype[evolution.A]]
          val newArchive = _newArchive.asInstanceOf[Prototype[evolution.A]]
        }
    }
  }

}

abstract sealed class ArchiveDiffTask(val evolution: Archive)(
    val name: String)(implicit val plugins: PluginSet) extends Task { task ⇒

  def originalArchive: Prototype[evolution.A]
  def newArchive: Prototype[evolution.A]

  override def process(context: Context) = {
    val diff = evolution.diff(
      context.valueOrException(originalArchive),
      context.valueOrException(newArchive))
    Context(Variable(newArchive, diff))
  }
}
