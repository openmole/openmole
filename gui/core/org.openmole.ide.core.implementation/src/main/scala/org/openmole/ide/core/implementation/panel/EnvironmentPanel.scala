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

import java.awt.BorderLayout
import org.openmole.ide.core.implementation.execution.ScenesManager
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.dataproxy.Proxies
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.JavaConversions._
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent
import scala.swing.Component
import scala.swing.event.FocusGained

class EnvironmentPanel(proxy: IEnvironmentDataProxyUI,
                       scene: IMoleScene,
                       val index: Int) extends BasePanel(Some(proxy), scene) {
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.fatImagePath)))

  val panelUI = proxy.dataUI.buildPanelUI
  def created = Proxies.instance.contains(proxy)

  listenTo(panelUI.help.components.toSeq: _*)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
  }

  refreshPanel

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    if (tabbedPane.pages.size == 0) contents += panelUI.peer
    else contents += tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  def create = {
    Proxies.instance += proxy
    scene.manager.invalidateCache
    ConceptMenu.environmentMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }

  def delete = {
    val capsulesWithEnv = ScenesManager.moleScenes.flatMap {
      _.manager.capsules.values.filter {
        _.dataUI.environment == Some(proxy)
      }
    }.toList
    capsulesWithEnv match {
      case Nil ⇒
        scene.closePropertyPanel(0)
        Proxies.instance -= proxy
        ConceptMenu.removeItem(proxy)
        true
      case _ ⇒
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          capsulesWithEnv.foreach { _.environment_=(None) }
          delete
        } else false
    }
  }

  def save = proxy.dataUI = panelUI.saveContent(nameTextField.text)
}
