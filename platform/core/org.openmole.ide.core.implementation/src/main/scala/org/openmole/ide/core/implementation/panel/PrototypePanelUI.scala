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
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.core.model.panel.PanelMode._

class PrototypePanelUI[T](proxy: IPrototypeDataProxyUI,
                         scene: IMoleScene,
                         mode: Value = CREATION) extends BasePanelUI(proxy, scene,mode){
  iconLabel.icon = new ImageIcon(ImageTool.loadImage(proxy.dataUI.fatImagePath,50,50))
  peer.setBorder(BorderFactory.createLineBorder(new Color(102,102,102),2))
  val panelUI = proxy.dataUI.buildPanelUI
  mainPanel.contents += panelUI.peer
  
  def create = {
    Proxys.prototypes += proxy
    ConceptMenu.prototypeMenu.contents += ConceptMenu.addItem(proxy.dataUI.name, proxy)
  }
  
  def delete = {
    Proxys.prototypes -= proxy
    ConceptMenu.removeItem(proxy)
  }
  
  def save = proxy.dataUI = panelUI.saveContent(nameTextField.text)
  
}
