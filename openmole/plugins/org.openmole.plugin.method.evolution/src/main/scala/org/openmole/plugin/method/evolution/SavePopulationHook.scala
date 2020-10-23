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
import org.openmole.plugin.method.evolution.data.{ EvolutionMetadata, SavedData }

object SavePopulationHook {

  def resultVariables(t: EvolutionWorkflow, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
    import p._
    context.variable(t.generationPrototype).toSeq ++
      t.operations.result(
        context(t.populationPrototype).toVector,
        context(t.statePrototype),
        keepAll = keepAll,
        includeOutputs = includeOutputs).from(context)
  }

  def apply[T, F](
    evolution:      EvolutionWorkflow,
    output:         WritableOutput,
    frequency:      OptionalArgument[Long] = None,
    last:           Boolean                = false,
    keepAll:        Boolean                = false,
    includeOutputs: Boolean                = false,
    format:         F                      = CSVOutputFormat(unrollArray = true))(implicit name: sourcecode.Name, definitionScope: DefinitionScope, outputFormat: OutputFormat[F, EvolutionMetadata]) = Hook("SavePopulationHook") { p ⇒
    import p._

    def fileName =
      (frequency.option, last) match {
        case (_, true) ⇒ Some("population")
        case (None, _) ⇒ Some("population${" + evolution.generationPrototype.name + "}")
        case (Some(f), _) if context(evolution.generationPrototype) % f == 0 ⇒ Some("population${" + evolution.generationPrototype.name + "}")
        case _ ⇒ None
      }

    fileName match {
      case Some(fileName) ⇒
        def savedData = SavedData(
          generation = context(evolution.generationPrototype),
          frequency = frequency,
          name = fileName,
          last = last
        )

        def evolutionData = evolution.operations.metadata(savedData)

        val content = OutputFormat.PlainContent(resultVariables(evolution, keepAll = keepAll, includeOutputs = includeOutputs).from(context), Some(ExpandedString(fileName)))
        outputFormat.write(executionContext)(format, output, content, evolutionData).from(context)
      case None ⇒
    }

    context
  } withValidate { outputFormat.validate(format, _) } set (inputs += (evolution.populationPrototype, evolution.statePrototype))

}

