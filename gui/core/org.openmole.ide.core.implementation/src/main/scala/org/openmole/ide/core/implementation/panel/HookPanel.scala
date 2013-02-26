/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.core.implementation.panel

import org.openmole.ide.misc.widget.{ LinkLabel, PluginPanel }
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode
import org.openmole.ide.core.model.dataproxy.IHookDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import scala.swing.Component
import java.awt.BorderLayout
import javax.swing.ImageIcon
import javax.imageio.ImageIO
import swing.event.FocusGained
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent

class HookPanel(proxy: IHookDataProxyUI,
                scene: IMoleScene,
                mode: PanelMode.Value) extends BasePanel(Some(proxy), scene, mode) { capsulePanel ⇒
  iconLabel.icon = new ImageIcon(ImageIO.read(this.getClass.getClassLoader.getResource("img/hook.png")))
  /*listenTo(panelUI.help.components.toSeq: _*)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
  }   */

  def create = {
    Proxys.hooks += proxy
    ConceptMenu.hookMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }

  def delete = {
    scene.closePropertyPanel
    Proxys.hooks -= proxy
    ConceptMenu.removeItem(proxy)
    true
  }

  def save = proxy.dataUI = panelUI.saveContent(nameTextField.text)

  val panelUI = proxy.dataUI.buildPanelUI

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    if (panelUI.tabbedPane.pages.size == 0) contents += panelUI.peer
    else contents += panelUI.tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)
}