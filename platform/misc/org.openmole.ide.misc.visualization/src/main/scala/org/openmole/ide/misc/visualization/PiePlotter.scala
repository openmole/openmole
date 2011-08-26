/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: foryou can redistribute it and/or modify
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

package org.openmole.ide.misc.visualization

import java.lang.Comparable
import org.jfree.chart.ChartFactory
import org.jfree.data.general.DefaultPieDataset
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart

class PiePlotter(title: String, data: Map[String, Integer]) {
  def this(title: String) = this(title,Map.empty[String,Integer])

  val pieData = new DefaultPieDataset
  val chart = ChartFactory.createPieChart(title,pieData , false, true, false)
  
  def updateData(key: String, value: Double) = pieData.setValue(key,value)
  
  def chartPanel = new ChartPanel(chart)
}