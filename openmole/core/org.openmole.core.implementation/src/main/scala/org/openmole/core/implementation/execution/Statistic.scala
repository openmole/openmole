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

import org.openmole.core.model.execution.IStatistic
import org.openmole.core.model.execution.IStatisticKey
import org.openmole.core.model.execution.IStatisticSample
import org.openmole.core.model.execution.IStatisticSamples

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

class Statistic(historySize: Int) extends IStatistic {

  val stats = new WeakHashMap[String, HashMap[IStatisticKey, IStatisticSamples]] with SynchronizedMap[String, HashMap[IStatisticKey, IStatisticSamples]]

  override def apply(moleExecutionId: String, key: IStatisticKey): IStatisticSamples =
    stats.getOrElse(moleExecutionId, return StatisticSamples.empty).getOrElse(key, StatisticSamples.empty)

  override def += (moleExecutionId: String, key: IStatisticKey, sample: IStatisticSample) = synchronized {
    stats.getOrElseUpdate(moleExecutionId, new HashMap[IStatisticKey, IStatisticSamples] with SynchronizedMap[IStatisticKey, IStatisticSamples]).getOrElseUpdate(key, new StatisticSamples(historySize)) += sample
  }

}
