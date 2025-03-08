/*
 * Copyright (C) 24/11/12 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object BreedTask:

  def apply(evolution: EvolutionWorkflow, size: Int, suggestion: Genome.SuggestedValues)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("BreedTask") { p =>
      import p._

      def defaultSetToVariables(ds: Seq[ValueAssignment.Untyped]) = ds.map(v => Variable.unsecureUntyped(v.value, v.equal.from(context))).toVector
      val suggestedGenomes = Genome.SuggestedValues.values(suggestion).map(ds => evolution.operations.buildGenome(defaultSetToVariables(ds)))

      val population = context(evolution.populationVal)
      val s = context(evolution.stateVal)

      (population.isEmpty, evolution.operations.generationLens.get(s), suggestedGenomes.isEmpty) match {
        case (true, 0, false) =>
          val gs =
            size - suggestedGenomes.size match {
              case x if x > 0 => evolution.operations.initialGenomes(x, random())(context)
              case x          => Vector.empty
            }

          Context(
            evolution.genomeVal.array -> random().shuffle(suggestedGenomes ++ gs).toArray(evolution.genomeVal.`type`.manifest)
          )
        case (true, _, _) =>
          val gs = evolution.operations.initialGenomes(size, random())(context)

          Context(
            Variable(evolution.genomeVal.array, gs.toArray(evolution.genomeVal.`type`.manifest))
          )
        case (false, _, _) =>
          val breeded = evolution.operations.breeding(population.toVector, size, s, random()).from(context)

          Context(
            Variable(evolution.genomeVal.array, breeded.toArray(evolution.genomeVal.`type`.manifest))
          )
      }

    } set (
      inputs += (evolution.populationVal, evolution.stateVal),
      outputs += evolution.stateVal,
      exploredOutputs += evolution.genomeVal.array
    ) withValidate (
      Validate:
        Genome.SuggestedValues.errors(suggestion)
    )


