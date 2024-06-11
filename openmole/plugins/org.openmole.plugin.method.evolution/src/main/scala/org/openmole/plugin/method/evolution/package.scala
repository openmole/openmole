/*
 * Copyright (C) 22/11/12 Romain Reuillon
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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.setter.*
import monocle.macros.*
import org.openmole.plugin.task.tools.*
import org.openmole.plugin.tool.pattern
import org.openmole.plugin.tool.pattern.MasterSlave
import squants.time.Time

type Objectives = Seq[Objective] | Objective
type Genome = Seq[Genome.GenomeBound]
type GenomeDouble = Seq[Genome.GenomeBound.ScalarDouble]

implicit def intToCounterTerminationConverter(n: Long): EvolutionWorkflow.AfterEvaluated = EvolutionWorkflow.AfterEvaluated(n)
implicit def durationToDurationTerminationConverter(d: Time): EvolutionWorkflow.AfterDuration = EvolutionWorkflow.AfterDuration(d)
implicit def byEvolutionPattern[T](implicit patternContainer: ExplorationMethodSetter[T, EvolutionWorkflow.EvolutionPattern], method: ExplorationMethod[T, EvolutionWorkflow]): ExplorationMethod[By[T, EvolutionWorkflow.EvolutionPattern], EvolutionWorkflow] = v ⇒ method(patternContainer(v.value, v.by))

implicit class EvolutionHookDecorator[T](t: T)(implicit method: ExplorationMethod[T, EvolutionWorkflow]):
  val decorator = new MethodHookDecorator(t)(using method)
  export decorator.*

  def hook[F](
    output:         WritableOutput,
    frequency:      OptionalArgument[Long] = None,
    last:           Boolean                = false,
    keepAll:        Boolean                = false,
    includeOutputs: Boolean                = true,
    filter:         Seq[Val[_]]            = Vector.empty)(implicit outputFormat: OutputFormat[F, EvolutionMetadata]): Hooked[T] =
    val m = method(t)
    implicit def scope: DefinitionScope = m.scope

    Hooked(
      t,
      SavePopulationHook(
        evolution = m.method,
        output = output,
        frequency = frequency,
        last = last,
        keepAll = keepAll,
        includeOutputs = includeOutputs,
        filter = filter)
    )


export EvolutionWorkflow.Island
export Genome.Suggestion
