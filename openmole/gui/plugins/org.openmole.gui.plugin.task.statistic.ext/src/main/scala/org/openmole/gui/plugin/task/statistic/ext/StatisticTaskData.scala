package org.openmole.gui.plugin.task.statistic.ext

/*
 * Copyright (C) 24/02/2015 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.gui.ext.data.{InAndOutput, TaskData, InOutput}

object StatisticType extends Enumeration {

  case class StatisticType(uuid: String, name: String) extends Val(name)
  val SUM = new StatisticType("SUM", "Sum")
  val MEDIAN = new StatisticType("MEDIAN", "Median")

 val ALL = Seq(SUM, MEDIAN)
}

case class StatisticTaskData(inputs: Seq[InOutput] = Seq(),
                             outputs: Seq[InOutput] = Seq(),
                             inAndOutputs: Seq[InAndOutput] = Seq()) extends TaskData {
}