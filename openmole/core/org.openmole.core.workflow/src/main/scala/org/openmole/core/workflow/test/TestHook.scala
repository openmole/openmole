/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.core.workflow.test

import org.openmole.core.context.Context
import org.openmole.core.argument.FromContext
import org.openmole.core.setter._
import org.openmole.core.workflow.hook.{ Hook, HookExecutionContext }
import org.openmole.core.workflow.test.Stubs._
import monocle.Focus

object TestHook:
  implicit def isBuilder: InputOutputBuilder[TestHook] = InputOutputBuilder(Focus[TestHook](_.config))

case class TestHook(
  f:      Context => Unit    = identity[Context],
  config: InputOutputConfig = InputOutputConfig(),
  info:   InfoConfig        = InfoConfig()
) extends Hook {
  override protected def process(executionContext: HookExecutionContext) = FromContext { p => f(p.context); p.context }
}
