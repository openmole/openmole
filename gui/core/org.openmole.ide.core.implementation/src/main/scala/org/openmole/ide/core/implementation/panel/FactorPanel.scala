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

package org.openmole.ide.core.implementation.panel

import org.openmole.ide.core.implementation.sampling.FactorPanelUI
import org.openmole.ide.core.model.sampling.IFactorWidget
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel._
import org.openmole.ide.misc.widget.PluginPanel
import java.awt.BorderLayout

class FactorPanel(factorWidget: IFactorWidget,
                  scene: IMoleScene,
                  mode: PanelMode.Value) extends BasePanel(None,
  scene,
  mode) {

  val panelUI = new FactorPanelUI(factorWidget)

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += panelUI.peer
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  def create = {}

  def delete = true

  def save = {
    factorWidget.dataUI = panelUI.saveContent
    factorWidget.update
  }
}