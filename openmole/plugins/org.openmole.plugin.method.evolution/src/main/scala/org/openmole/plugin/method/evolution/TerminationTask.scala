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

  import EvolutionDSL._

  def apply[T](evolution: EvolutionWorkflow, termination: OMTermination)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("TerminationTask") { (context, _, _) ⇒
      val term = OMTermination.toTermination(termination, evolution)

      val state = context(evolution.statePrototype)
      val te = term(state, context(evolution.populationPrototype).toVector)

      Context(
        Variable(evolution.terminatedPrototype, te),
        Variable(evolution.generationPrototype, evolution.operations.generationLens.get(state))
      )
    } set (
      inputs += (evolution.statePrototype, evolution.populationPrototype),
      outputs += (evolution.statePrototype, evolution.terminatedPrototype, evolution.generationPrototype)
    )

}

