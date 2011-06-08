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

import org.openmole.core.implementation.hook.CapsuleExecutionHook
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.tools.service.Logger

class ToStringHook(execution: IMoleExecution, capsule: IGenericCapsule, prototypes: IPrototype[_]*) extends CapsuleExecutionHook(execution, capsule) with Logger {
  
  def this(execution: IMoleExecution, capsule: IGenericCapsule, prototypes: Array[IPrototype[_]]) = this(execution, capsule, prototypes: _*)
  
  override def process(moleJob: IMoleJob) = {
    import moleJob.context
    
    prototypes.map(p => p -> context.variable(p)) foreach {
      case(prototype, option) => option match {
          case Some(v) => println(v.toString)
          case None => logger.warning("No variable " + prototype + " found.")
        }
    }
  }
  
}
