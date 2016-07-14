/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.hook.display

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.validation.ValidateHook

object DisplayHook {

  implicit def isIO: InputOutputBuilder[DisplayHook] = InputOutputBuilder(config)

  def apply(toDisplay: FromContext[String]) =
    new DisplayHook(
      toDisplay,
      config = InputOutputConfig()
    )
}

@Lenses case class DisplayHook(
    toDisplay: FromContext[String],
    config:    InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]): Seq[Throwable] = toDisplay.validate(inputs)

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    executionContext.out.println(toDisplay.from(context))
    context
  }

}
