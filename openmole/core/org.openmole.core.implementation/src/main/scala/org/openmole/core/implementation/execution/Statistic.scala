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


import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

class Statistic(historySize: Int)  {

  val stats = new WeakHashMap[String, HashMap[StatisticKey, StatisticSamples]] with SynchronizedMap[String, HashMap[StatisticKey, StatisticSamples]]

  def apply(moleExecutionId: String, key: StatisticKey): StatisticSamples =
    stats.getOrElse(moleExecutionId, return StatisticSamples.empty).getOrElse(key, StatisticSamples.empty)

  def += (moleExecutionId: String, key: StatisticKey, sample: StatisticSample) = synchronized {
    stats.getOrElseUpdate(moleExecutionId, new HashMap[StatisticKey, StatisticSamples] with SynchronizedMap[StatisticKey, StatisticSamples]).getOrElseUpdate(key, new StatisticSamples(historySize)) += sample
  }

}
