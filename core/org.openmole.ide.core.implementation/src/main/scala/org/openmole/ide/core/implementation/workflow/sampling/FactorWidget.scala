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

package org.openmole.ide.core.implementation.workflow.sampling

import java.awt.Color
import java.awt.Dimension
import java.awt.BorderLayout
import scala.swing.Action
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.model.data.IFactorDataUI
import org.openmole.ide.core.model.sampling.IFactorWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.MigPanel

class FactorWidget(val moleScene: IMoleScene,
                   var factor: IFactorDataUI) extends IFactorWidget { factorWidget ⇒
  preferredSize = new Dimension(100, 25)
  background = new Color(2, 240, 240)
  opaque = true
  peer.setLayout(new BorderLayout)
  val link = new LinkLabel(factorPreview,
    new Action("") { def apply = moleScene.displayExtraPropertyPanel(factorWidget) },
    3,
    "73a5d2",
    true)

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