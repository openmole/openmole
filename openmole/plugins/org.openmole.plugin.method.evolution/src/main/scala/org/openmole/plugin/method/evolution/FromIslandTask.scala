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

  def apply[T](evolution: EvolutionWorkflow, islandEvaluated: Val[Long])(using sourcecode.Name, DefinitionScope) =
    Task("FromIslandTask") { p ⇒
      import p._
      val state = context(evolution.stateVal)
      val population = evolution.operations.migrateFromIsland(context(evolution.populationVal).toVector, state, context(evolution.generationVal))
      val evaluated = evolution.operations.evaluatedLens.get(state)

      Context(
        evolution.offspringPopulationVal -> population.toArray(evolution.individualVal.`type`.manifest),
        islandEvaluated -> evaluated
      )
    } set (
      inputs += (evolution.populationVal, evolution.stateVal, evolution.generationVal),
      outputs += (evolution.offspringPopulationVal, islandEvaluated)
    )

