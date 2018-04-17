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

import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object TerminationTask {

  def apply[T](algorithm: T, termination: OMTermination)(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    val t = wfi(algorithm)

    ClosureTask("TerminationTask") { (context, _, _) ⇒
      val term = OMTermination.toTermination(termination, t)

      val (newState, te) = term(context(t.populationPrototype).toVector).run(context(t.statePrototype)).value

      Context(
        Variable(t.terminatedPrototype, te),
        Variable(t.statePrototype, newState),
        Variable(t.generationPrototype, t.operations.generation(newState))
      )
    } set (
      inputs += (t.statePrototype, t.populationPrototype),
      outputs += (t.statePrototype, t.terminatedPrototype, t.generationPrototype)
    )
  }

}

