/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.task.statistic

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.task._
import scala.collection.mutable.ListBuffer

object StatisticTask {
  def apply() = new StatisticTaskBuilder
}

abstract class StatisticTask extends Task {

  def statistics: Iterable[(Prototype[Array[Double]], Prototype[Double], StatisticalAggregation[Double])]

  override def process(context: Context)(implicit rng: RandomProvider) =
    Context(
      statistics.map {
        case (sequence, statProto, agg) ⇒ Variable(statProto, agg(context(sequence)))
      })

}
