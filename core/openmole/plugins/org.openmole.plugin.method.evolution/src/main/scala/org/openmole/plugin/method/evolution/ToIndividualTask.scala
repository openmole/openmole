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
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.tools.VariableExpansion

object ToIndividualTask {

  def expand(objectives: List[(Prototype[Double], String)], context: Context): List[(Prototype[Double], Double)] =
    if (objectives.isEmpty) List.empty
    else {
      val (p, v) = objectives.head
      val vDouble = VariableExpansion(context, v).toDouble
      (p, vDouble) :: expand(objectives.tail, context + Variable(p, vDouble))
    }

  def apply(evolution: G with F with MG)(
    name: String,
    genome: Prototype[evolution.G],
    individual: Prototype[Individual[evolution.G, evolution.F]])(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒

      private var objectives = new ListBuffer[(Prototype[Double], String)]

      def addObjective(p: Prototype[Double], v: String) = {
        this addInput p
        objectives += (p -> v)
        this
      }

      addInput(genome)
      addOutput(individual)

      val (_genome, _individual) = (genome, individual)

      def toTask = new ToIndividualTask(evolution)(name) {
        val genome = _genome.asInstanceOf[Prototype[evolution.G]]
        val individual = _individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.F]]]

        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
        val objectives = builder.objectives.toList
      }
    }

}

sealed abstract class ToIndividualTask(val evolution: G with F with MG)(
    val name: String)(implicit val plugins: PluginSet) extends Task { task ⇒

  def genome: Prototype[evolution.G]
  def individual: Prototype[Individual[evolution.G, evolution.F]]

  def objectives: List[(Prototype[Double], String)]

  override def process(context: Context) = {
    val i: Individual[evolution.G, evolution.F] =
      Individual(
        context(task.genome),
        MGFitness(
          ToIndividualTask.expand(objectives.toList, context).map {
            case (o, v) ⇒ math.abs(context(o) - v)
          }))
    Context(Variable(individual, i))
  }
}
