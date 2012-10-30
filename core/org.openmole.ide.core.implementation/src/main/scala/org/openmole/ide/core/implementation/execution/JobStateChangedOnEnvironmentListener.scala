/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

import org.openmole.core.model.execution.Environment
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.eventdispatcher._
import org.openmole.core.model.execution.ExecutionState._
import ExecutionManager._

class JobStateChangedOnEnvironmentListener(exeManager: ExecutionManager,
                                           moleExecution: IMoleExecution,
                                           environment: Environment) extends EventListener[Environment] {
  override def triggered(environment: Environment, event: Event[Environment]) = {
    event match {
      case x: Environment.JobStateChanged ⇒
        exeManager.decrementEnvironmentState(environment, x.oldState)
        exeManager.incrementEnvironmentState(environment, x.newState)
    }
  }
}
