/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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

import java.awt.{ Color, Rectangle, RenderingHints }
import org.netbeans.api.visual.widget._
import org.openmole.ide.core.model.workflow.{ IConnectorViewUI, IConnectorUI, IMoleScene }
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.core.model.sampling.ISamplingCompositionProxyUI

class OrderingOnConnectorWidget(scene: Scene,
                                val proxy: ISamplingCompositionProxyUI,
                                val link: LinkLabel) extends ComponentWidget(scene, link.peer) {
  val dim = 15
  val pos = link.size.width / 2 + 1
  setPreferredBounds(new Rectangle(dim, dim))
  link.foreground = Color.black

  override def paintBackground = {
    val g = scene.getGraphics
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)
    g.fillOval(pos, pos, dim, dim)
    g.setColor(Color.white)
    link.text = proxy.ordering.toString
    revalidate
  }
}
