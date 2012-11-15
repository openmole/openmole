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

package org.openmole.ide.core.implementation.sampling

import java.awt._
import scala.swing.Action
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.data.IFactorDataUI
import org.openmole.ide.core.model.sampling.{ IDomainProxyUI, IDomainWidget }
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget._
import javax.swing.BorderFactory
import org.openmole.ide.core.model.panel.ISamplingCompositionPanelUI

class DomainWidget(val proxy: IDomainProxyUI,
                   scenePaneUI: ISamplingCompositionPanelUI,
                   display: Boolean = false) extends MigPanel("wrap", "[center]", "[center]") with IDomainWidget {
  domainWidget ⇒
  preferredSize = new Dimension(100, 38)
  val link = new LinkLabel(domainPreview,
    new Action("") {
      def apply = displayOnMoleScene
    },
    3,
    "73a5d2",
    true) {
    opaque = false
    maximumSize = new Dimension(80, 30)
  }

  var color = SamplingCompositionPanelUI.DEFAULT_COLOR

  if (display) displayOnMoleScene

  def displayOnMoleScene = ScenesManager.currentSceneContainer match {
    case Some(s: ISceneContainer) ⇒ s.scene.displayExtraPropertyPanel(domainWidget)
    case _ ⇒
  }

  def domainPreview = proxy.dataUI.preview

  def update = {
    link.link(domainPreview)
    scenePaneUI.testConnections
    revalidate
    repaint
  }

  override def paintComponent(g: Graphics2D) = {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)

    val start = new Point(0, 0)
    val end = new Point(0, size.height)
    val dist = Array(0.0f, 0.5f, 0.8f)
    val colors = Array(color, new Color(228, 228, 228), color)
    val gp = new LinearGradientPaint(start, end, dist, colors)

    g.setPaint(gp)
    g.fillRoundRect(0, 0, size.width, size.height, 8, 8)
  }

  contents += link
}