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
import javax.swing.ImageIcon
import org.openide.awt.StatusDisplayer
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.IPanelUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.MainLinkLabel
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor
import scala.swing.Action
import scala.swing.Panel
import scala.swing.Separator

class TaskPanelUI(proxy: ITaskDataProxyUI,
                  scene: IMoleScene,
                  mode: Value = CREATION) extends BasePanelUI(proxy, scene,mode,proxy.dataUI.borderColor){
  iconLabel.icon = new ImageIcon(ImageTool.loadImage(proxy.dataUI.fatImagePath,50,50))
  var panelUI = proxy.dataUI.buildPanelUI
  var protoPanel: Option[IOPrototypePanel] = None
  preferredSize.width = 300
  //mainLinksPanel.contents += new Label("                        ")
  var propertyMode = true
  properties
  
  def create = {
    Proxys.tasks += proxy
    ConceptMenu.taskMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }
  
  def delete = {
    Proxys.tasks -= proxy
    ConceptMenu.removeItem(proxy)
  }
  
  def save = {
    propertyMode match {
      case true=> 
      case false=> protoPanel.get.save
    }
    println("save :: " + nameTextField.text)
    proxy.dataUI = panelUI.saveContent(nameTextField.text)
  }
  
  def switch = {
    save
    if(mainPanel.contents.size == 2) mainPanel.contents.remove(1)
    if(mainLinksPanel.contents.size == 2) mainLinksPanel.contents.remove(1)
    TopComponentsManager.currentMoleSceneTopComponent.get.getMoleScene.closeExtraProperty
  }
  
  def properties = {
    propertyMode = true
    switch
    panelUI = proxy.dataUI.buildPanelUI
    mainPanel.contents += panelUI.peer
    mainLinksPanel.contents +=  new MainLinkLabel("",new Action("") { def apply = protos }) {
      icon = new ImageIcon(ImageTool.loadImage("img/next.png",20,20))}
    revalidate
    repaint
  }
  
  def protos : Unit = {
    if (Proxys.prototypes.isEmpty) StatusDisplayer.getDefault().setStatusText("Prototypes can not be set to this task since no one is defined yet")
    else {
      protoPanel = Some(new IOPrototypePanel(proxy))
      propertyMode = false
      switch
      mainPanel.contents += protoPanel.get.peer
    
      mainLinksPanel.contents +=  new MainLinkLabel("",new Action("") { def apply = properties }) {
        icon = new ImageIcon(ImageTool.loadImage("img/previous.png",20,20))}
      revalidate
      repaint
    }
  }
  
  class IOPrototypePanel(taskproxy: ITaskDataProxyUI) extends Panel{
    peer.setLayout(new BorderLayout)
    //val action = new Action("") { override def apply = println("view")}
    val image = new ImageIcon(ImageTool.loadImage("img/eye.png",20,20))
    val protoIn = new MultiComboLinkLabelGroovyTextFieldEditor("Inputs",
                                                               proxy.dataUI.prototypesIn.map{case(proto,v) => (proto,contentAction(proto),v)}.toList,
                                                               Proxys.prototypes.map{p=>(p,contentAction(p))}.toList,
                                                               image)        
                                                                      
    val protoOut = new MultiComboLinkLabel("Outputs",
                                           proxy.dataUI.prototypesOut.map{proto => (proto,contentAction(proto))}.toList,
                                           Proxys.prototypes.map{p=>(p,contentAction(p))}.toList,
                                           image)        
    
    peer.add(protoIn.panel.peer,BorderLayout.WEST)
    peer.add((new Separator).peer)
    peer.add(protoOut.panel.peer,BorderLayout.EAST)
  
    def contentAction(proto : IPrototypeDataProxyUI) = new ContentAction(proto.dataUI.displayName,proto){
      override def apply = TopComponentsManager.currentMoleSceneTopComponent.get.getMoleScene.displayExtraProperty(proto)}
    
    def save = {
      taskproxy.dataUI.prototypesOut.clear
      taskproxy.dataUI.prototypesIn.clear
      protoOut.content.foreach(taskproxy.dataUI.prototypesOut+=) 
      protoIn.content.foreach(taskproxy.dataUI.prototypesIn+= )
    }
  }
}
