/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.control

import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.eventdispatcher._
import ExecutionManager._

class JobOnEnvironmentStatusListener(exeManager: ExecutionManager,
                                     moleExecution: IMoleExecution, 
                                     executionJob: IExecutionJob) extends EventListener[IExecutionJob] {
  override def triggered(executionJob: IExecutionJob, event: Event[IExecutionJob]) = {
    event match {
      case x: IExecutionJob.StateChanged=> 
        val env = exeManager.environments(executionJob.environment)
        env._2(x.oldState).decrementAndGet
        env._2(x.newState).incrementAndGet
        exeManager.envBarPlotter.update(env._2.states)
       // exeManager.envBarPlotter.update(x.oldState,env._2(x.oldState).decrementAndGet)
      //  exeManager.envBarPlotter.update(x.newState,env._2(x.newState).incrementAndGet)
    }
  }
}
