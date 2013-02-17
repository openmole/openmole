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

import org.openmole.core.model.execution.Environment.ExceptionRaised
import java.awt.Color
import java.io.BufferedOutputStream
import java.io.PrintStream
import org.openmole.core.model.execution.Environment
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import TextAreaOutputStream._
import org.openmole.misc.exception.ExceptionUtils

class EnvironmentExceptionListener(exeManager: ExecutionManager) extends EventListener[Environment] {

  override def triggered(environment: Environment, event: Event[Environment]) = synchronized {
    event match {
      case x: ExceptionRaised ⇒
        exeManager.moleExecutionExceptionTextArea.append(x.level + ": Exception in task " + x.job)
        exeManager.moleExecutionExceptionTextArea.append(ExceptionUtils.prettify(x.exception))
    }
  }
}
