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

package org.openmole.ide.core.implementation.display

import scala.swing._
import swing.Swing._
import swing.ListView._
import javax.swing.ImageIcon
import javax.swing.JPanel
import org.openmole.ide.core.model.panel.IPanelUI
//import org.openmole.ide.misc.image._
import org.openmole.ide.misc.widget.MigPanel
import scala.swing._
import org.openmole.ide.core.model.commons.IOType
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.implementation.action._
import org.openmole.ide.core.implementation.display._
import scala.swing.event.ButtonClicked
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.misc.widget.MenuToggleButton
import org.openmole.ide.misc.widget.PopupMenu
import org.openmole.ide.core.model.commons.Constants._

class PropertyPanel extends MigPanel("fillx,wrap 4","[][grow,fill][][]", "[fill]30[]rel[grow,fill]rel[grow,fill]"){
//  Displays.currentType = TASK
//  var editable = true
//    
////  val environmentMenu = new Menu("Environment")
////  EnvironmentDisplay.implementationClasses.toList.sortBy(_.factory.displayName).foreach(
////    d => environmentMenu.contents += new MenuItem(new EnvironmentDisplayAction(d, ENVIRONMENT))
////  )
//  
////  val taskMenu = new Menu("Task")
////  TaskDisplay.implementationClasses.toList.sortBy(_.factory.displayName).foreach(
////    d => taskMenu.contents += new MenuItem(new TaskDisplayAction(d, TASK))
////  )
////  val prototypeMenu = new Menu("Prototype")
////  PrototypeDisplay.implementationClasses.toList.sortBy(_.factory.displayName).foreach(
////    d => prototypeMenu.contents += new MenuItem(new PrototypeDisplayAction(d, PROTOTYPE))
////  )
////  
////  val samplingMenu = new Menu("Sampling")
////  SamplingDisplay.implementationClasses.toList.sortBy(_.factory.displayName).foreach(
////    d => samplingMenu.contents += new MenuItem(new SamplingDisplayAction(d, SAMPLING))
////  )
//    
//  val fakeToggleButton = new MenuToggleButton(Some(new ImageIcon(ImageTool.loadImage("img/empty.png",30,30))))
//  val saveButton = new Button("Apply")
//  val cancelButton = new Button("Cancel")
//  val nameTextField = new TextField(15) 
////  val menuBar = new MenuBar{contents.append(prototypeMenu,taskMenu,samplingMenu,environmentMenu)}
// // menuBar.minimumSize = new Dimension(size.width,30)
//  
//  listenTo(saveButton,cancelButton)
//  reactions += {
//    case ButtonClicked(`saveButton`) =>  save
//    case ButtonClicked(`cancelButton`) =>  cancel}
//
//  val propertyScrollPane = new ScrollPane{verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded}
//  var taskPropertyComponent = new ScrollPane{verticalScrollBarPolicy = ScrollPane.BarPolicy.Never}
// // contents+= (menuBar,"span 4, growx")
//  contents+= fakeToggleButton
//  contents+= nameTextField
//  contents+= saveButton
//  contents+= cancelButton
//  
//  contents+= (propertyScrollPane,"span 4,growx, growy")
//  contents+= (taskPropertyComponent,"span 4,growx")
//  
//  def displayCurrentTypeIcon: Unit = displayCurrentTypeIcon(Displays.dataProxy.get)
//  
//  def displayCurrentTypeIcon(p: IDataProxyUI):Unit = {
////    contents.remove(1) 
////    contents.insert(1,new MenuToggleButton(
////        Some(new ImageIcon(ImageTool.loadImage(p.dataUI.imagePath,30,30))))
////      {popup= Displays.firstManagementMenu})
////    revalidate
//  }
//  
//  def displayCurrentEntity: Unit = displayCurrentEntity(Displays.dataProxy.get)
//  
//  def displayCurrentEntity(p: IDataProxyUI): Unit = {
////    nameTextField.text = p.dataUI.name
////    val pui = Displays.buildPanelUI
////    updateViewport(pui.peer)
////    displayCurrentTypeIcon(p)
////    
////    pui.peer.getComponents.foreach(_.setEnabled(editable))
////    pui.peer.getComponents.foreach(_.setEnabled(editable))
////    peer.getComponents.foreach(_.setEnabled(editable))
////    menuBar.peer.getComponents.foreach(_.setVisible(editable))
//  }
//  
//  def cleanViewport = {
////    removeViewport
////    nameTextField.text = ""
////    repaint
////    revalidate
//  }
//  
//  def removeViewport = {
////    propertyScrollPane.peer.getViewport.removeAll
////    contents.remove(1) 
////    contents.insert(1,fakeToggleButton)
//    }
//    
//  
//  def updateViewport(panel: JPanel)= {
////    removeViewport
////    updateTaskViewport
////    propertyScrollPane.peer.setViewportView(panel)
////    revalidate
//  }
//  
//  def updateTaskViewport = {
////    taskPropertyComponent.peer.getViewport.removeAll
////    Displays.dataProxy match {
////      case Some(x: ITaskDataProxyUI)=> {
////          taskPropertyComponent.peer.setViewportView(buildTaskPropertyPanel(x).peer)
////          taskPropertyComponent.visible =  true
////        }
////      case _=> taskPropertyComponent.visible = false
////    }
////    taskPropertyComponent.revalidate
//  }
//  
//  def save = {
//    Displays.saveContent
//    Displays.initMode = false
//  }
//  
//  def cancel = displayCurrentEntity
//  
//  def initNewEntity = {
//    Displays.initMode = true
//    nameTextField.text = ""
//   // updateViewport(Displays.buildPanelUI.peer)
//    displayCurrentTypeIcon
//  }
//  
// 
//  def buildTaskPropertyPanel(dpu: ITaskDataProxyUI) = {
////    var empty = true
////    val mp = new MigPanel("wrap 4","[]5[]","[]5[]") {
////      if (dpu.dataUI.prototypesIn.size>0){
////        empty= false
////        contents+= new Label("In")
////        contents+= (new MigPanel(""){
////            dpu.dataUI.prototypesIn.foreach(p=> contents+= 
////                                            secondManagementMenu(p.dataUI.imagePath,p.dataUI.name,PrototypeDisplay.secondManagementMenu(dpu,p,IOType.INPUT)))},"wrap")}
////      if (dpu.dataUI.prototypesOut.size>0){
////        empty= false
////        contents+= new Label("Out")
////        contents+= (new MigPanel(""){
////            dpu.dataUI.prototypesOut.foreach(p=> contents+= 
////                                             secondManagementMenu(p.dataUI.imagePath,p.dataUI.name,PrototypeDisplay.secondManagementMenu(dpu,p,IOType.OUTPUT)))},"wrap")}
////      if (dpu.dataUI.sampling.isDefined) {
////        empty= false
////        contents+= new Label("Sampling")
////        contents+= new MigPanel(""){contents+= secondManagementMenu(dpu.dataUI.sampling.get.dataUI.imagePath,
////                                                                    dpu.dataUI.sampling.get.dataUI.name,
////                                                                    SamplingDisplay.secondManagementMenu(dpu,dpu.dataUI.sampling.get))}}
//////      if (dpu.dataUI.environment.isDefined) {
//////        empty= false
//////        contents+= new Label("Environment")
//////        contents+= new MigPanel(""){contents+= secondManagementMenu(dpu.dataUI.environment.get.dataUI.imagePath,
//////                                                                    dpu.dataUI.environment.get.dataUI.name,
//////                                                                    EnvironmentDisplay.secondManagementMenu(dpu,dpu.dataUI.environment.get))}}
////    }
////    taskPropertyComponent.visible = !empty
////    if (empty) taskPropertyComponent.size.height = 0
////    mp.peer.getComponents.foreach(_.setEnabled(editable))
////    mp
//  }
//     
////  def secondManagementMenu(impath: String, displayname: String,p: PopupMenu) = new MenuToggleButton(
////    Some(new ImageIcon(ImageTool.loadImage(impath,30,30))),displayname){popup= p; popup.peer.getComponents.foreach(_.setEnabled(editable))}
}
