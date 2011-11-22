/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.hook.display

import java.io.PrintStream
import org.openmole.core.implementation.hook.CapsuleExecutionHook
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.io.Prettifier._

class ToStringHook(execution: IMoleExecution, capsule: ICapsule, out: PrintStream, prototypes: IPrototype[_]*) extends CapsuleExecutionHook(execution, capsule) {

  def this(execution: IMoleExecution, capsule: ICapsule, prototypes: IPrototype[_]*) = this(execution, capsule, System.out, prototypes: _*)
  
  def this(execution: IMoleExecution, capsule: ICapsule, prototypes: Array[IPrototype[_]]) = this(execution, capsule, System.out, prototypes: _*)

  def this(execution: IMoleExecution, capsule: ICapsule, out: PrintStream, prototypes: Array[IPrototype[_]]) = this(execution, capsule, out, prototypes: _*)

  override def process(moleJob: IMoleJob) = {
    import moleJob.context
    
    prototypes.map(p => p -> context.variable(p)) foreach {
      case(prototype, option) => option match {
          case Some(v) => out.println(prototype.name + " = " + (if(v.value == null) "null" else v.value.prettify))
          case None => throw new UserBadDataError("No variable " + prototype + " found.")
        }
    }
  }
  
}
