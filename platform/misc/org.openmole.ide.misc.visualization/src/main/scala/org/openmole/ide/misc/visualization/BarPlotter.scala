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


import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.labels.ItemLabelAnchor
import org.jfree.chart.labels.ItemLabelPosition
import org.jfree.chart.plot.CategoryPlot
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.renderer.category.BarRenderer
import org.jfree.data.category.DefaultCategoryDataset
import org.openmole.core.model.execution.ExecutionState._
import scala.collection.JavaConversions._
import java.awt.Paint
import java.awt.Paint._

class BarPlotter(title: String, val data: Map[String,Map[ExecutionState, Double]]){
  def this(t: String)  = this(t,Map.empty)

  val dataSet = new DefaultCategoryDataset
  data.keys.foreach(env=> data(env).keys.foreach(k=>dataSet.addValue(data(env)(k),k.name,env)))
  val chart = ChartFactory.createBarChart(
    "Environements",         // chart title
    "",               // domain axis label
    "",                  // range axis label
    dataSet,                  // data
    PlotOrientation.VERTICAL, // orientation
    false,                     // include legend
    true,                     // tooltips?
    false                     // URLs?
  )
  customize(chart.getPlot.asInstanceOf[CategoryPlot])
  
  def chartPanel = new ChartPanel(chart) {setPreferredSize(new Dimension(200,200))} 
  
  def updateData(env: String, key: ExecutionState, value: Double) = if(value>=0.0) dataSet.setValue(value,key.name,env)
  
  private def customize(plot: CategoryPlot) = {
    chart.getTitle.setPaint(new Color(102,102,102))
    chart.getTitle.setFont(new Font("Ubuntu",Font.BOLD,15))
    chart.setAntiAlias(true)
    // plot.setShadowPaint(new Color(0,0,0,0))
    plot.setBackgroundPaint(new Color(0,0,0,0))
    
    
    val barRenderer = plot.getRenderer.asInstanceOf[BarRenderer]
    barRenderer.setMaximumBarWidth(0.1)
    // dataSet.getColumnKeys.zipWithIndex.foreach {case (k,i) => barRenderer.setSeriesPaint(}
    import PlotterColor._
   
    barRenderer.setSeriesPaint(READY_COLOR._2, READY_COLOR._1)
    barRenderer.setSeriesPaint(SUBMITTED_COLOR._2, SUBMITTED_COLOR._1)
    barRenderer.setSeriesPaint(RUNNING_COLOR._2, RUNNING_COLOR._1)
    barRenderer.setSeriesPaint(DONE_COLOR._2,DONE_COLOR._1)
    barRenderer.setSeriesPaint(FAILED_COLOR._2,FAILED_COLOR._1)
    barRenderer.setSeriesPaint(KILLED_COLOR._2,KILLED_COLOR._1)
    barRenderer.setShadowVisible(false)
    plot.setRenderer(barRenderer)
        
//    val colorList = List[Color](Color.BLACK,Color.BLACK,Color.BLACK,Color.BLACK,Color.BLACK,Color.BLACK)
//    dataSet.getColumnKeys.zipWithIndex.foreach{case (k,i) => k match {
//      case ("Ready",x)=> colorList
//      }}
   
//    val renderer = new CustomRenderer(
//            List(Color.red, Color.blue, Color.green,
//                Color.yellow, Color.orange, Color.cyan,
//                Color.magenta, Color.blue).toArray)
   
    
    //  plot.setLabelBackgroundPaint(new Color(0,0,0,0))
    // plot.setLabelOutlinePaint(new Color(0,0,0,0))
    // plot.setLabelShadowPaint(new Color(0,0,0,0))
    // plot.setLabelLinksVisible(false)
    //  plot.setIgnoreZeroValues(true)
    //  plot.setLabelPaint(new Color(102,102,102))
    
    //  import PlotterColor._
//    plot.setSectionPaint("Ready",READY_COLOR)
//    plot.setSectionPaint("Running",RUNNING_COLOR)
//    plot.setSectionPaint("Completed",COMPLETED_COLOR)
//    plot.setSectionPaint("Failed",FAILED_COLOR)
//    plot.setSectionPaint("Canceled",CANCELED_COLOR)
//    plot.setSectionPaint("Killed",KILLED_COLOR)
//    plot.setSectionPaint("Done",COMPLETED_COLOR)
//    plot.setSectionPaint("Submitted",SUBMITTED_COLOR)
  }
}
