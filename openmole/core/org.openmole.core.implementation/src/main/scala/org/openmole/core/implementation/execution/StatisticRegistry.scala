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

package org.openmole.core.implementation.execution

import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.collection.Registry
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

object StatisticRegistry extends WeakHashMap[IEnvironment, Statistic] with SynchronizedMap[IEnvironment, Statistic] {
  
  def sample(environment: IEnvironment, job: IJob, sample: StatisticSample) = 
    statistic(environment) += (job.executionId, new StatisticKey(job), sample)

  def statistic(environment: IEnvironment) = 
    getOrElseUpdate(environment, new Statistic(Workspace.preferenceAsInt(Environment.StatisticsHistorySize)))

}
