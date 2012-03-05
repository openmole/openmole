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
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing._
import scala.swing.event.SelectionChanged
import swing.Swing._
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.dataproxy.ISamplingDataProxyUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openide.awt.StatusDisplayer

class ExplorationTaskPanelUI (pud: ExplorationTaskDataUI) extends PluginPanel("wrap 3") with ITaskPanelUI {
  val samplingComboBox = new ComboBox(Proxys.samplings.toList) 
  {tooltip = Help.tooltip("The name of the sampling to be executed")}

  contents += new Label("Sampling")
  contents += samplingComboBox
  val linkLabel : LinkLabel = new LinkLabel("",viewAction(pud.sampling)) {
    icon = new ImageIcon(ImageTool.loadImage("img/eye.png",20,20))
  }
    
  listenTo(`samplingComboBox`)
  samplingComboBox.selection.reactions += {
    case SelectionChanged(`samplingComboBox`)=> 
      val proxy = Proxys.samplings.filter{s => s == samplingComboBox.selection.item}.head
      linkLabel.action = contentAction(proxy) 
  }
  contents += linkLabel
    
  def viewAction(proxy : Option[ISamplingDataProxyUI]) = proxy match {
    case Some(x: ISamplingDataProxyUI)=> contentAction(x)
    case _=> new ContentAction("",Unit){override def apply = StatusDisplayer.getDefault.setStatusText("No sampling to be displayed")}
  }
  
  def contentAction(proxy : ISamplingDataProxyUI)  = new ContentAction(proxy.dataUI.name,proxy){
    override def apply = TopComponentsManager.currentMoleSceneTopComponent.get.getMoleScene.displayExtraProperty(proxy)}

  override def saveContent(name: String) = new ExplorationTaskDataUI(name,Some(samplingComboBox.selection.item))
}
