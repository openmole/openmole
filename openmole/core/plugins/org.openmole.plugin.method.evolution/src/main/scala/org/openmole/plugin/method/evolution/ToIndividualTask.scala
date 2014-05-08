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

import org.openmole.plugin.method.evolution.algorithm.DoubleSequencePhenotype
import fr.iscpif.mgo._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.tools.VariableExpansion
import algorithm.{ GA ⇒ OMGA }

object ToIndividualTask {

  def expand(objectives: List[(Prototype[Double], String)], context: Context): List[(Prototype[Double], Double)] =
    if (objectives.isEmpty) List.empty
    else {
      val (p, v) = objectives.head
      val vDouble = VariableExpansion(context, v).toDouble
      (p, vDouble) :: expand(objectives.tail, context + Variable(p, vDouble))
    }

  def apply(evolution: OMGA[_])(
    name: String,
    genome: Prototype[evolution.G],
    individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]])(implicit plugins: PluginSet) = {

    new TaskBuilder { builder ⇒

      evolution.objectives.foreach {
        case (p, _) ⇒ addInput(p)
      }

      addInput(genome)
      addOutput(individual)

      val (_genome, _individual) = (genome, individual)

      def toTask = new ToIndividualTask(evolution)(name) with Built {
        val genome = _genome.asInstanceOf[Prototype[evolution.G]]
        val individual = _individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.P, evolution.F]]]
      }
    }
  }

}

sealed abstract class ToIndividualTask(val evolution: OMGA[_])(
    val name: String) extends Task { task ⇒

  def genome: Prototype[evolution.G]
  def individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]]

  override def process(context: Context) = {

    val scaled: List[(Prototype[Double], Double)] =
      ToIndividualTask.expand(evolution.objectives.toList, context).map {
        case (o, v) ⇒ o -> math.abs(context(o) - v)
      }

    val i: Individual[evolution.G, evolution.P, evolution.F] =
      Individual(
        context(task.genome),
        scaled.unzip._2,
        scaled.unzip._2)

    Context(Variable(individual, i))
  }
}
