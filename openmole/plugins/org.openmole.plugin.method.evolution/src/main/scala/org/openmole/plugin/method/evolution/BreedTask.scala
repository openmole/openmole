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

import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.workflow.builder.{ ValueAssignment, DefinitionScope }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.DefaultSet

object BreedTask {

  def apply[T: WorkflowIntegration](algorithm: T, size: Int, suggestion: Seq[Seq[ValueAssignment[_]]] = Seq.empty)(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    lazy val t = wfi(algorithm)

    FromContextTask("BreedTask") { p ⇒
      import p._

      def defaultSetToVariables(ds: Seq[ValueAssignment[_]]) = ds.map(v ⇒ Variable.unsecure(v.value, v.equal.from(context))).toVector
      val suggestedGenomes = suggestion.map(ds ⇒ t.operations.buildGenome(defaultSetToVariables(ds)).from(context))

      val population = context(t.populationPrototype)
      val s = context(t.statePrototype)

      (population.isEmpty, t.operations.generation(s), suggestedGenomes.isEmpty) match {
        case (true, 0, false) ⇒
          val (news, gs) =
            size - suggestedGenomes.size match {
              case x if x > 0 ⇒ t.operations.initialGenomes(x)(context).run(s).value
              case x          ⇒ (s, Vector.empty)
            }

          Context(
            Variable(t.genomePrototype.array, random().shuffle(suggestedGenomes ++ gs).toArray(t.genomePrototype.`type`.manifest)),
            Variable(t.statePrototype, news)
          )
        case (true, _, _) ⇒
          val (news, gs) = t.operations.initialGenomes(size)(context).run(s).value

          Context(
            Variable(t.genomePrototype.array, gs.toArray(t.genomePrototype.`type`.manifest)),
            Variable(t.statePrototype, news)
          )
        case (false, _, _) ⇒
          val (newState, breeded) = t.operations.breeding(population.toVector, size).from(context).run(s).value

          Context(
            Variable(t.genomePrototype.array, breeded.toArray(t.genomePrototype.`type`.manifest)),
            Variable(t.statePrototype, newState)
          )
      }

    } set (
      inputs += (t.populationPrototype, t.statePrototype),
      outputs += (t.statePrototype),
      exploredOutputs += (t.genomePrototype.array)
    )
  }

}

