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

object ToOffspringTask {

  def apply(algorithm: GAAlgorithm)(
    genome: Prototype[algorithm.G],
    offspring: Prototype[algorithm.Pop],
    state: Prototype[algorithm.AlgorithmState]) = {

    new TaskBuilder { builder ⇒
      algorithm.objectives.foreach(p ⇒ addInput(p))
      addInput(genome)
      addInput(state)
      addOutput(state)
      addOutput(offspring)

      val (_genome, _offspring, _state) = (genome, offspring, state)

      def toTask = new ToOffspringTask(algorithm) with Built {
        val genome = _genome.asInstanceOf[Prototype[algorithm.G]]
        val offspring = _offspring.asInstanceOf[Prototype[algorithm.Pop]]
        val state = _state.asInstanceOf[Prototype[algorithm.AlgorithmState]]
      }
    }
  }

}

abstract class ToOffspringTask(val algorithm: GAAlgorithm) extends Task { task ⇒
  def genome: Prototype[algorithm.G]
  def offspring: Prototype[algorithm.Pop]
  def state: Prototype[algorithm.AlgorithmState]

  override def process(context: Context)(implicit rng: RandomProvider) = {
    val scaled: Seq[(Prototype[Double], Double)] = algorithm.objectives.map(o ⇒ o -> context(o))
    val phenotype: Seq[Double] = scaled.map(_._2)

    val i: algorithm.Ind =
      new Individual[algorithm.G, algorithm.P](
        context(task.genome),
        algorithm.toPhenotype(phenotype),
        born = algorithm.generation.get(context(state))
      )

    Context(Variable(offspring, Vector(i)))
  }
}
