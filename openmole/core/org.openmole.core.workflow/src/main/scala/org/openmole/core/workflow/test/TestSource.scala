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
import org.openmole.core.setter.{ InfoConfig, InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.mole.{ MoleExecutionContext, Source }
import org.openmole.core.workflow.test.Stubs._
import monocle.Focus

object TestSource {
  implicit def isBuilder: InputOutputBuilder[TestSource] = InputOutputBuilder(Focus[TestSource](_.config))
}

case class TestSource(
  f:      Context => Context = identity[Context],
  config: InputOutputConfig = InputOutputConfig(),
  info:   InfoConfig        = InfoConfig()
) extends Source {
  override protected def process(executionContext: MoleExecutionContext) = FromContext { p => f(p.context) }
}