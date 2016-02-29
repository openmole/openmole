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
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._

object ToOffspringTask {

  def apply[T](algorithm: T)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    new TaskBuilder {
      builder ⇒
      t.outputPrototypes.foreach(p ⇒ addInput(p))
      addInput(t.genomePrototype)
      addInput(t.statePrototype)
      addOutput(t.statePrototype)
      addOutput(t.offspringPrototype)

      abstract class ToOffspringTask extends Task {
        override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
          val i = t.buildIndividual(context(t.genomePrototype), context)
          Context(Variable(t.offspringPrototype, Vector(i)))
        }
      }

      def toTask = new ToOffspringTask with Built
    }
  }
}

