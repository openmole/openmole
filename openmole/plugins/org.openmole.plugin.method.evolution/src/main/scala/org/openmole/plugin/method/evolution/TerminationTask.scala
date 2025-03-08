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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object TerminationTask {
  import EvolutionWorkflow._

  def apply[T](evolution: EvolutionWorkflow, termination: OMTermination)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("TerminationTask") { p =>
      import p._
      val term = OMTermination.toTermination(termination, evolution)

      val state = context(evolution.stateVal)
      val te = term(state, context(evolution.populationVal).toVector)

      Context(
        evolution.terminatedVal -> te
      )
    } set (
      inputs += (evolution.stateVal, evolution.populationVal),
      outputs += (evolution.stateVal, evolution.terminatedVal)
    )

}

