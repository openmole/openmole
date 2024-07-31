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

object ElitismTask:

  def apply[T](evolution: EvolutionWorkflow, evaluated: Val[Long])(using sourcecode.Name, DefinitionScope) =
    Task("ElitismTask") { p ⇒
      import p._

      val (newState, newPopulation) =
        evolution.operations.elitism(
          context(evolution.populationVal).toVector,
          context(evolution.offspringPopulationVal).toVector,
          context(evolution.stateVal),
          context(evaluated),
          random()
        ).from(context)

      Context(
        Variable(evolution.populationVal, newPopulation.toArray(evolution.individualVal.`type`.manifest)),
        Variable(evolution.stateVal, newState)
      )
    } set (
      inputs += (evolution.stateVal, evolution.populationVal, evolution.offspringPopulationVal, evaluated),
      outputs += (evolution.populationVal, evolution.stateVal)
    )

