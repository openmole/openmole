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

object ElitismTask {

  def apply(evolution: Elitism with Termination with Modifier with Archive)(
    name: String,
    individuals: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]],
    archive: Prototype[evolution.A])(implicit plugins: PluginSet) = {
    val (_individuals, _archive) = (individuals, archive)

    new TaskBuilder { builder ⇒
      addInput(archive)
      addInput(individuals)
      addOutput(individuals)

      def toTask = new ElitismTask(name, evolution) with builder.Built {
        val individuals = _individuals.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]]
        val archive = _archive.asInstanceOf[Prototype[evolution.A]]
      }
    }
  }
}

sealed abstract class ElitismTask[E <: Elitism with Termination with Modifier with Archive](
    val name: String, val evolution: E) extends Task {

  def individuals: Prototype[Array[Individual[evolution.G, evolution.P, evolution.F]]]
  def archive: Prototype[evolution.A]

  override def process(context: Context) = {
    val a = context(archive)
    val newIndividuals = evolution.elitism(context(individuals), a)

    Context(
      Variable(individuals, newIndividuals.toArray))
  }

}
