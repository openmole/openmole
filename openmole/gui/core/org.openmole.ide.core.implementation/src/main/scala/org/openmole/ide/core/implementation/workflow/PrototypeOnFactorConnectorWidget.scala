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
package org.openmole.ide.core.implementation.workflow

import org.openmole.ide.misc.widget.LinkLabel
import java.awt.{ Rectangle, RenderingHints, Color, Font }
import org.netbeans.api.visual.widget.Scene

class PrototypeOnFactorConnectorWidget(sc: Scene,
                                       conUI: ConnectorViewUI,
                                       li: LinkLabel,
                                       colorP: (Color, Color) = PrototypeOnConnectorWidget.lightOnDark,
                                       altColorPattern: (Color, Color) = PrototypeOnConnectorWidget.lightOnDark,
                                       d: Int = 0) extends PrototypeOnConnectorWidget(sc, conUI, li, colorP, d) {
  link.font = link.font.deriveFont(Font.BOLD)
  setPreferredBounds(new Rectangle(30, 30))

  override def paintBackground = {
    val g = scene.getGraphics
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    link.text = connectorUI.preview
    if (connectorUI.preview == "?") {
      link.opaque = false
      setPreferredBounds(new Rectangle(30, 30))
      g.setColor(colorPattern._1)
      g.fillOval(pos, pos, 30, 30)
      link.foreground = colorPattern._2
      link.revalidate
    }

    else {
      setPreferredBounds(new Rectangle(80, 30))
      link.foreground = altColorPattern._2
      link.background = altColorPattern._1
      link.opaque = true
    }
    link.revalidate
  }
}