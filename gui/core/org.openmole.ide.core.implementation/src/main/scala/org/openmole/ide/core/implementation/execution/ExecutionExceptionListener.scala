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

import org.openmole.core.model.mole.IMoleExecution._
import java.io.PrintStream
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import TextAreaOutputStream._
import org.openmole.misc.exception.ExceptionUtils
import org.openmole.core.model.mole.IMoleExecution.JobFailed
import org.openmole.core.model.mole.IMoleExecution.ExceptionRaised
import org.openmole.core.model.mole.IMoleExecution.HookExceptionRaised
import org.openmole.core.model.mole.IMoleExecution.SourceExceptionRaised

class ExecutionExceptionListener(exeManager: ExecutionManager) extends EventListener[IMoleExecution] {

  override def triggered(execution: IMoleExecution, event: Event[IMoleExecution]) = synchronized {
    event match {
      case j: JobFailed ⇒
        exeManager.executionJobExceptionTextArea.append("Job failed for capsule " + j.capsule)
        exeManager.executionJobExceptionTextArea.append(ExceptionUtils.prettify(j.exception))
      case e: ExceptionRaised ⇒
        exeManager.executionJobExceptionTextArea.append(e.level + ": Exception managing job " + e.moleJob)
        exeManager.executionJobExceptionTextArea.append(ExceptionUtils.prettify(e.exception))
      case h: HookExceptionRaised ⇒
        exeManager.executionJobExceptionTextArea.append(h.level + ": Exception in misc " + h.hook)
        exeManager.executionJobExceptionTextArea.append(ExceptionUtils.prettify(h.exception))
      case s: SourceExceptionRaised ⇒
        exeManager.executionJobExceptionTextArea.append(s.level + ": Exception in source " + s.source)
        exeManager.executionJobExceptionTextArea.append(ExceptionUtils.prettify(s.exception))
      case s: ProfilerExceptionRaised ⇒
        exeManager.executionJobExceptionTextArea.append(s.level + ": Exception in profiler " + s.profiler)
        exeManager.executionJobExceptionTextArea.append(ExceptionUtils.prettify(s.exception))

    }
  }
}
