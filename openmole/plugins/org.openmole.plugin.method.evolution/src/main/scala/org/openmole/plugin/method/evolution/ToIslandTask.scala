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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

object ToIslandTask:

  def apply[T](evolution: EvolutionWorkflow, islandPopulation: Val[evolution.Pop], initialIslandStateVal: Val[evolution.S])(using sourcecode.Name, DefinitionScope) =
    Task("ToIslandTask") { p =>
      import p._
      val (population, state) = evolution.operations.migrateToIsland(context(islandPopulation).toVector, context(evolution.stateVal))
      Context(
        evolution.populationVal -> population.toArray,
        evolution.stateVal -> state,
        initialIslandStateVal -> state
      )

    } set (
      inputs += (islandPopulation, evolution.stateVal),
      outputs += (evolution.populationVal, evolution.stateVal, initialIslandStateVal)
    )

