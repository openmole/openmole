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
import java.awt.Color
import org.openmole.ide.core.implementation.execution.ScenesManager
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.JavaConversions._
import BasePanelUI._

class EnvironmentPanelUI(proxy: IEnvironmentDataProxyUI,
                         scene: IMoleScene,
                         mode: Value = CREATION) extends BasePanelUI(proxy, scene, mode, new Color(68, 120, 33)) {
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.fatImagePath)))

  val panelUI = proxy.dataUI.buildPanelUI

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(panelUI.tabbedPane.peer, BorderLayout.CENTER)

  def create = {
    Proxys.environments += proxy
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
        scene.closePropertyPanel
        Proxys.environments -= proxy
        ConceptMenu.removeItem(proxy)
      case _ ⇒
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          capsulesWithEnv.foreach { _.setEnvironment(None) }
          delete
        }
    }
  }

  def save = proxy.dataUI = panelUI.saveContent(nameTextField.text)
}
