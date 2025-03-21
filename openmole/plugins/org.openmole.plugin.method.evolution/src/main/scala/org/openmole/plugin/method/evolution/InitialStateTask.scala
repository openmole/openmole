/**
 * Created by Romain Reuillon on 20/01/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
 *
 */
package org.openmole.plugin.method.evolution

import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.argument.FromContext
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object InitialStateTask:

  def apply(evolution: EvolutionWorkflow)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("InitialStateTask"): p =>
      import p._
      def initialisedState =
        evolution.startTimeLens.set(System.currentTimeMillis) andThen
          evolution.generationLens.set(0L) andThen
          evolution.evaluatedLens.set(0L) apply context(evolution.stateVal)

      Context(Variable(evolution.stateVal, initialisedState))
    .set (
      inputs += (evolution.stateVal, evolution.populationVal),
      outputs += (evolution.stateVal, evolution.populationVal),
      evolution.stateVal := FromContext(p => evolution.operations.initialState),
      evolution.populationVal := Array.empty[evolution.I]
    )

