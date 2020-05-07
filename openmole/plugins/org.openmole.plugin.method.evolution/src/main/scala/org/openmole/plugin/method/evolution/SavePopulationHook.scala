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
import org.openmole.core.workflow.format.WritableOutput

object SavePopulationHook {

  case class EvolutionData()

  def evolutionData(t: EvolutionWorkflow) = EvolutionData()

  def resultVariables(t: EvolutionWorkflow) = FromContext { p ⇒
    import p._
    context.variable(t.generationPrototype).toSeq ++
      t.operations.result(context(t.populationPrototype).toVector, context(t.statePrototype)).from(context)
  }

  def hook[F](t: EvolutionWorkflow, output: WritableOutput, frequency: Option[Long], last: Boolean, format: F)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F, EvolutionData]) = {
    Hook("SavePopulationHook") { p ⇒
      import p._

      def fileName =
        (frequency, last) match {
          case (_, true) ⇒ Some(ExpandedString("population"))
          case (None, _) ⇒ Some(ExpandedString("population${" + t.generationPrototype.name + "}"))
          case (Some(f), _) if context(t.generationPrototype) % f == 0 ⇒ Some(ExpandedString("population${" + t.generationPrototype.name + "}"))
          case _ ⇒ None
        }

      val section = OutputFormat.PlainContent(resultVariables(t).from(context), fileName)
      outputFormat.write(format, output, section, evolutionData(t)).from(context)

      context
    } validate { p ⇒ outputFormat.validate(format)(p) } set (inputs += (t.populationPrototype, t.statePrototype))

  }

  def apply[T, F](algorithm: T, output: WritableOutput, frequency: OptionalArgument[Long] = None, last: Boolean = false, format: F = CSVOutputFormat(unrollArray = true))(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F, EvolutionData]) = {
    val t = wfi(algorithm)
    hook(t, output, frequency.option, last = last, format = format)
  }

}

object SaveLastPopulationHook {

  def apply[T, F](algorithm: T, output: WritableOutput, format: F = CSVOutputFormat(unrollArray = true))(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F, SavePopulationHook.EvolutionData]) = {
    val t = wfi(algorithm)

    Hook("SaveLastPopulationHook") { p ⇒
      import p._
      import org.openmole.core.csv

      outputFormat.write(format, output, SavePopulationHook.resultVariables(t).from(context), SavePopulationHook.evolutionData(t)).from(context)
      context
    } set (inputs += (t.populationPrototype, t.statePrototype))

  }

}

