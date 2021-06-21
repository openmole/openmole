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

package org.openmole.plugin.method

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.builder._
import monocle.macros._
import org.openmole.core.workflow.format.WritableOutput
import org.openmole.plugin.hook.omr.MethodData
import org.openmole.plugin.method.evolution.data.EvolutionMetadata
import org.openmole.plugin.task.tools._
import org.openmole.plugin.tool.pattern
import org.openmole.plugin.tool.pattern.MasterSlave
import squants.time.Time

package object evolution {

  type Objectives = Seq[Objective]
  type Genome = Seq[Genome.GenomeBound]

  implicit def intToCounterTerminationConverter(n: Long) = EvolutionWorkflow.AfterEvaluated(n)
  implicit def durationToDurationTerminationConverter(d: Time) = EvolutionWorkflow.AfterDuration(d)

  implicit def byEvolutionPattern[T](implicit patternContainer: EvolutionWorkflow.EvolutionPatternContainer[T], method: ExplorationMethod[T, EvolutionWorkflow]): ExplorationMethod[By[T, EvolutionWorkflow.EvolutionPattern], EvolutionWorkflow] = p ⇒ method(patternContainer().set(p.by)(p.value))
  implicit def isEvolutionHookable[T](implicit hookContainer: EvolutionWorkflow.HookContainer[T]): SavePopulationHook.Hookable[T] = (t, h) ⇒ hookContainer().modify(_ ++ Seq(h))(t)
  implicit class SavePopulationHookDecorator[T](p: T)(implicit hookable: SavePopulationHook.Hookable[T]) extends SavePopulationHook.HookFunction[T](p)

  def Island = EvolutionWorkflow.Island

}
