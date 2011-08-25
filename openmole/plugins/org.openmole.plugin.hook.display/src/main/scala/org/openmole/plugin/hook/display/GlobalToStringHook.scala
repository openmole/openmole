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
import org.openmole.core.implementation.hook.MoleExecutionHook
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.job.IMoleJob
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.Prettifier._

class GlobalToStringHook(execution: IMoleExecution, out: PrintStream, prototypes: Map[ICapsule, Iterable[IPrototype[_]]]) extends MoleExecutionHook(execution) {
  def this(execution: IMoleExecution, prototypes: Map[ICapsule, Iterable[IPrototype[_]]]) = this(execution, System.out, prototypes)
  
  override def jobFinished(moleJob: IMoleJob, capsule: ICapsule) =
    prototypes.get(capsule) match {
      case Some(ps) => ps.foreach (p => moleJob.context.value(p) match {
            case Some(v) => out.println(p.name + " = " + v.prettify) 
            case None => throw new UserBadDataError("No variable " + p + " found at the end of capsule "+ capsule + ".")
          })
      case None =>
    }
  
}
