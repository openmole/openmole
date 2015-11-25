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

import fr.iscpif.mgo
import fr.iscpif.mgo._
import org.openmole.core.workflow.builder.TaskBuilder

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

import scalaz.Tag

object TerminationTask {

  def apply[T](t: T, termination: OMTermination)(implicit integration: WorkflowIntegration[T]) = {
    val wfi = integration(t)
    import wfi._

    val term = OMTermination.toTermination(termination, wfi)

    new TaskBuilder {
      builder ⇒
      addInput(statePrototype)
      addOutput(statePrototype)
      addOutput(terminatedPrototype)
      addOutput(generationPrototype)

      abstract class TerminationTask extends Task {

        override def process(context: Context)(implicit rng: RandomProvider) = {
          val (newState, t) = term.run(context(statePrototype))

          Context(
            Variable(terminatedPrototype, t),
            Variable(statePrototype, newState),
            Variable(generationPrototype, Tag.unwrap(mgo.generation.get(newState)))
          )
        }

      }

      def toTask = new TerminationTask with Built
    }
  }

}

