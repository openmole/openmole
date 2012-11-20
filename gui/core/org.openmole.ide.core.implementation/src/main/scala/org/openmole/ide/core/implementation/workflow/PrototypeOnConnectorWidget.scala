/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.implementation.workflow

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Rectangle
import java.awt.RenderingHints
import org.netbeans.api.visual.widget._
import org.openmole.ide.core.model.workflow.{ IConnectorViewUI, IConnectorUI, IMoleScene }
import org.openmole.ide.misc.widget.LinkLabel

object PrototypeOnConnectorWidget {
  val darkOnLight = (new Color(218, 218, 218), new Color(0, 0, 0, 180))

  val lightOnDark = (new Color(0, 0, 0, 180), new Color(200, 200, 200))
}

import PrototypeOnConnectorWidget._

class PrototypeOnConnectorWidget(scene: Scene,
                                 var connectorUI: IConnectorViewUI,
                                 val link: LinkLabel,
                                 val colorPattern: (Color, Color) = PrototypeOnConnectorWidget.lightOnDark) extends ComponentWidget(scene, link.peer) {
  link.foreground = colorPattern._2
  val dim = 30
  val pos = link.size.width / 2 + 1
  setPreferredBounds(new Rectangle(dim, dim))

  override def paintBackground = {
    val g = scene.getGraphics
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    g.setColor(colorPattern._1)
    g.fillOval(pos, pos, dim, dim)
    link.text = connectorUI.preview
    revalidate
  }

  override def paintBorder = {
    val g = scene.getGraphics
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    g.setStroke(new BasicStroke(3f))
    g.setColor(colorPattern._2)
    if (colorPattern == lightOnDark)
      g.drawOval(pos, pos, dim - 2, dim - 2)
    revalidate
  }
}

