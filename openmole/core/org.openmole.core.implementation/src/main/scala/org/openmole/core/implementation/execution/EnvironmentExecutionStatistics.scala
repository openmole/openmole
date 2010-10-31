/*
 * Copyright (C) 2010 reuillon
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

import java.util.Arrays
import org.openmole.core.model.execution.IEnvironmentExecutionStatistics
import org.openmole.core.model.execution.IStatistic
import org.openmole.core.model.job.IJob
import org.openmole.core.model.execution.SampleType

import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IGenericTask
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

class EnvironmentExecutionStatistics(historySize: Int) extends IEnvironmentExecutionStatistics {

  val stats = new WeakHashMap[IMoleExecution, HashMap[StatisticKey, IStatistic]] with SynchronizedMap[IMoleExecution, HashMap[StatisticKey, IStatistic]]
   

  override def getStatFor(job: IJob): IStatistic = {
    val map = stats.get(JobRegistry.getInstance().getMoleExecutionForJob(job));
        
    if (map == null) {
      return Statistic.empty
    }

    val ret = map.get(new StatisticKey(job))

    if (ret == null) {
      return Statistic.empty
    }
    return ret
  }

  override def statusJustChanged(sample: SampleType, length: Long, job: IJob) = {
    val statForTask = getOrConstructStatistic(job)
    statForTask.add(sample, length)
  }

  private def getOrConstructStatistic(job: IJob): IStatistic = {
    synchronized {
      val map = getOrConstructStatisticMap(JobRegistry.getInstance().getMoleExecutionForJob(job));

      val key = new StatisticKey(job)
        
      map.get(key) match {
        case Some(m) => m
        case None => val stat = new Statistic(historySize)
          map(key) = stat
          stat
      }
    }
  }

  private def getOrConstructStatisticMap(moleExecution: IMoleExecution): HashMap[StatisticKey, IStatistic] = {
    synchronized {
      stats.get(moleExecution) match {
        case Some(m) => m
        case None => 
          val m = new HashMap[StatisticKey, IStatistic] with SynchronizedMap[StatisticKey, IStatistic]
          stats(moleExecution) = m
          m
      }

    }
  }

}
