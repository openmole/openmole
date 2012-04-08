/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.task.exploration


import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing._
import scala.swing.event.SelectionChanged
import swing.Swing._
import javax.swing.ImageIcon
import org.openide.util.ImageUtilities
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.core.model.dataproxy.ISamplingDataProxyUI
import org.openmole.ide.core.model.panel.ITaskPanelUI

class ExplorationTaskPanelUI (pud: ExplorationTaskDataUI) extends PluginPanel("wrap 3") with ITaskPanelUI {
  val samplingComboBox = new ComboBox(comboContent) 
  {tooltip = Help.tooltip("The name of the sampling to be executed")}

  contents += new Label("Sampling")
  contents += samplingComboBox
  val linkLabel : LinkLabel = new LinkLabel("",contentAction(pud.sampling.getOrElse(emptyProxy))) {
    icon = new ImageIcon(ImageUtilities.loadImage("img/eye.png"))
  }
  
  samplingComboBox.selection.item = pud.sampling.getOrElse(emptyProxy)
  listenTo(`samplingComboBox`)
  samplingComboBox.selection.reactions += {
    case SelectionChanged(`samplingComboBox`)=> 
      linkLabel.action = contentAction(samplingComboBox.selection.item) 
  }
  contents += linkLabel
  
  def contentAction(proxy : ISamplingDataProxyUI)  = new ContentAction(proxy.dataUI.name,proxy){
    override def apply = TopComponentsManager.currentMoleSceneTopComponent.get.getMoleScene.displayExtraPropertyPanel(proxy)}

  override def saveContent(name: String) = 
    new ExplorationTaskDataUI(name , Some(samplingComboBox.selection.item))
  
  def comboContent: List[ISamplingDataProxyUI] = emptyProxy :: Proxys.samplings.toList
  
  def emptyProxy = new SamplingDataProxyUI(new EmptySamplingDataUI)
}
