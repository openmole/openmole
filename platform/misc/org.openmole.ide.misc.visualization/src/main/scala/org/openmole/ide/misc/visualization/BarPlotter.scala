/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.misc.visualization


import java.awt.Dimension
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset
import org.openmole.core.model.execution.ExecutionState._

class BarPlotter(title: String, val data: Map[String,Map[ExecutionState, Double]]){
  def this(t: String)  = this(t,Map.empty)

  val dataSet = new DefaultCategoryDataset
  data.keys.foreach(env=> data(env).keys.foreach(k=>dataSet.addValue(data(env)(k),env,k.name)))
  val chart = ChartFactory.createBarChart(
    "Bar Chart Demo",         // chart title
    "Category",               // domain axis label
    "Value",                  // range axis label
    dataSet,                  // data
    PlotOrientation.VERTICAL, // orientation
    true,                     // include legend
    true,                     // tooltips?
    false                     // URLs?
  )
  
  def chartPanel = new ChartPanel(chart) {setPreferredSize(new Dimension(200,200))} 
  
  def updateData(env: String, key: ExecutionState, value: Double) = if(value>=0.0) dataSet.setValue(value,env,key.name)
}
