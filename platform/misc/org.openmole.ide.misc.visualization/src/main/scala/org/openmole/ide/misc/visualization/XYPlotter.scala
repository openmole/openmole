/*
 * Copyright (C) 2011 mathieu
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

import de.erichseifert.gral.data.DataSeries
import de.erichseifert.gral.data.DataSource
import de.erichseifert.gral.data.DataTable
import de.erichseifert.gral.data.statistics.Statistics
import de.erichseifert.gral.plots.Plot
import de.erichseifert.gral.plots.XYPlot
import de.erichseifert.gral.plots.areas.DefaultAreaRenderer2D
import de.erichseifert.gral.plots.axes.AxisRenderer
import de.erichseifert.gral.plots.lines.DefaultLineRenderer2D
import de.erichseifert.gral.plots.lines.LineRenderer
import de.erichseifert.gral.plots.areas.AreaRenderer
import de.erichseifert.gral.ui.InteractivePanel
import de.erichseifert.gral.util.GraphicsUtils
import de.erichseifert.gral.util.Insets2D
import java.awt.Color
import java.text.SimpleDateFormat
import org.openmole.core.model.execution.ExecutionState
import de.erichseifert.gral.util.Orientation
import de.erichseifert.gral.Legend
import de.erichseifert.gral.Location
import java.awt.Dimension

class XYPlotter(t: String,
                totalDuration: Int,
                nbInterval: Int){
  val buffer_size = totalDuration/nbInterval
  var yMax = 0
  var ready = 0
  var submitted = 0
  var running = 0
  
  
  val data = new DataTable(classOf[java.lang.Long],
                           classOf[java.lang.Integer],
                           classOf[java.lang.Integer],
                           classOf[java.lang.Integer])
  val start = System.currentTimeMillis
  for(i <- nbInterval to 1 by -1) {
    data.add((start - (i*buffer_size)).toLong, null, null, null)
  }
  
  val data1 = new DataSeries("Ready", data, 0, 1)
  val data2 = new DataSeries("Submitted", data, 0, 2)
  val data3 = new DataSeries("Running", data, 0, 3)
  
  val plot = new XYPlot(data1, data2, data3)
  title(t)
  plot.setSetting(Plot.LEGEND, true)
  plot.setSetting(Plot.LEGEND_LOCATION, Location.NORTH_WEST)
  plot.getLegend.setSetting(Legend.ORIENTATION, Orientation.HORIZONTAL)
  
  plot.setInsets(new Insets2D.Double(20.0, 40.0, 40.0, 40.0))
  
  val axisRendererY = plot.getAxisRenderer(XYPlot.AXIS_Y);
                           
  val axisRendererX = plot.getAxisRenderer(XYPlot.AXIS_X)
  axisRendererX.setSetting(AxisRenderer.TICKS_SPACING, buffer_size);
  axisRendererX.setSetting(AxisRenderer.TICK_LABELS_FORMAT, new SimpleDateFormat("HH:mm"))
  
  // Format data series
  formatFilledArea(plot, data1, new Color(77,77,77))
  formatFilledArea(plot, data2, new Color(187,200,7))
  formatFilledArea(plot, data3, new Color(55,170,20))
  
  // Add plot to Swing component
  val panel = new InteractivePanel(plot)
  panel.setPreferredSize(new Dimension(600,200))
  panel.setZoomable(false)
  panel.setPannable(false)
  
  def title(t: String) = plot.setSetting(Plot.TITLE,t)
  
  def update(re: Int,
             s: Int,
             ru: Int): Unit = {
    val time = System.currentTimeMillis
    
    ready = re
    submitted = s
    running = ru
    
    data.add(time,ready,submitted,running)
    val col1 = data.getColumn(0)
                                              
    plot.getAxis(XYPlot.AXIS_X).setRange(col1.getStatistics(Statistics.MIN),
                                         col1.getStatistics(Statistics.MAX))
    data.remove(0)
    
    
    yMax = scala.math.max(scala.math.max(ready,submitted),scala.math.max(running,yMax))
    plot.getAxis(XYPlot.AXIS_Y).setRange(0, 2.5*yMax)
    panel.repaint()
  }
  
  def update(key: ExecutionState.ExecutionState, value: Int): Unit = {
    key match {
      case ExecutionState.READY=> update(value,submitted,running)
      case ExecutionState.SUBMITTED=> update(ready,value,running)
      case ExecutionState.RUNNING=> update(ready,submitted,value)
      case _=>
    }
  }
  
  def formatFilledArea(plot: XYPlot,data: DataSource,color: Color) = {
    plot.setPointRenderer(data, null)
    val line = new DefaultLineRenderer2D
    line.setSetting(LineRenderer.COLOR, color)
    line.setSetting(LineRenderer.GAP, 3.0)
    line.setSetting(LineRenderer.GAP_ROUNDED, true)
    plot.setLineRenderer(data, line)
    val area = new DefaultAreaRenderer2D
    area.setSetting(AreaRenderer.COLOR, GraphicsUtils.deriveWithAlpha(color, 64))
    plot.setAreaRenderer(data, area)
  }
}
