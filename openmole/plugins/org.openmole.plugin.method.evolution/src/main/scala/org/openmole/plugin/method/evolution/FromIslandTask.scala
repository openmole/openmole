/**
 * Created by Romain Reuillon on 21/01/16.
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

import org.openmole.core.context.Variable
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._

object FromIslandTask {

  def apply[T](evolution: EvolutionWorkflow)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("FromIslandTask") { (context, _, _) ⇒
      val population = evolution.operations.migrateFromIsland(context(evolution.populationPrototype).toVector, context(evolution.statePrototype))
      Variable(evolution.populationPrototype, population.toArray(evolution.individualPrototype.`type`.manifest))
    } set (
      inputs += (evolution.populationPrototype, evolution.statePrototype),
      outputs += evolution.populationPrototype
    )

}
