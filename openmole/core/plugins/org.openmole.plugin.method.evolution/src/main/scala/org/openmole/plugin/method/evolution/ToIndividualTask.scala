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

import algorithm.ContextPhenotype
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

  def apply(evolution: G with ContextPhenotype with F with MG)(
    name: String,
    genome: Prototype[evolution.G],
    individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]],
    objectives: Seq[(Prototype[Double], String)])(implicit plugins: PluginSet) = {

    val (_objectives) = (objectives)

    new TaskBuilder { builder ⇒

      //      private var objectives = new ListBuffer[(Prototype[Double], String)]
      //
      //      def addObjective(p: Prototype[Double], v: String) = {
      //        this addInput p
      //        objectives += (p -> v)
      //        this
      //      }

      objectives.foreach {
        case (p, _) ⇒ addInput(p)
      }

      addInput(genome)
      addOutput(individual)

      val (_genome, _individual) = (genome, individual)

      def toTask = new ToIndividualTask(evolution)(name) with Built {
        val genome = _genome.asInstanceOf[Prototype[evolution.G]]
        val individual = _individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.P, evolution.F]]]

        val objectives = _objectives
      }
    }
  }

}

sealed abstract class ToIndividualTask(val evolution: G with ContextPhenotype with F with MG)(
    val name: String) extends Task { task ⇒

  def genome: Prototype[evolution.G]
  def individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]]

  def objectives: Seq[(Prototype[Double], String)]

  override def process(context: Context) = {

    val scaled: List[(Prototype[Double], Double)] =
      ToIndividualTask.expand(objectives.toList, context).map {
        case (o, v) ⇒ o -> math.abs(context(o) - v)
      }

    val i: Individual[evolution.G, evolution.P, evolution.F] =
      Individual(
        context(task.genome),
        context + scaled.map { case (p, v) ⇒ Variable(p, v) },
        MGFitness(scaled.map { _._2 }))

    Context(Variable(individual, i))
  }
}
