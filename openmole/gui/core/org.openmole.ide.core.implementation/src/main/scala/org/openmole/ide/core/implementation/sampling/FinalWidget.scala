/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.sampling

import org.openmole.ide.misc.widget.MigPanel
import java.awt.{ Dimension, Color, RenderingHints, Graphics2D }
import javax.imageio.ImageIO

class FinalWidget extends MigPanel("") {

  preferredSize = new Dimension(40, 40)
  override def paintComponent(g: Graphics2D) = {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)

    g.setPaint(Color.WHITE)
    g.fillOval(0, 0, 30, 30)
    g.drawImage(ImageIO.read(getClass.getClassLoader.getResource("img/finalSampling.png")), 10, 8, 10, 14, peer)

  }
}