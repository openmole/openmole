/*
 * Copyright (C) 24/11/12 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import fr.iscpif.mgo._
import monocle.macros.Lenses
import org.openmole.core.workflow.dsl
import dsl._

object BreedTask {
  implicit def isBuilder = TaskBuilder[BreedTask].from(this)

  def apply[T: WorkflowIntegration](algorithm: T, size: Int)(implicit wfi: WorkflowIntegration[T]): BreedTask = {
    val t = wfi(algorithm)

    new BreedTask(t, size) set (
      dsl.inputs += (t.populationPrototype, t.statePrototype),
      dsl.outputs += (t.statePrototype),
      dsl.exploredOutputs += (t.genomePrototype.array)
    )
  }

}

@Lenses case class BreedTask(
    t:        EvolutionWorkflow,
    size:     Int,
    inputs:   PrototypeSet      = PrototypeSet.empty,
    outputs:  PrototypeSet      = PrototypeSet.empty,
    defaults: DefaultSet        = DefaultSet.empty,
    name:     Option[String]    = None
) extends Task {

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
    val p = context(t.populationPrototype)

    if (p.isEmpty) {
      val s = context(t.statePrototype)
      val (news, gs) = t.integration.run(s, t.operations.initialGenomes(size))

      Context(
        Variable(t.genomePrototype.array, gs.toArray(t.genomePrototype.`type`.manifest)),
        Variable(t.statePrototype, news)
      )
    }
    else {
      val s = context(t.statePrototype)
      val (newState, breeded) = t.integration.run(s, t.operations.breeding(size).run(p))

      Context(
        Variable(t.genomePrototype.array, breeded.toArray(t.genomePrototype.`type`.manifest)),
        Variable(t.statePrototype, newState)
      )
    }
  }

}