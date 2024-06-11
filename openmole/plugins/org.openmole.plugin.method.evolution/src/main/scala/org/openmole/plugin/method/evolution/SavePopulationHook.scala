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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

object SavePopulationHook:

  def resultVariables(t: EvolutionWorkflow, keepAll: Boolean, includeOutputs: Boolean, filter: Seq[String]) =
    FromContext: p ⇒
      import p._
      val state = context(t.stateVal)

      def all =
        Seq[Variable[_]](
          t.generationVal -> t.operations.generationLens.get(state),
          t.evaluatedVal -> t.operations.evaluatedLens.get(state)
        ) ++
          t.operations.result(
            context(t.populationVal).toVector,
            state,
            keepAll = keepAll,
            includeOutputs = includeOutputs).from(context)

      val filterSet = filter.toSet
      all.filter(v ⇒ !filterSet.contains(v.name))


  def apply[F](
    evolution:      EvolutionWorkflow,
    output:         WritableOutput,
    frequency:      OptionalArgument[Long] = None,
    last:           Boolean                = false,
    keepAll:        Boolean                = false,
    includeOutputs: Boolean                = true,
    filter:         Seq[Val[_]]            = Vector.empty)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, scriptSourceData: ScriptSourceData) = Hook("SavePopulationHook") { p ⇒
    import p._

    val state = context(evolution.stateVal)
    val generation = evolution.operations.generationLens.get(state)
    val shouldBeSaved = frequency.map(generation % _ == 0).getOrElse(true) || context(evolution.terminatedVal)

    if shouldBeSaved
    then
      def saveOption = SaveOption(frequency = frequency, last = last)
      def evolutionData = evolution.operations.metadata(state, saveOption)

      val augmentedContext =
        context + (evolution.generationVal -> generation)

      val content =
        OutputContent(
          SectionContent(
            Some("population"),
            resultVariables(evolution, keepAll = keepAll, includeOutputs = includeOutputs, filter = filter.map(_.name)).from(augmentedContext),
            Seq(evolution.generationVal.name, evolution.evaluatedVal.name)
          )
        )

      OMROutputFormat.write(executionContext, output, content, evolutionData).from(augmentedContext)

    context
  } set (inputs += (evolution.populationVal, evolution.stateVal))



