package org.openmole.gui.plugin.task.statistic.client

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

import org.openmole.gui.plugin.task.statistic.ext._
import org.openmole.gui.client.core.dataui._
import org.openmole.gui.client.core.dataui.IOMappingFactory._

import rx._

class StatisticTaskDataUI(val name: Var[String] = Var("")) extends InAndOutTaskDataUI {

  def data = new StatisticTaskData(inputDataUI().data.inputs, outputDataUI().data.outputs, inAndOutDataUI().data.inAndOutputs)

  def panelUI = new StatisticTaskPanelUI(this)

  def dataType = "Statistic"

  def inAndOutMappingsFactory = IOMappingsFactory(Seq(
    selectField("Statistic", StatisticType.MEDIAN, StatisticType.ALL)
  ),dimension1Filter, dimension0Filter
  )
}