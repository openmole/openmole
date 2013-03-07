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
import swing.{ Label, TabbedPane, Component }
import java.awt.{ Dimension, BorderLayout }
import javax.swing.ImageIcon
import javax.imageio.ImageIO
import swing.event.FocusGained
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent
import org.openmole.ide.core.implementation.dialog.StatusBar

class HookPanel(proxy: IHookDataProxyUI,
                scene: IMoleScene,
                mode: PanelMode.Value) extends BasePanel(Some(proxy), scene, mode) { hookPanel ⇒
  iconLabel.icon = new ImageIcon(ImageIO.read(this.getClass.getClassLoader.getResource("img/hook.png")))
  /*listenTo(panelUI.help.components.toSeq: _*)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
  }   */
  val panelUI = proxy.dataUI.buildPanelUI

  val protoPanel = Proxys.prototypes.size match {
    case 0 ⇒
      StatusBar().inform("No Prototype has been created yet")
      panelUI.tabbedPane.pages += new TabbedPane.Page("Inputs / Outputs", new Label("First define Prototypes !"))
      None
    case _ ⇒
      val (implicitIP, implicitOP) = proxy.dataUI.implicitPrototypes
      val iop = Some(new IOPrototypePanel(scene,
        proxy.dataUI.inputs,
        proxy.dataUI.outputs,
        implicitIP,
        implicitOP,
        proxy.dataUI.inputParameters.toMap))
      panelUI.tabbedPane.pages.insert(0, new TabbedPane.Page("Inputs / Outputs", iop.get))
      iop
  }

  def create = {
    Proxys += proxy
    ConceptMenu.hookMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }

  def delete = {
    scene.closePropertyPanel
    Proxys -= proxy
    ConceptMenu.removeItem(proxy)
    true
  }

  def save = {
    val protoPanelSave = IOPrototypePanel.save(protoPanel)
    proxy.dataUI = panelUI.save(nameTextField.text, protoPanelSave._1, protoPanelSave._2, protoPanelSave._3)
  }

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += panelUI.tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  listenTo(panelUI.help.components.toSeq: _*)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
  }
}