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

import org.openmole.ide.core.implementation.sampling.DomainPanelUI
import org.openmole.ide.core.model.sampling.IDomainWidget
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel._
import org.openmole.ide.misc.widget.PluginPanel
import java.awt.BorderLayout
import swing.event.FocusGained
import swing.Component
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent

class DomainPanel(domainWidget: IDomainWidget,
                  scene: IMoleScene,
                  mode: PanelMode.Value) extends BasePanel(None,
  scene,
  mode) {
  val panelUI = new DomainPanelUI(domainWidget, this)

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += panelUI.peer
  }.peer, BorderLayout.CENTER)
  peer.add(panelUI.dPanel.help.peer, BorderLayout.SOUTH)

  def create = {}

  def delete = true

  def save = {
    domainWidget.dataUI = panelUI.saveContent
    domainWidget.update
  }

  def updateHelp = {
    if (peer.getComponentCount == 3) peer.remove(2)
    peer.add(panelUI.dPanel.help.peer, BorderLayout.SOUTH)
    revalidate
    repaint
  }
}