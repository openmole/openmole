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

import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.eventdispatcher._
import org.openmole.core.model.execution.ExecutionState._

class JobCreatedOnEnvironmentListener(exeManager: ExecutionManager,
                                      moleExecution: IMoleExecution, 
                                      environment: IEnvironment) extends EventListener[IEnvironment] {
  override def triggered(environment: IEnvironment, event: Event[IEnvironment]) = {
    event match {
      case x: IEnvironment.JobSubmitted=>
        val env = exeManager.environments(environment)
        exeManager.envBarPlotter.update(READY,env._2(READY).incrementAndGet)
        EventDispatcher.listen(x.job,new JobOnEnvironmentStatusListener(exeManager,moleExecution,x.job),classOf[IExecutionJob.StateChanged])
    }
  }
}