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
import org.openmole.ide.misc.widget.MigPanel
import scala.swing._
import org.openmole.ide.core.implementation.action._
import org.openmole.ide.core.implementation.display._
import scala.swing.ToggleButton
import scala.swing.event.ButtonClicked
import org.openmole.ide.core.implementation.palette.PaletteSupport
import org.openmole.ide.misc.widget.MenuToggleButton
import org.openide.util.ImageUtilities
import org.openmole.ide.core.model.commons.Constants._

class PropertyPanel extends MigPanel("fillx,wrap 4","[grow,fill]", "[]30[]rel[grow,fill]"){
  Displays.currentType = TASK
  
  val domainMenu = new Menu("Domain")
  DomainDisplay.implementationClasses.foreach(d=>domainMenu.contents+=new MenuItem(new DomainDisplayAction(d,DOMAIN)))
  val environmentMenu = new Menu("Environment")
  EnvironmentDisplay.implementationClasses.foreach(d=>environmentMenu.contents+=new MenuItem(new EnvironmentDisplayAction(d,ENVIRONMENT)))
  val taskMenu = new Menu("Task")
  TaskDisplay.implementationClasses.foreach(d=>taskMenu.contents+=new MenuItem(new TaskDisplayAction(d,TASK)))
  val prototypeMenu = new Menu("Prototype")
  PrototypeDisplay.implementationClasses.foreach(d=>prototypeMenu.contents+= new MenuItem(new PrototypeDisplayAction(d,PROTOTYPE)))
  val samplingMenu = new Menu("Sampling")
  SamplingDisplay.implementationClasses.foreach(d=>samplingMenu.contents+= new MenuItem(new SamplingDisplayAction(d,SAMPLING)))
    
  val saveButton = new Button("Apply")
  val cancelButton = new Button("Cancel")
  val nameTextField = new TextField(12) 
  
  listenTo(`saveButton`,`cancelButton`)
  reactions += {
    case ButtonClicked(`saveButton`) =>  save
    case ButtonClicked(`cancelButton`) =>  cancel}

  val propertyScrollPane = new ScrollPane{minimumSize = new Dimension(150,20)}
  
  contents+= (new MenuBar{contents.append(prototypeMenu,taskMenu,domainMenu,samplingMenu,environmentMenu)},"span 4,growx")
  contents+= new Label("Name")
  contents+= nameTextField
  contents+= saveButton
  contents+= cancelButton
  contents+= (propertyScrollPane,"span 4,growx")
  
  
  def displayCurrentTypeIcon = {
    contents.remove(1) 
    contents.insert(1,new MenuToggleButton(
        Some(new ImageIcon(ImageUtilities.loadImage(Displays.dataProxy.get.dataUI.imagePath))))
                    {popup= Displays.managementMenu})}
  
  def displayCurrentEntity = {
    nameTextField.text = Displays.name
    displayCurrentTypeIcon
    updateViewport(Displays.buildPanelUI.peer)
    repaint
  }
  
  def cleanViewport = {
    removeViewport
    PaletteSupport.refreshPalette
    nameTextField.text = ""
    repaint
  }
  
  def removeViewport = propertyScrollPane.peer.getViewport.removeAll
  
  def updateViewport(panel: JPanel)= {
    removeViewport
    propertyScrollPane.peer.setViewportView(panel)
  }
  
  def save = {
    Displays.saveContent(nameTextField.text)
    PaletteSupport.refreshPalette
    Displays.initMode = false
    displayCurrentTypeIcon
  }
  
  def cancel = displayCurrentEntity
  
  def initNewEntity = {
    Displays.initMode = true
    nameTextField.text = ""
    updateViewport(Displays.buildPanelUI.peer)
  }
}