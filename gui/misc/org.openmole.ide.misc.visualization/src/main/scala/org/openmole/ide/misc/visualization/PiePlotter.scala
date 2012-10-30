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

import de.erichseifert.gral.plots.PiePlot.PieSliceRenderer
import de.erichseifert.gral.data.DataTable
import de.erichseifert.gral.plots._
import de.erichseifert.gral.plots.colors._
import de.erichseifert.gral.util.Orientation
import de.erichseifert.gral.util.Orientation._
import de.erichseifert.gral.plots.points.PointRenderer
import de.erichseifert.gral.ui.InteractivePanel
import de.erichseifert.gral.util.Insets2D
import de.erichseifert.gral.Legend
import de.erichseifert.gral.Legend._
import de.erichseifert.gral.Location
import java.awt.Color
import java.awt.Color._
import org.openmole.core.model.job.State._
import java.awt.Dimension

class PiePlotter {

  val data = new DataTable(classOf[java.lang.Integer])
  data.add(0)
  data.add(0)
  data.add(0)

  // Create new pie plot
  val plot = new PiePlot(data)

  // Change relative size of pie
  plot.setSetting(PiePlot.RADIUS, 0.95)
  // Change relative size of inner region
  plot.getPointRenderer(data).setSetting(PieSliceRenderer.RADIUS_INNER, 0.25)
  // Change the width of gaps between segments
  plot.getPointRenderer(data).setSetting(PieSliceRenderer.GAP, 0.2)
  // Display labels
  plot.getPointRenderer(data).setSetting(PointRenderer.VALUE_DISPLAYED, true)
  plot.getPointRenderer(data).setSetting(PointRenderer.VALUE_COLOR, Color.WHITE)
  // Change the colors
  val colors = new IndexedColors(new Color(77, 77, 77), new Color(187, 200, 7), new Color(170, 0, 0))
  colors.setMode(ColorMapper.Mode.REPEAT)
  plot.getPointRenderer(data).setSetting(PieSliceRenderer.COLORS, colors)
  plot.setInsets(new Insets2D.Double(0.0, 0.0, 0.0, 0.0))

  plot.setSetting(Plot.LEGEND, true)
  plot.setSetting(Plot.LEGEND_LOCATION, Location.SOUTH_WEST)

  def update(ready: Int,
             completed: Int,
             canceled: Int): Unit = {
    updateReady(ready)
    updateCompleted(completed)
    updateCancel(canceled)
  }

  def update(key: State, value: Int): Unit = {
    key match {
      case READY ⇒ updateReady(value)
      case COMPLETED ⇒ updateCompleted(value)
      case CANCELED ⇒ updateCancel(value)
      case _ ⇒
    }
  }

  def updateReady(ready: Int) = { data.set(0, 0, ready); panel.repaint() }
  def updateCompleted(completed: Int) = { data.set(0, 1, completed); panel.repaint() }
  def updateCancel(canceled: Int) = { data.set(0, 2, canceled); panel.repaint() }

  val panel = new InteractivePanel(plot) {
    setZoomable(false)
    setPannable(false)
  }
  panel.setPreferredSize(new Dimension(200, 200))
}
