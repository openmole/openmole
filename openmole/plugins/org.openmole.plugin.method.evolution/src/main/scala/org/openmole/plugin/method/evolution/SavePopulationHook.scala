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

  def hook[F](t: EvolutionWorkflow, output: WritableOutput, frequency: Option[Long], last: Boolean, format: F)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F]) = {
    Hook("SavePopulationHook") { p ⇒
      import p._

      def saveFile(dir: FromContext[File]) =
        (frequency, last) match {
          case (_, true) ⇒ Some(dir / ExpandedString("population" + outputFormat.extension))
          case (None, _) ⇒ Some(dir / ExpandedString("population${" + t.generationPrototype.name + "}" + outputFormat.extension))
          case (Some(f), _) if context(t.generationPrototype) % f == 0 ⇒
            Some(dir / ExpandedString("population${" + t.generationPrototype.name + "}" + outputFormat.extension))
          case _ ⇒ None
        }

      output match {
        case WritableOutput.FileValue(dir) ⇒
          saveFile(dir) match {
            case Some(outputFile) ⇒ outputFormat.write(format, outputFile.from(context), resultVariables(t).from(context)).from(context)
            case None             ⇒
          }
        case o ⇒
          val save =
            (frequency, last) match {
              case (_, true)    ⇒ true
              case (Some(f), _) ⇒ context(t.generationPrototype) % f == 0
              case _            ⇒ false
            }

          if (save) outputFormat.write(format, o, resultVariables(t).from(context)).from(context)
      }

      context
    } validate { p ⇒ outputFormat.validate(format)(p) } set (inputs += (t.populationPrototype, t.statePrototype))

  }

  def apply[T, F: OutputFormat](algorithm: T, output: WritableOutput, frequency: OptionalArgument[Long] = None, last: Boolean = false, format: F = CSVOutputFormat())(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    val t = wfi(algorithm)
    hook(t, output, frequency.option, last = last, format = format)
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

