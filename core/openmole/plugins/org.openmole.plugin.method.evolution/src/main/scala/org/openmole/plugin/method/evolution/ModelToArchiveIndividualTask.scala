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

object ModelToArchiveIndividualTask {

  def apply(evolution: Evolution with G with F with Archive { type F = MGFitness })(
    name: String,
    genome: Prototype[evolution.G],
    individual: Prototype[Individual[evolution.G, evolution.F]],
    newArchive: Prototype[evolution.A])(implicit plugins: PluginSet) =
    new TaskBuilder { builder ⇒

      private var objectives = new ListBuffer[(Prototype[Double], Double)]

      def addObjective(p: Prototype[Double], v: Double) = {
        this addInput p
        objectives += (p -> v)
        this
      }

      addInput(genome)
      addOutput(individual.toArray)
      addOutput(newArchive)

      val (_genome, _individual, _newArchive) = (genome, individual, newArchive)

      def toTask = new ModelToArchiveIndividualTask(evolution)(name) {
        val genome = _genome.asInstanceOf[Prototype[evolution.G]]
        val individual = _individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.F]]]
        val newArchive = _newArchive.asInstanceOf[Prototype[evolution.A]]

        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
        val objectives = builder.objectives.toList
      }
    }

}

sealed abstract class ModelToArchiveIndividualTask(val evolution: Evolution with G with F { type F = MGFitness })(
    val name: String)(implicit val plugins: PluginSet) extends Task { task ⇒

  def genome: Prototype[evolution.G]
  def individual: Prototype[Individual[evolution.G, evolution.F]]
  def newArchive: Prototype[evolution.A]

  def objectives: List[(Prototype[Double], Double)]

  override def process(context: Context) = {
    val i: Individual[evolution.G, evolution.F] =
      Individual(
        context.valueOrException(task.genome),
        MGFitness(
          objectives.map {
            case (o, v) ⇒ math.abs(context.valueOrException(o) - v)
          }))
    val individuals = Seq(i).toArray
    Context(
      Variable(individual.toArray, individuals),
      Variable(newArchive, evolution.toArchive(individuals.toSeq)))
  }
}
