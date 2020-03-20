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

  def hook[F](t: EvolutionWorkflow, output: WritableOutput, frequency: OptionalArgument[Long], format: F)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F]) = {
    Hook("SavePopulationHook") { p ⇒
      import p._

      def save =
        frequency.option match {
          case None ⇒ true
          case Some(f) ⇒
            val generation = context(t.generationPrototype)
            (generation % f) == 0
        }

      if (save) {
        output match {
          case WritableOutput.FileValue(dir) ⇒
            val outputFile = dir / ExpandedString("population${" + t.generationPrototype.name + "}" + outputFormat.extension)
            outputFormat.write(format, outputFile, resultVariables(t).from(context)).from(context)
          case o ⇒ outputFormat.write(format, o, resultVariables(t).from(context)).from(context)
        }
      }

      context
    } validate { p ⇒ outputFormat.validate(format)(p) } set (inputs += (t.populationPrototype, t.statePrototype))

  }

  def apply[T, F: OutputFormat](algorithm: T, output: WritableOutput, frequency: OptionalArgument[Long] = None, format: F = CSVOutputFormat(overwrite = true))(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    val t = wfi(algorithm)
    hook(t, output, frequency, format)
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

      file.from(context).withPrintStream(create = true) { ps ⇒
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

