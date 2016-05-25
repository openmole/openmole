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
import monocle.macros.Lenses
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.dsl
import dsl._

object ElitismTask {

  implicit def isBuilder = TaskBuilder[ElitismTask].from(this)

  def apply[T](algorithm: T)(implicit wfi: WorkflowIntegration[T]) = {
    val t = wfi(algorithm)

    new ElitismTask(t) set (
      dsl.inputs += (t.statePrototype, t.populationPrototype, t.offspringPrototype),
      dsl.outputs += (t.populationPrototype, t.statePrototype)
    )
  }

}

@Lenses case class ElitismTask(
    t:        EvolutionWorkflow,
    inputs:   PrototypeSet      = PrototypeSet.empty,
    outputs:  PrototypeSet      = PrototypeSet.empty,
    defaults: DefaultSet        = DefaultSet.empty,
    name:     Option[String]    = None
) extends Task {

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    val (newState, newPopulation) = t.integration.run(context(t.statePrototype), t.operations.elitism.run(context(t.populationPrototype) ++ context(t.offspringPrototype)))

    Context(
      Variable(t.populationPrototype, newPopulation),
      Variable(t.statePrototype, newState)
    )
  }

}
