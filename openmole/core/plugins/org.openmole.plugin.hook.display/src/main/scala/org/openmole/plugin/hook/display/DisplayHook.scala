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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools.ExpandedString

object DisplayHook {

  def apply(toDisplay: ExpandedString) =
    new HookBuilder {
      def toHook = new DisplayHook(toDisplay) with Built
    }

}

abstract class DisplayHook(toDisplay: ExpandedString) extends Hook {

  override def process(context: Context, executionContext: ExecutionContext) = {
    executionContext.out.println(toDisplay.from(context))
    context
  }

}
