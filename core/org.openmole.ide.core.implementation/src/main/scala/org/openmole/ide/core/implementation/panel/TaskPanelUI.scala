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
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget.ContentAction
import org.openmole.ide.misc.widget.ImplicitLinkLabel
import org.openmole.ide.misc.widget.ImageLinkLabel
import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabel
import org.openmole.ide.misc.widget.multirow.MultiComboLinkLabelGroovyTextFieldEditor
import scala.swing.Action
import scala.swing.Label
import scala.swing.Separator
import scala.collection.JavaConversions._
import org.openmole.ide.misc.tools.image.Images._
import BasePanelUI._

class TaskPanelUI(proxy: ITaskDataProxyUI,
                  scene: IMoleScene,
                  mode: Value = CREATION) extends BasePanelUI(proxy, scene,mode,proxy.dataUI.borderColor){
  iconLabel.icon = new ImageIcon(ImageIO.read(proxy.dataUI.getClass.getClassLoader.getResource(proxy.dataUI.fatImagePath)))
  
  val panelUI = proxy.dataUI.buildPanelUI
  val protoPanel = new IOPrototypePanel
  mode match {
    case IO => protos
    case _=> properties
  }
  
  def create = {
    Proxys.tasks += proxy
    ConceptMenu.taskMenu.popup.contents += ConceptMenu.addItem(nameTextField.text, proxy)
  }
  
  def delete = {
    val toBeRemovedCapsules : List[ICapsuleUI] = ScenesManager.moleScenes.map{_.manager.capsules.values.filter{_.dataUI.task == Some(proxy)}}.flatten.toList
    toBeRemovedCapsules match {
      case Nil => 
        scene.closePropertyPanel
        Proxys.tasks -= proxy
        ConceptMenu.removeItem(proxy)
      case _ => 
        if (DialogFactory.deleteProxyConfirmation(proxy)) {
          toBeRemovedCapsules.foreach{c=>c.scene.graphScene.removeNodeWithEdges(c.scene.manager.removeCapsuleUI(c))}
          delete
        }
    }
  }
  
  def save = {
    proxy.dataUI = panelUI.save(nameTextField.text,protoPanel.protoInEditor.content,protoPanel.protoOutEditor.content)
  
    ScenesManager.capsules(proxy).foreach {c =>
      proxy.dataUI match {
        case x : IExplorationTaskDataUI => c.setSampling(x.sampling)
        case _ => 
      }
    }
  }
  
  def switch = {
    save
    if(mainPanel.contents.size == 2) mainPanel.contents.remove(1)
    if(mainLinksPanel.contents.size == 2) mainLinksPanel.contents.remove(1)
    ScenesManager.currentSceneContainer match {
      case Some(x : ISceneContainer) => x.scene.closeExtraPropertyPanel
      case None =>
    }
  }
  
  def properties = {
    switch    
    mainPanel.contents += panelUI.peer
    mainLinksPanel.contents +=  new ImageLinkLabel(NEXT,new Action("") { def apply = protos })
    revalidate
    repaint
  }
  
  def protos : Unit = {
    switch
    mainPanel.contents += protoPanel.peer
    mainLinksPanel.contents +=  new ImageLinkLabel(PREVIOUS,new Action("") { def apply = properties })
  }
  
  class IOPrototypePanel extends MyPanel{
    peer.setLayout(new BorderLayout)
    val image = EYE
      
    val emptyProto = EmptyDataUIs.emptyPrototypeProxy
    
    val protoInEditor = new MultiComboLinkLabelGroovyTextFieldEditor("",
                                                                     TaskPanelUI.this.proxy.dataUI.prototypesIn.map{case(proto,v) =>
          (proto,proto.dataUI.coreObject,contentAction(proto),v)}.toList,
                                                                     (List(emptyProto):::(Proxys.prototypes.toList.filterNot{p=>proxy.dataUI.implicitPrototypesIn.map{
              _.dataUI.name}.contains(p.dataUI.name)
          })).map{
        p=>(p,p.dataUI.coreObject,contentAction(p))
      }.toList,
                                                                     image)
    val protoOutEditor = new MultiComboLinkLabel("",
                                                 TaskPanelUI.this.proxy.dataUI.prototypesOut.map{proto => (proto,contentAction(proto))}.toList,
                                                 (List(emptyProto):::(Proxys.prototypes.toList.filterNot{p=>proxy.dataUI.implicitPrototypesOut.map{
              _.dataUI.name}.contains(p.dataUI.name)
          })).map{
        p=>(p,contentAction(p))}.toList,
                                                 image)

    val protoIn = new PluginPanel("wrap"){
      contents += new Label("Inputs") {foreground = Color.WHITE}
      contents += new PluginPanel("wrap"){
        TaskPanelUI.this.proxy.dataUI.implicitPrototypesIn.foreach{p=> 
          contents += new ImplicitLinkLabel(p.dataUI.name,contentAction(p))
        }
      }
      contents += protoInEditor.panel    
    }
                                                                      
    val protoOut =   new PluginPanel("wrap"){
      contents += new Label("Outputs") {foreground = Color.WHITE}
      contents += new PluginPanel("wrap"){  
        TaskPanelUI.this.proxy.dataUI.implicitPrototypesOut.foreach{p=> 
          contents += new ImplicitLinkLabel(p.dataUI.name,contentAction(p))
        }
      }
      contents += protoOutEditor.panel    
    }    
    
    if (TaskPanelUI.this.proxy.dataUI.prototypesIn.isEmpty) protoInEditor.removeAllRows
    if (TaskPanelUI.this.proxy.dataUI.prototypesOut.isEmpty) protoOutEditor.removeAllRows
    
    peer.add(protoIn.peer,BorderLayout.WEST)
    peer.add((new Separator).peer)
    peer.add(protoOut.peer,BorderLayout.EAST)
  
    def contentAction(proto : IPrototypeDataProxyUI) = new ContentAction(proto.dataUI.displayName,proto){
      override def apply = 
        ScenesManager.currentSceneContainer match {
          case Some(x : ISceneContainer) => x.scene.displayExtraPropertyPanel(proto)
          case None =>
        }
    }
  }
}
