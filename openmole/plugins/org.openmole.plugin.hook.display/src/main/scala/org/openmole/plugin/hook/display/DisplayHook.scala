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

import monocle.macros.Lenses
import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._

object DisplayHook {

  implicit def isIO: InputOutputBuilder[DisplayHook] = InputOutputBuilder(config)

  def apply(toDisplay: FromContext[String])(implicit name: sourcecode.Name) =
    new DisplayHook(
      toDisplay,
      config = InputOutputConfig()
    )
}

@Lenses case class DisplayHook(
  toDisplay: FromContext[String],
  config:    InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    toDisplay.validate(inputs)
  }

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters ⇒
    import parameters._
    executionContext.out.println(toDisplay.from(context))
    context
  }

}
