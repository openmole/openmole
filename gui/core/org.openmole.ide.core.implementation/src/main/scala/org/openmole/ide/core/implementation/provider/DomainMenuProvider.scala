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

package org.openmole.ide.core.implementation.provider

import java.awt.Point
import org.netbeans.api.visual.widget._
import org.openmole.ide.core.model.panel.ISamplingCompositionPanelUI
import org.openmole.ide.core.model.sampling.{ IDomainWidget, ISamplingComponent }
import scala.swing.Action
import scala.swing.MenuItem
import org.openmole.ide.core.implementation.sampling.SamplingComponent

class DomainMenuProvider(panelScene: ISamplingCompositionPanelUI) extends GenericMenuProvider {

  override def getPopupMenu(widget: Widget,
                            point: Point) = {
    items.clear
    val itRemoveFactor = new MenuItem(new Action("Remove Domain") {
      def apply =
        widget match {
          case cw: ISamplingComponent ⇒ panelScene.remove(cw)
          case _ ⇒
        }
    })

    val itSetAsFinalDomain = new MenuItem(new Action("Set as final") {
      def apply = widget match {
        case cw: SamplingComponent ⇒
          cw.component match {
            case domainWidget: IDomainWidget ⇒
              panelScene.setFinalSampling(domainWidget.proxy)
            case _ ⇒
          }
        case _ ⇒
      }
    })
    items += itRemoveFactor.peer
    super.getPopupMenu(widget, point)
  }
}
