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

class JobOnEnvironmentStatusListener(moleExecution: IMoleExecution, executionJob: IExecutionJob) extends EventListener[IExecutionJob] {
  override def triggered(executionJob: IExecutionJob, event: Event[IExecutionJob]) = {
    event match {
      case x: IExecutionJob.StateChanged=> 
        println("state changed from  " + x.oldState + " to " + x.newState)
        val exeManager = TopComponentsManager.executionManager(moleExecution)
        val env = exeManager.environments(executionJob.environment)
        env._2(x.oldState) -= 1 
        env._2(x.newState) += 1
        env._1.updateData(x.oldState.name,env._2(x.oldState))
        env._1.updateData(x.newState.name,env._2(x.newState))
    }
  }
}
