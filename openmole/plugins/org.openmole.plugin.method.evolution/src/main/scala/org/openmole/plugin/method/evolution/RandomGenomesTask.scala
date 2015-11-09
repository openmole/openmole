/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import scala.util.Random
import scalaz._

object RandomGenomesTask {

  def apply(algorithm: Algorithm)(
    size: Int,
    genome: Prototype[algorithm.G],
    state: Prototype[algorithm.AlgorithmState],
    randomGenome: State[Random, algorithm.G]) = {

    val (_genome, _state, _randomGenome) = (genome, state, randomGenome)

    new TaskBuilder {
      addInput(state)
      addOutput(state)
      addExploredOutput(genome.toArray)
      setDefault(Default(state, ctx ⇒ algorithm.algorithmState(Task.buildRNG(ctx))))

      override def toTask: Task = new RandomGenomesTask(algorithm, size) with Built {
        val genome = _genome.asInstanceOf[Prototype[algorithm.G]]
        val randomGenome = _randomGenome.asInstanceOf[State[Random, algorithm.G]]
        val state = _state.asInstanceOf[Prototype[algorithm.AlgorithmState]]
      }
    }

  }

}

abstract class RandomGenomesTask(val algorithm: Algorithm, size: Int) extends Task {

  def genome: Prototype[algorithm.G]
  def randomGenome: State[Random, algorithm.G]
  def state: Prototype[algorithm.AlgorithmState]

  override protected def process(context: Context)(implicit rng: RandomProvider): Context = {
    val s = context(state)
    val (news, gs) = (algorithm.random lifts randomGenomes(randomGenome, size)).run(s)

    Context(
      Variable(state, news),
      Variable(genome.toArray, gs.toArray(genome.`type`.manifest))
    )
  }
}
