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

import monocle.macros.Lenses
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.dsl
import dsl._

object ToOffspringTask {

  implicit def isBuilder = TaskBuilder[ToOffspringTask].from(this)

  def apply[T](algorithm: T)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    new ToOffspringTask(t) set (
      dsl.inputs += (t.genomePrototype, t.statePrototype),
      dsl.outputs += (t.statePrototype, t.offspringPrototype),
      dsl.outputs += (t.outputPrototypes: _*)
    )
  }

}

@Lenses case class ToOffspringTask(
    t:        EvolutionWorkflow,
    inputs:   PrototypeSet      = PrototypeSet.empty,
    outputs:  PrototypeSet      = PrototypeSet.empty,
    defaults: DefaultSet        = DefaultSet.empty,
    name:     Option[String]    = None
) extends Task {
  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    val i = t.buildIndividual(context(t.genomePrototype), context)
    Context(Variable(t.offspringPrototype, Vector(i)))
  }
}
