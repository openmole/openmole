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

import scala.collection.mutable.HashSet
import scala.swing._
import swing.Swing._
import swing.ListView._
import javax.swing.JPanel
import scala.swing.MenuItem
import org.openide.windows.WindowManager
import org.openmole.ide.core.implementation.MoleSceneTopComponent
import org.openmole.ide.core.model.dataproxy.IDataProxyFactory
import org.openmole.ide.core.implementation.action._
import org.openmole.ide.core.implementation.display._
import scala.swing.event.ButtonClicked
import org.openmole.ide.core.model.commons.Constants._

class PropertyPanel extends BoxPanel(Orientation.Vertical){
  border = Swing.EmptyBorder(10, 10, 10, 10)
  var initEntity = false
  var oldName = ""
  Displays.currentType = TASK
  
  
  val environmentToggleButton = new MenuToggleButton2("Environment")
  EnvironmentDisplay.implementationClasses.foreach(d=>environmentToggleButton.addItem(new MenuItem(new EnvironmentDisplayAction(d,ENVIRONMENT))))
  val taskToggleButton = new MenuToggleButton2("Task")
  TaskDisplay.implementationClasses.foreach(d=>taskToggleButton.addItem(new MenuItem(new TaskDisplayAction(d,TASK))))
  val prototypeToggleButton = new MenuToggleButton2("Prototype")
  PrototypeDisplay.implementationClasses.foreach(d=>prototypeToggleButton.addItem(new MenuItem(new PrototypeDisplayAction(d,PROTOTYPE))))
  val samplingToggleButton = new MenuToggleButton2("Sampling")
  SamplingDisplay.implementationClasses.foreach(d=>samplingToggleButton.addItem(new MenuItem(new SamplingDisplayAction(d,SAMPLING))))
    
  val saveButton = buildButton("Save")
  val cancelButton = buildButton("Cancel")
  val nameTextField = new TextField {
    maximumSize = new Dimension(150,30)
  }
  
  listenTo(`saveButton`,`cancelButton`)
  
  val categoryButtonPanel = new BoxPanel(Orientation.Horizontal) {
    contents.append(prototypeToggleButton,taskToggleButton,samplingToggleButton,environmentToggleButton)
    border = Swing.EmptyBorder(15, 5, 15, 5)
  }
  
  reactions += {
    case ButtonClicked(`saveButton`) =>  save
    case ButtonClicked(`cancelButton`) =>  cancel
  }

  val namePanel = new BoxPanel(Orientation.Horizontal) {
    contents.append(buildLabel("Name :"),nameTextField,saveButton,cancelButton)
    border = Swing.EmptyBorder(10, 5, 10, 5)
  }
    
  val propertyScrollPane = new ScrollPane{minimumSize = new Dimension(150,200)}
  
  contents.append(categoryButtonPanel,namePanel,propertyScrollPane)
  border = Swing.EmptyBorder(10, 10, 10, 10)
  
  def buildButton(name: String) = 
    new Button(name){
      minimumSize = new Dimension(100,25)
      border = Swing.EmptyBorder(5,15,5,15)
    }

  def buildLabel(name: String) = 
    new Label(name) {
      preferredSize = new Dimension(60,25)
      border = Swing.EmptyBorder(5,5,5,5)
    }
  
  def displayCurrentEntity = {
    oldName = Displays.name
    nameTextField.text = Displays.name
    updateViewport(Displays.buildPanelUI.peer)
    repaint
  }
  
  def cleanViewport = propertyScrollPane.peer.getViewport.removeAll
  
  def updateViewport(panel: JPanel)= {
    cleanViewport
    propertyScrollPane.peer.setViewportView(panel)
  }
  
  def save = {
    Displays.saveContent(oldName, nameTextField.text)
    WindowManager.getDefault().findTopComponent("MoleSceneTopComponent").asInstanceOf[MoleSceneTopComponent].refreshPalette
  }
  
  def cancel = displayCurrentEntity
  
  def initNewEntity = {
    Displays.increment
    nameTextField.text = Displays.name
    oldName = Displays.name
    updateViewport(Displays.buildPanelUI.peer)
  }
}