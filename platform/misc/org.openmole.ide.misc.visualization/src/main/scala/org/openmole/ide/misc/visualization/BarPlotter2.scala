/*
 * Copyright (C) 2011 leclaire
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

import de.erichseifert.gral.data.DataSource
import de.erichseifert.gral.data.DataTable
import de.erichseifert.gral.plots.BarPlot
import de.erichseifert.gral.plots.BarPlot._
import de.erichseifert.gral.plots.points.PointRenderer
import de.erichseifert.gral.ui.InteractivePanel
import de.erichseifert.gral.Location
import de.erichseifert.gral.util.GraphicsUtils
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.LinearGradientPaint
import javax.swing.JPanel
import org.openmole.core.model.execution.ExecutionState._
import de.erichseifert.gral.data.DataSeries
import scala.collection.mutable.HashMap
import scala.swing.Panel

class BarPlotter2(title: String, val dataKeys: List[String]) extends JPanel{
  
  var dataValues = new HashMap[String, DataTable]
  
  dataKeys.foreach{d=>
    val t = new DataTable(classOf[java.lang.Integer],classOf[java.lang.Integer])
    t.add(1,8)
    t.add(2,18)
    dataValues+= d->t
  }
//  
//  val plot = new BarPlot(new DataSeries("Ready",dataValues("Ready"),1),
//                         new DataSeries("Submitted",dataValues("Submitted"),1))
  
  val plot = new BarPlot(dataValues("Ready"))
  plot.setSetting(BarPlot.BAR_WIDTH, 0.75)
   
  val COLOR1 = new Color( 55, 170, 200)
  // Format bars
  val pointRenderer = plot.getPointRenderer(dataValues("Ready"))
  pointRenderer.setSetting(PointRenderer.COLOR,
                           new LinearGradientPaint(0f,0f, 0f,1f,
                                                   List(0.0f,1.0f).toArray,
                                                   List(COLOR1, GraphicsUtils.deriveBrighter(COLOR1)).toArray))
  
  pointRenderer.setSetting(BarPlot.BarRenderer.STROKE, new BasicStroke(3f))
  pointRenderer.setSetting(BarPlot.BarRenderer.STROKE_COLOR,
                           new LinearGradientPaint(0f,0f, 0f,1f,
                                                   List(0.0f,1.0f).toArray,
                                                   List(COLOR1, GraphicsUtils.deriveBrighter(COLOR1)).toArray))
  
  pointRenderer.setSetting(PointRenderer.VALUE_DISPLAYED, true)
  pointRenderer.setSetting(PointRenderer.VALUE_LOCATION, Location.CENTER);
  pointRenderer.setSetting(PointRenderer.VALUE_COLOR, GraphicsUtils.deriveDarker(COLOR1));
  pointRenderer.setSetting(PointRenderer.VALUE_FONT,
                           Font.decode(null).deriveFont(Font.BOLD))

  add(new InteractivePanel(plot))
  
// val plot = new BarPlot(dataValues.map{case(k,v)=> new DataSeries(k,v,10)})
  
// plot.setSetting(BarPlot.TITLE,title)
  
  def updateData(key: String, value: Double) = println("update " + key + " "+value)
  
  def chartPanel = {
    val p = new InteractivePanel(plot)
    p.setPannable(false)
    p.setZoomable(false)
    p
  }
}
