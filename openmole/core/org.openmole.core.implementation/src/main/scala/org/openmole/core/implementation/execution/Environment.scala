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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.execution

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.job.IJob
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.core.model.execution.SampleType

object Environment {
  val StatisticsHistorySize = new ConfigurationLocation("Environment", "StatisticsHistorySize")

  Activator.getWorkspace += (StatisticsHistorySize, "1000")
}


abstract class Environment[EXECUTIONJOB <: IExecutionJob] extends IEnvironment {
   
  val statistic = new Statistic(Activator.getWorkspace.preferenceAsInt(Environment.StatisticsHistorySize))
  val jobRegistry = new ExecutionJobRegistry[EXECUTIONJOB]
  val id = UUID.randomUUID.toString
  val executionJobId = new AtomicLong

  def sample(sample: SampleType, value: Long, job: IJob) = {
    JobRegistry(job) match {
      case None =>
      case Some(moleExecution) =>
        statistic += (moleExecution, new StatisticKey(job), sample, value)
    }

  }

  def nextExecutionJobId = new ExecutionJobId(id, executionJobId.getAndIncrement)
  
}
