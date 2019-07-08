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
import org.openmole.core.workflow.builder.ValueAssignment

object BreedTask {

  def apply(evolution: EvolutionWorkflow, size: Int, suggestion: Seq[Seq[ValueAssignment[_]]] = Seq.empty)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    Task("BreedTask") { p ⇒
      import p._

      def defaultSetToVariables(ds: Seq[ValueAssignment[_]]) = ds.map(v ⇒ Variable.unsecure(v.value, v.equal.from(context))).toVector
      val suggestedGenomes = suggestion.map(ds ⇒ evolution.operations.buildGenome(defaultSetToVariables(ds)).from(context))

      val population = context(evolution.populationPrototype)
      val s = context(evolution.statePrototype)

      (population.isEmpty, evolution.operations.generationLens.get(s), suggestedGenomes.isEmpty) match {
        case (true, 0, false) ⇒
          val (news, gs) =
            size - suggestedGenomes.size match {
              case x if x > 0 ⇒ evolution.operations.initialGenomes(x)(context).run(s).value
              case x          ⇒ (s, Vector.empty)
            }

          Context(
            evolution.genomePrototype.array -> random().shuffle(suggestedGenomes ++ gs).toArray(evolution.genomePrototype.`type`.manifest),
            Variable(evolution.statePrototype, news)
          )
        case (true, _, _) ⇒
          val (news, gs) = evolution.operations.initialGenomes(size)(context).run(s).value

          Context(
            Variable(evolution.genomePrototype.array, gs.toArray(evolution.genomePrototype.`type`.manifest)),
            Variable(evolution.statePrototype, news)
          )
        case (false, _, _) ⇒
          val (newState, breeded) = evolution.operations.breeding(population.toVector, size).from(context).run(s).value

          Context(
            Variable(evolution.genomePrototype.array, breeded.toArray(evolution.genomePrototype.`type`.manifest)),
            Variable(evolution.statePrototype, newState)
          )
      }

    } set (
      inputs += (evolution.populationPrototype, evolution.statePrototype),
      outputs += (evolution.statePrototype),
      exploredOutputs += (evolution.genomePrototype.array)
    )

}

