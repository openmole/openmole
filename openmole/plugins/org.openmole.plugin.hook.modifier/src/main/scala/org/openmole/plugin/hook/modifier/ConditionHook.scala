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

import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._

object ConditionHook {

  def apply(hook: Hook, condition: Condition) =
    new HookBuilder {
      addInput(hook.inputs.toSeq: _*)
      addOutput(hook.outputs.toSeq: _*)
      def toHook = new ConditionHook(hook, condition) with Built
    }

}

abstract class ConditionHook(
    val hook: Hook,
    val condition: Condition) extends Hook {

  override def process(context: Context, executionContext: ExecutionContext) =
    if (condition.evaluate(context)) hook.perform(context, executionContext) else context

}