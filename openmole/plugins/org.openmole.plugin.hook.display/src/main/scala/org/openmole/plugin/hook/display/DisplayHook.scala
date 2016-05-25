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
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workflow.validation.ValidateHook

object DisplayHook {

  implicit def isBuilder = new HookBuilder[DisplayHook] {
    override def name = DisplayHook.name
    override def outputs = DisplayHook.outputs
    override def inputs = DisplayHook.inputs
    override def defaults = DisplayHook.defaults
  }

  def apply(toDisplay: ExpandedString) =
    new DisplayHook(
      toDisplay,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None
    )
}

@Lenses case class DisplayHook(
    toDisplay: ExpandedString,
    inputs:    PrototypeSet,
    outputs:   PrototypeSet,
    defaults:  DefaultSet,
    name:      Option[String]
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]): Seq[Throwable] = toDisplay.validate(inputs)

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    executionContext.out.println(toDisplay.from(context))
    context
  }

}
