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
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

object ToIndividualTask {

  def apply(evolution: GAAlgorithm)(
    genome: Prototype[evolution.G],
    individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]])(implicit plugins: PluginSet) = {

    new TaskBuilder { builder ⇒
      evolution.outputPrototypes.foreach(p ⇒ addInput(p))
      addInput(genome)
      addOutput(individual)

      val (_genome, _individual) = (genome, individual)

      def toTask = new ToIndividualTask(evolution) with Built {
        val genome = _genome.asInstanceOf[Prototype[evolution.G]]
        val individual = _individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.P, evolution.F]]]
      }
    }
  }

}

sealed abstract class ToIndividualTask(val evolution: GAAlgorithm) extends Task { task ⇒

  def genome: Prototype[evolution.G]
  def individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]]

  override def process(context: Context) = {
    val scaled: Seq[(Prototype[Double], Double)] = evolution.objectives.map(o ⇒ o -> context(o))

    val i: Individual[evolution.G, evolution.P, evolution.F] =
      Individual(
        context(task.genome),
        scaled.unzip._2,
        scaled.unzip._2)

    Context(Variable(individual, i))
  }
}
