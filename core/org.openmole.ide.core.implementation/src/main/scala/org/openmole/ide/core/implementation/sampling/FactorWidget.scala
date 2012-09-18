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

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.BorderLayout
import scala.swing.Action
import java.awt.GradientPaint
import java.awt.LinearGradientPaint
import java.awt.Graphics2D
import org.openmole.ide.misc.widget.NimbusPainter
import java.awt.Point
import java.awt.RenderingHints
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.data.IFactorDataUI
import org.openmole.ide.core.model.sampling.IFactorWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget._

class FactorWidget(var factor: IFactorDataUI,
                   display: Boolean = false) extends MigPanel("wrap", "[center]", "[center]") with IFactorWidget { factorWidget ⇒
  preferredSize = new Dimension(130, 38)
  val link = new LinkLabel(factorPreview,
    new Action("") {
      def apply = displayOnMoleScene
    },
    3,
    "73a5d2",
    true) { opaque = false; maximumSize = new Dimension(100, 28) }

  if (display) displayOnMoleScene

  def displayOnMoleScene = ScenesManager.currentSceneContainer match {
    case Some(s: ISceneContainer) ⇒ s.scene.displayExtraPropertyPanel(factorWidget)
    case _ ⇒
  }

  def factorPreview =
    factor.prototype.getOrElse("") + {
      factor.domain match {
        case Some(d: IDomainDataUI) ⇒ d.preview
        case _ ⇒ ""
      }
    } match {
      case "" ⇒ "define Factor"
      case x: String ⇒ x
    }

  def update = {
    link.link(factorPreview)
    revalidate
    repaint
  }

  override def paintComponent(g: Graphics2D) = {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON)

    val start = new Point(0, 0)
    val end = new Point(0, size.height)
    val dist = Array(0.0f, 0.5f, 0.8f)
    val colors = Array(new Color(250, 250, 250), new Color(228, 228, 228), new Color(250, 250, 250))
    val gp = new LinearGradientPaint(start, end, dist, colors)

    g.setPaint(gp)
    g.fillRoundRect(0, 0, size.width, size.height, 8, 8)
    g.setColor(Color.WHITE)
  }
  contents += link
}