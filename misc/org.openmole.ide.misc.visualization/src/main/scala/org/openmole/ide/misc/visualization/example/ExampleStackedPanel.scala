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

package org.openmole.ide.misc.visualization.example

import java.awt.Color
import java.awt.Dimension
import org.openmole.ide.misc.visualization._
import org.openmole.ide.misc.widget.MigPanel
import scala.util.Random
import org.openmole.core.model.job.State._

class ExampleStackedPanel extends MigPanel("", "[][grow,fill]", "") {
  val g = new Random(51)

  //PiePlotter    
  val plot = new PiePlotter
  plot.update(READY, 8)
  plot.update(COMPLETED, 8)
  plot.update(CANCELED, 4)

  //XYPLOT
  val plot2 = new XYPlotter(3000, 20) {
    background = Color.white
  }

  peer.add(plot.panel)
  peer.add(plot2.panel)

  for (i ← 1 to 5) {
    println(i)
    Thread.sleep(7000)
    plot2.update(new States(200, 100, 50))
  }

  preferredSize = new Dimension(200, 800)

}
