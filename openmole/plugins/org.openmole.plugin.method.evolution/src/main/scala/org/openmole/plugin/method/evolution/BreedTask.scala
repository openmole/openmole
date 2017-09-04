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

import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object BreedTask {

  def apply[T: WorkflowIntegration](algorithm: T, size: Int)(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name) = {
    val t = wfi(algorithm)

    ClosureTask("BreedTask") { (context, _, _) ⇒
      val p = context(t.populationPrototype)

      if (p.isEmpty) {
        val s = context(t.statePrototype)
        val (news, gs) = t.operations.initialGenomes(size).run(s).value

        Context(
          Variable(t.genomePrototype.array, gs.toArray(t.genomePrototype.`type`.manifest)),
          Variable(t.statePrototype, news)
        )
      }
      else {
        val s = context(t.statePrototype)
        val (newState, breeded) = t.operations.breeding(p, size).run(s).value

        Context(
          Variable(t.genomePrototype.array, breeded.toArray(t.genomePrototype.`type`.manifest)),
          Variable(t.statePrototype, newState)
        )
      }
    } set (
      inputs += (t.populationPrototype, t.statePrototype),
      outputs += (t.statePrototype),
      exploredOutputs += (t.genomePrototype.array)
    )
  }

}

