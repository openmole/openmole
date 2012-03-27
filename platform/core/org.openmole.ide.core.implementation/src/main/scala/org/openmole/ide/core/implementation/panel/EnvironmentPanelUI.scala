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
import javax.swing.ImageIcon
import org.openide.util.ImageUtilities
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.JavaConversions._

class EnvironmentPanelUI(proxy: IEnvironmentDataProxyUI,
                         scene: IMoleScene,
                         mode: Value = CREATION) extends BasePanelUI(proxy, scene, mode,new Color(68,120,33)){
  iconLabel.icon = new ImageIcon(ImageUtilities.loadImage(proxy.dataUI.fatImagePath))
  
  val panelUI = proxy.dataUI.buildPanelUI
  mainPanel.contents += panelUI.peer
  
  def create = {
    Proxys.environments += proxy
    ConceptMenu.environmentMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }
  
  def delete = {
    val capsulesWithEnv : List[ICapsuleUI] = TopComponentsManager.moleScenes.map{_.manager.capsules.values.filter{_.dataUI.environment == Some(proxy)}}.flatten.toList
    capsulesWithEnv match {
      case Nil => Proxys.environments -= proxy
        ConceptMenu.removeItem(proxy)
      case _ => 
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          capsulesWithEnv.foreach{_.setEnvironment(None)}
          scene.closePropertyPanel
          delete
        }
    }
  }
  
  def save = proxy.dataUI = panelUI.saveContent(nameTextField.text)
}
