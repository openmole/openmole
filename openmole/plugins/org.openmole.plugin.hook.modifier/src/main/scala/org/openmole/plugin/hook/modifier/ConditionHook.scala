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

import monocle.Focus
import org.openmole.core.context._
import org.openmole.core.argument._
import org.openmole.core.dsl._
import org.openmole.core.setter._
import org.openmole.core.workflow.hook.{ Hook, HookExecutionContext }
import org.openmole.core.workflow.mole._

object ConditionHook {

  implicit def isIO: InputOutputBuilder[ConditionHook] = InputOutputBuilder(Focus[ConditionHook](_.config))
  implicit def isInfo: InfoBuilder[ConditionHook] = InfoBuilder(Focus[ConditionHook](_.info))

  def apply(hook: Hook, condition: Condition)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new ConditionHook(
      hook,
      condition,
      config = InputOutputConfig(),
      info = InfoConfig()
    ) set (
      inputs ++= hook.inputs.toSeq,
      outputs ++= hook.outputs.toSeq
    )

}

case class ConditionHook(
  hook:      Hook,
  condition: Condition,
  config:    InputOutputConfig,
  info:      InfoConfig
) extends Hook {

  override protected def process(executionContext: HookExecutionContext) = FromContext { parameters =>
    import parameters._
    if (condition.from(context)) hook.perform(context, executionContext) else context
  }

}