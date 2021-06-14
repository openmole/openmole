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
import org.openmole.plugin.method.evolution.data.{ EvolutionMetadata, SaveOption }

object SavePopulationHook {

  case class Parameters[F](
    output:         WritableOutput,
    frequency:      OptionalArgument[Long],
    last:           Boolean,
    keepAll:        Boolean,
    includeOutputs: Boolean,
    filter:         Seq[Val[_]],
    format:         F,
    outputFormat:   OutputFormat[F, EvolutionMetadata]) {
    def apply(method: EvolutionWorkflow, scope: DefinitionScope) = {
      implicit def of = outputFormat
      implicit def sc = scope
      SavePopulationHook(method, output, frequency = frequency, last = last, keepAll = keepAll, includeOutputs = includeOutputs, filter = filter, format = format)
    }
  }

  trait Hookable[T] {
    def apply(t: T, p: Parameters[_]): T
  }

  class HookFunction[H](h: H)(implicit hookable: Hookable[H]) {
    def hook[F](
      output:         WritableOutput,
      frequency:      OptionalArgument[Long] = None,
      last:           Boolean                = false,
      keepAll:        Boolean                = false,
      includeOutputs: Boolean                = true,
      filter:         Seq[Val[_]]            = Vector.empty,
      format:         F                      = CSVOutputFormat(unrollArray = true))(implicit outputFormat: OutputFormat[F, EvolutionMetadata]) = {
      hookable(
        h,
        SavePopulationHook.Parameters(
          output = output,
          frequency = frequency,
          last = last,
          keepAll = keepAll,
          includeOutputs = includeOutputs,
          filter = filter,
          format = format,
          outputFormat = outputFormat
        )
      )
    }
  }

  def resultVariables(t: EvolutionWorkflow, keepAll: Boolean, includeOutputs: Boolean, filter: Seq[String]) = FromContext { p ⇒
    import p._
    def all =
      context.variable(t.generationPrototype).toSeq ++
        t.operations.result(
          context(t.populationPrototype).toVector,
          context(t.statePrototype),
          keepAll = keepAll,
          includeOutputs = includeOutputs).from(context)

    val filterSet = filter.toSet
    all.filter(v ⇒ !filterSet.contains(v.name))
  }

  def apply[T, F](
    evolution:      EvolutionWorkflow,
    output:         WritableOutput,
    frequency:      OptionalArgument[Long] = None,
    last:           Boolean                = false,
    keepAll:        Boolean                = false,
    includeOutputs: Boolean                = true,
    filter:         Seq[Val[_]]            = Vector.empty,
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
        def saveOption = SaveOption(frequency = frequency, last = last)
        def evolutionData = evolution.operations.metadata(context(evolution.generationPrototype), saveOption)

        val content = OutputFormat.PlainContent(resultVariables(evolution, keepAll = keepAll, includeOutputs = includeOutputs, filter = filter.map(_.name)).from(context), Some(fileName))
        outputFormat.write(executionContext)(format, output, content, evolutionData).from(context)
      case None ⇒
    }

    context
  } withValidate { outputFormat.validate(format) } set (inputs += (evolution.populationPrototype, evolution.statePrototype))

}

