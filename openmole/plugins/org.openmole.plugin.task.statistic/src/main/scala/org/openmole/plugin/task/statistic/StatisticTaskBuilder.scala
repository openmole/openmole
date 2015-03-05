/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.task.statistic

import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.PluginSet

import scala.collection.mutable.ListBuffer

class StatisticTaskBuilder extends TaskBuilder { builder ⇒
  private var _sequences = new ListBuffer[(Prototype[Array[Double]], Prototype[Double], StatisticalAggregation[Double])]

  def sequences = _sequences.toList

  def addStatistic(sequence: Prototype[Array[Double]], stat: Prototype[Double], agg: StatisticalAggregation[Double]): this.type = {
    this addInput sequence
    this addOutput stat
    _sequences += ((sequence, stat, agg))
    this
  }

  def toTask = new StatisticTask with super.Built {
    val statistics = builder.sequences
  }
}
