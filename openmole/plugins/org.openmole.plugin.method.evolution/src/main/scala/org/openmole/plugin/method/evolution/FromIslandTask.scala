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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
object FromIslandTask:

  def apply[T](evolution: EvolutionWorkflow, islandStateVal: Val[evolution.S], initialIslandStateVal: Val[evolution.S])(using sourcecode.Name, DefinitionScope) =
    Task("FromIslandTask"): p =>
      import p._
      val state = context(evolution.stateVal)
      val initialState = context(initialIslandStateVal)
      val (population, islandState) = evolution.operations.migrateFromIsland(context(evolution.populationVal).toVector, initialState, state)

      Context(
        evolution.offspringPopulationVal -> population.toArray(using evolution.individualVal.`type`.manifest),
        islandStateVal -> islandState
      )
    .set (
      inputs += (evolution.populationVal, evolution.stateVal, initialIslandStateVal),
      outputs += (evolution.offspringPopulationVal, islandStateVal)
    )

