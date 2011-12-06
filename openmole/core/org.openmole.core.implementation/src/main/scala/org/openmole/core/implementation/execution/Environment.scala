/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.execution

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.job.IJob
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace

object Environment {
  val StatisticsHistorySize = new ConfigurationLocation("Environment", "StatisticsHistorySize")
  Workspace += (StatisticsHistorySize, "1000")
}


abstract class Environment extends IEnvironment {
 
  class JobSubmissionListner extends EventListener[IEnvironment] {
    override def triggered(job: IEnvironment, ev: Event[IEnvironment]) = {
      ev match {
        case ev: IEnvironment.JobSubmitted => 
          EventDispatcher.listen(ev.job, new JobExceptionListner, classOf[IExecutionJob.ExceptionRaised])
      }
    } 
  }
  
  class JobExceptionListner extends EventListener[IExecutionJob] {
    override def triggered(job: IExecutionJob, ev: Event[IExecutionJob]) {
      ev match  {
        case ev: IExecutionJob.ExceptionRaised => 
          EventDispatcher.trigger(Environment.this, new IEnvironment.ExceptionRaised(job, ev.exception, ev.level))
      }
    }
  } 
  
  EventDispatcher.listen(this.asInstanceOf[IEnvironment], new JobSubmissionListner, classOf[IEnvironment.JobSubmitted])
   
  val id = UUID.randomUUID.toString
  val executionJobId = new AtomicLong

  def nextExecutionJobId = new ExecutionJobId(id, executionJobId.getAndIncrement)
  
}
