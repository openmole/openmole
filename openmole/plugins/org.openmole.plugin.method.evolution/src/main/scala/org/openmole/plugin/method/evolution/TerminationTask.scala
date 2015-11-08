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
import org.openmole.core.workflow.builder.TaskBuilder

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

import scalaz.Tag

object TerminationTask {

  def apply(algorithm: Algorithm)(
    termination: Termination[algorithm.AlgorithmState],
    state: Prototype[algorithm.AlgorithmState],
    generation: Prototype[Long],
    terminated: Prototype[Boolean]) = {
    val (_state, _generation, _termination, _terminated) = (state, generation, termination, terminated)

    new TaskBuilder { builder ⇒
      addInput(state)
      addOutput(state)
      addOutput(terminated)
      addOutput(generation)

      def toTask = new TerminationTask(algorithm) with Built {
        val state = _state.asInstanceOf[Prototype[algorithm.AlgorithmState]]
        val generation = _generation
        val termination = _termination.asInstanceOf[Termination[algorithm.AlgorithmState]]
        val terminated = _terminated
      }
    }
  }
}

abstract class TerminationTask(val algorithm: Algorithm) extends Task {

  def generation: Prototype[Long]
  def state: Prototype[algorithm.AlgorithmState]
  def termination: Termination[algorithm.AlgorithmState]
  def terminated: Prototype[Boolean]

  override def process(context: Context)(implicit rng: RandomProvider) = {
    val (newState, t) = termination.run(context(state))

    Context(
      Variable(terminated, t),
      Variable(state, newState),
      Variable(generation, Tag.unwrap(algorithm.generation.get(newState)))
    )
  }

}
