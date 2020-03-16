/*
 * Copyright (C) 2014 Romain Reuillon
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
 */

package org.openmole.plugin.method.evolution

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object SavePopulationHook {

  def resultVariables(t: EvolutionWorkflow) = FromContext { p ⇒
    import p._
    context.variable(t.generationPrototype).toSeq ++
      t.operations.result(context(t.populationPrototype).toVector, context(t.statePrototype)).from(context)
  }

  def hook(t: EvolutionWorkflow, output: WritableOutput, frequency: OptionalArgument[Long])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = {
    Hook("SavePopulationHook") { p ⇒
      import p._
      import org.openmole.core.csv

      def save =
        frequency.option match {
          case None ⇒ true
          case Some(f) ⇒
            val generation = context(t.generationPrototype)
            (generation % f) == 0
        }

      if (save) {
        val values = resultVariables(t).from(context).map(_.value)
        def headerLine = csv.header(resultVariables(t).from(context).map(_.prototype.array), values)

        output match {
          case WritableOutput.FileValue(dir) ⇒
            (dir / ExpandedString("population${" + t.generationPrototype.name + "}.csv")).from(context).withPrintStream(overwrite = false, create = true) { ps ⇒
              csv.writeVariablesToCSV(
                ps,
                Some(headerLine),
                values
              )
            }
          case WritableOutput.PrintStreamValue(ps) ⇒
            csv.writeVariablesToCSV(
              ps,
              Some(headerLine),
              values
            )
        }

      }

      context
    } set (inputs += (t.populationPrototype, t.statePrototype))

  }

  def apply[T](algorithm: T, output: WritableOutput, frequency: OptionalArgument[Long] = None)(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    val t = wfi(algorithm)
    hook(t, output, frequency)
  }

}

object SaveLastPopulationHook {

  def apply[T](algorithm: T, file: FromContext[File])(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    val t = wfi(algorithm)

    Hook("SaveLastPopulationHook") { p ⇒
      import p._
      import org.openmole.core.csv

      val values = SavePopulationHook.resultVariables(t).from(context).map(_.value)
      def headerLine = csv.header(SavePopulationHook.resultVariables(t).from(context).map(_.prototype.array), values)

      file.from(context).withPrintStream(overwrite = true, create = true) { ps ⇒
        csv.writeVariablesToCSV(
          ps,
          Some(headerLine),
          values
        )
      }

      context
    } set (inputs += (t.populationPrototype, t.statePrototype))

  }

}

