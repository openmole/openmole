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

  def apply[T](evolution: EvolutionWorkflow)(using sourcecode.Name, DefinitionScope) =
    Task("ElitismTask") { p =>
      import p._

      val (newState, newPopulation) =
        evolution.operations.elitism(
          context(evolution.populationVal).toVector,
          context(evolution.offspringPopulationVal).toVector,
          context(evolution.stateVal),
          random()
        ).from(context)

      val incrementedState =
        evolution.operations.generationLens.modify(_ + 1)
        .andThen(evolution.operations.evaluatedLens.modify(_ + 1))
        .apply(newState)

      Context(
        Variable(evolution.populationVal, newPopulation.toArray(evolution.individualVal.`type`.manifest)),
        Variable(evolution.stateVal, incrementedState)
      )
    } set (
      inputs += (evolution.stateVal, evolution.populationVal, evolution.offspringPopulationVal),
      outputs += (evolution.populationVal, evolution.stateVal)
    )

object IslandElitismTask:

  def apply[T](evolution: EvolutionWorkflow, islandStateVal: Val[evolution.S])(using sourcecode.Name, DefinitionScope) =
    Task("IslandElitismTask") { p =>
      import p._

      def state = context(evolution.stateVal)
      def islandState = context(islandStateVal)
      val mergedState = evolution.operations.mergeIslandState(state, islandState)

      val (newState, newPopulation) =
        evolution.operations.elitism(
          context(evolution.populationVal).toVector,
          context(evolution.offspringPopulationVal).toVector,
          mergedState,
          random()
        ).from(context)

      val incrementedState =
        evolution.operations.generationLens.modify(_ + 1)
          .andThen(evolution.operations.evaluatedLens.modify(_ + evolution.operations.evaluatedLens.get(islandState)))
          .apply(newState)

      Context(
        Variable(evolution.populationVal, newPopulation.toArray(evolution.individualVal.`type`.manifest)),
        Variable(evolution.stateVal, incrementedState)
      )
    } set (
      inputs += (evolution.stateVal, islandStateVal, evolution.populationVal, evolution.offspringPopulationVal),
      outputs += (evolution.populationVal, evolution.stateVal)
    )


