/*
 * Copyright (C) 2011 Leclaire Mathieu  <mathieu.Mathieu Leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.hook.modifier

import org.openmole.core.workflow.tools.Condition
import org.openmole.core.workflow.mole._
import org.openmole.core.dsl
import dsl._
import monocle.macros.Lenses
import org.openmole.core.workflow.data._

object ConditionHook {

  implicit def isBuilder = new HookBuilder[ConditionHook] {
    override def name = ConditionHook.name
    override def outputs = ConditionHook.outputs
    override def inputs = ConditionHook.inputs
    override def defaults = ConditionHook.defaults
  }

  def apply(hook: Hook, condition: Condition) =
    new ConditionHook(
      hook,
      condition,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None
    ) set (
      dsl.inputs += (hook.inputs.toSeq: _*),
      dsl.outputs += (hook.outputs.toSeq: _*)
    )

}

@Lenses case class ConditionHook(
    hook:      Hook,
    condition: Condition,
    inputs:    PrototypeSet,
    outputs:   PrototypeSet,
    defaults:  DefaultSet,
    name:      Option[String]
) extends Hook {

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) =
    if (condition.from(context)) hook.perform(context, executionContext) else context

}