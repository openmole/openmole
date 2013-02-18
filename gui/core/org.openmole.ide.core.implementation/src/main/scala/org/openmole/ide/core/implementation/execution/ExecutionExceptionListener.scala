/*
 * Copyright (C) 2011 mathieu
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

package org.openmole.ide.core.implementation.execution

import org.openmole.core.model.mole.IMoleExecution.HookExceptionRaised
import org.openmole.core.model.mole.IMoleExecution.ExceptionRaised
import java.io.PrintStream
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import TextAreaOutputStream._
import org.openmole.misc.exception.ExceptionUtils

class ExecutionExceptionListener(exeManager: ExecutionManager) extends EventListener[IMoleExecution] {

  override def triggered(execution: IMoleExecution, event: Event[IMoleExecution]) = synchronized {
    event match {
      case e: ExceptionRaised ⇒
        exeManager.executionJobExceptionTextArea.append(e.level + ": Exception in task " + e.moleJob)
        exeManager.executionJobExceptionTextArea.append(ExceptionUtils.prettify(e.exception))
      case h: HookExceptionRaised ⇒
        exeManager.executionJobExceptionTextArea.append(h.level + ": Exception in hook " + h.hook)
        exeManager.executionJobExceptionTextArea.append(ExceptionUtils.prettify(h.exception))
    }
  }
}
