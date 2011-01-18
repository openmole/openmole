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

import java.util.Arrays
import java.util.logging.Logger
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.model.execution.IStatistic
import org.openmole.core.model.job.IJob
import org.openmole.core.model.execution.IStatisticKey
import org.openmole.core.model.execution.IStatisticSamples
import org.openmole.core.model.execution.SampleType

import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.IGenericTask
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

class Statistic(historySize: Int) extends IStatistic {

  val stats = new WeakHashMap[IMoleExecution, HashMap[IStatisticKey, IStatisticSamples]] with SynchronizedMap[IMoleExecution, HashMap[IStatisticKey, IStatisticSamples]]

  override def apply(moleExecution: IMoleExecution, key: IStatisticKey): IStatisticSamples = {
    stats.get(moleExecution) match {
      case None => StatisticSamples.empty
      case Some(map) => map.get(key) match {
          case None => StatisticSamples.empty
          case Some(stat) => stat
      }
    }
  }

  override def += (moleExecution: IMoleExecution, key: IStatisticKey, sample: SampleType.Value, length: Long) = {
    val statForTask = getOrConstructStatistic(moleExecution, key)
   // Logger.getLogger(classOf[Statistic].getName).info("New sample for " + moleExecution + " " + key + " " + sample)
    statForTask += (sample, length)
  }

  private def getOrConstructStatistic(moleExecution: IMoleExecution, key: IStatisticKey): IStatisticSamples = {
    synchronized {
      val map = getOrConstructStatisticMap(moleExecution)
        
      map.get(key) match {
        case Some(m) => m
        case None => val stat = new StatisticSamples(historySize)
          map(key) = stat
          stat
      }
    }
  }

  private def getOrConstructStatisticMap(moleExecution: IMoleExecution): HashMap[IStatisticKey, IStatisticSamples] = {
    synchronized {
      stats.get(moleExecution) match {
        case Some(m) => m
        case None => 
          val m = new HashMap[IStatisticKey, IStatisticSamples] with SynchronizedMap[IStatisticKey, IStatisticSamples]
          stats += ((moleExecution, m))
          m
      }
    }
  }

}
