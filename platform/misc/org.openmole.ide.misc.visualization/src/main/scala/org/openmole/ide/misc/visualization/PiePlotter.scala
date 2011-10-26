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

import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.PiePlot
import org.jfree.data.general.DefaultPieDataset

class PiePlotter(title: String, data: Map[String, Double]){
  def this(title: String) = this(title,Map.empty[String,Double])

  val pieData = new DefaultPieDataset
  data.foreach(d=>pieData.setValue(d._1,d._2))
  
  val chart = ChartFactory.createPieChart(title,pieData , false, false, false)
  customize(chart.getPlot.asInstanceOf[PiePlot])
  
  def updateData(key: String, value: Double) = if(value>=0.0) pieData.setValue(key,value)
  
  def chartPanel = new ChartPanel(chart) {setPreferredSize(new Dimension(200,200))}
  
  private def customize(plot: PiePlot) = {
    chart.getTitle.setPaint(new Color(102,102,102))
    chart.getTitle.setFont(new Font("Ubuntu",Font.BOLD,15))
    chart.setAntiAlias(true)
    plot.setShadowPaint(new Color(0,0,0,0))
    plot.setBackgroundPaint(new Color(0,0,0,0))
    plot.setLabelBackgroundPaint(new Color(0,0,0,0))
    plot.setLabelOutlinePaint(new Color(0,0,0,0))
    plot.setLabelShadowPaint(new Color(0,0,0,0))
    plot.setLabelLinksVisible(false)
    plot.setIgnoreZeroValues(true)
    plot.setLabelPaint(new Color(102,102,102))
    
    import PlotterColor._
    plot.setSectionPaint("Ready",READY_COLOR._1)
    plot.setSectionPaint("Running",RUNNING_COLOR._1)
    plot.setSectionPaint("Completed",COMPLETED_COLOR._1)
    plot.setSectionPaint("Failed",FAILED_COLOR._1)
    plot.setSectionPaint("Canceled",CANCELED_COLOR._1)
    plot.setSectionPaint("Killed",KILLED_COLOR._1)
    plot.setSectionPaint("Done",COMPLETED_COLOR._1)
    plot.setSectionPaint("Submitted",SUBMITTED_COLOR._1)
  }
}