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

import java.awt.Color
import org.openmole.ide.core.implementation.execution.ScenesManager
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.dataproxy.ISamplingDataProxyUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import BasePanelUI._

class SamplingPanelUI(proxy: ISamplingDataProxyUI,
                      scene: IMoleScene,
                      mode: Value = CREATION) extends BasePanelUI(proxy, scene,mode,new Color(80,118,152)){
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.fatImagePath)))
  val panelUI = proxy.dataUI.buildPanelUI
  mainPanel.contents += panelUI.peer
  
  def create = {
    Proxys.samplings += proxy
    ConceptMenu.samplingMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }
  
  def delete = {
    val toBeRemovedSamplings  = ScenesManager.explorationCapsules.filter{case(c,d) => d.sampling == Some(proxy)}
    toBeRemovedSamplings match {
      case Nil => 
        scene.closePropertyPanel
        Proxys.samplings -= proxy
        ConceptMenu.removeItem(proxy)
      case _ => 
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          toBeRemovedSamplings.foreach{case(c,d) => c.scene.graphScene.removeNodeWithEdges(c.scene.manager.removeCapsuleUI(c))}
          delete
        }
    }
  }
  
  def save = proxy.dataUI = panelUI.saveContent(nameTextField.text)
}
