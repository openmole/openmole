/*
 * Copyright (C) 2011 reuillon
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

import java.io.PrintStream
import org.openmole.core.implementation.hook.CapsuleExecutionHook
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution

class DisplayHook(execution: IMoleExecution, capsule: ICapsule, toDisplay: String, out: PrintStream) extends CapsuleExecutionHook(execution, capsule) {
  
  def this(execution: IMoleExecution, capsule: ICapsule, toDisplay: String) = this(execution, capsule, toDisplay, System.out)
  
  override def process(moleJob: IMoleJob) = out.println(VariableExpansion.expandData(moleJob.context, toDisplay))
}
