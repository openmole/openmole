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
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object InitialStateTask {

  def apply[T](algorithm: T)(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name) = {
    val t = wfi(algorithm)

    ClosureTask("InitialStateTask") { (context, _, _) ⇒
      Context(
        Variable(t.statePrototype, t.operations.startTimeLens.set(System.currentTimeMillis)(context(t.statePrototype)))
      )
    } set (
      inputs += (t.statePrototype, t.populationPrototype),
      outputs += (t.statePrototype, t.populationPrototype),
      t.statePrototype := FromContext(p ⇒ t.operations.initialState(p.random())),
      t.populationPrototype := Array.empty[t.I](t.integration.iManifest)
    )
  }

}
