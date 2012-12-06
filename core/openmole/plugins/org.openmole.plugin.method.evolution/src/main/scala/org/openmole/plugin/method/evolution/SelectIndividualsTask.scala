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

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.task.Task._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.tools.service.Random._

import fr.iscpif.mgo._

object SelectIndividualsTask {

  def apply(evolution: Modifier with G with MF with F)(
    name: String,
    individuals: Prototype[Array[Individual[evolution.G, evolution.F]]],
    size: Int)(implicit plugins: PluginSet) = {
    val (_individuals) = (individuals)

    new TaskBuilder { builder ⇒
      addInput(individuals)
      addOutput(individuals)

      def toTask =
        new SelectIndividualsTask(name, evolution, size) {
          val individuals = _individuals.asInstanceOf[Prototype[Array[Individual[evolution.G, evolution.F]]]]
          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
        }
    }

  }

}

sealed abstract class SelectIndividualsTask(
    val name: String,
    val evolution: Modifier with G with MF with F,
    val size: Int)(implicit val plugins: PluginSet) extends Task {

  def individuals: Prototype[Array[Individual[evolution.G, evolution.F]]]

  override def process(context: Context) = {
    implicit val rng = newRNG(context(openMOLESeed))
    val is = context(individuals)
    val newIs = (0 until size).map { i ⇒ is(rng.nextInt(is.size)) }.toArray
    Variable(individuals, newIs)
  }

}
