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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import de.erichseifert.gral.util.Orientation
import de.erichseifert.gral.Legend
import de.erichseifert.gral.Location
import java.awt.Dimension

class XYPlotter(buffer_size: Int,
                nbInterval: Int) {

  val data = new DataTable(classOf[java.lang.Long],
    classOf[java.lang.Integer],
    classOf[java.lang.Integer])
  val start = System.currentTimeMillis
  for (i ← nbInterval to 1 by -1) {
    data.add((start - (i * buffer_size)).toLong, null, null)
  }

  val data2 = new DataSeries("Submitted", data, 0, 1)
  val data3 = new DataSeries("Running", data, 0, 2)

  val plot = new XYPlot(data2, data3)
  plot.setSetting(Plot.LEGEND, true)
  plot.setSetting(Plot.LEGEND_LOCATION, Location.NORTH_WEST)
  plot.getLegend.setSetting(Legend.ORIENTATION, Orientation.HORIZONTAL)

  plot.setInsets(new Insets2D.Double(0.0, 60.0, 40.0, 10.0))

  val axisRendererY = plot.getAxisRenderer(XYPlot.AXIS_Y)
  axisRendererY.setSetting(AxisRenderer.TICK_LABELS_FORMAT, new DecimalFormat)

  val axisRendererX = plot.getAxisRenderer(XYPlot.AXIS_X)
  axisRendererX.setSetting(AxisRenderer.TICKS_SPACING, 60000);
  axisRendererX.setSetting(AxisRenderer.TICK_LABELS_FORMAT, new SimpleDateFormat("HH:mm"))

  // Format data series
  formatFilledArea(plot, data2, new Color(77, 77, 77))
  formatFilledArea(plot, data3, new Color(187, 200, 7))

  // Add plot to Swing component
  val panel = new InteractivePanel(plot)
  panel.setPreferredSize(new Dimension(600, 250))
  panel.setZoomable(false)
  panel.setPannable(false)
  update(new States(0, 0, 0))

  def update(states: States): Unit = synchronized {
    import states._
    val time = System.currentTimeMillis

    data.add(time, submitted, running)
    val col1 = data.getColumn(0)

    plot.getAxis(XYPlot.AXIS_X).setRange(col1.getStatistics(Statistics.MIN),
      col1.getStatistics(Statistics.MAX))
    data.remove(0)

    plot.getAxis(XYPlot.AXIS_Y).setRange(0, 1.5 * scala.math.max(data.getColumn(1).getStatistics(Statistics.MAX),
      data.getColumn(2).getStatistics(Statistics.MAX)))
    panel.repaint()
  }

  def formatFilledArea(plot: XYPlot, data: DataSource, color: Color) = {
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
