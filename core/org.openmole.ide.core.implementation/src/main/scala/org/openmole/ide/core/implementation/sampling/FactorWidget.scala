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

import java.awt.Color
import java.awt.Dimension
import java.awt.BorderLayout
import scala.swing.Action
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.data.IFactorDataUI
import org.openmole.ide.core.model.sampling.IFactorWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.MigPanel

class FactorWidget(var factor: IFactorDataUI,
                   display: Boolean = false) extends IFactorWidget { factorWidget ⇒
  preferredSize = new Dimension(130, 25)
  background = new Color(2, 240, 240)
  opaque = true
  peer.setLayout(new BorderLayout)
  val link = new LinkLabel(factorPreview,
    new Action("") {
      def apply = displayOnMoleScene
    },
    3,
    "73a5d2",
    true)

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

  peer.add(new MigPanel("") {
    contents += link
  }.peer, BorderLayout.NORTH)
}