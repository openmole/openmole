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

package org.openmole.ide.core.display

import scala.swing._
import swing.Swing._
import swing.ListView._
import javax.swing.JPanel
import org.openmole.ide.core.MoleSceneTopComponent
import org.openmole.ide.core.palette.IDataProxyFactory
import org.openmole.ide.core.display._
import scala.swing.event.ButtonClicked
import org.openmole.ide.core.commons.Constants._

class PropertyPanel extends FlowPanel{
  border = Swing.EmptyBorder(10, 10, 10, 10)
  var initEntity = false
  var oldName = ""
  Displays.setAsTask
  
  
  val prototype = buildButton("Prototype")
  val task = buildButton("Task")
  val sampling = buildButton("Sampling")
  val environment = buildButton("Environment")
  val saveButton = buildButton("Save")
  val cancelButton = buildButton("Cancel")
  val nameTextField = new TextField {
    border = Swing.EmptyBorder(5, 5, 5, 5)
    //preferredSize = new Dimension(150,25)
  }
  val taskComboBox = buildCombo(TaskDisplay.implementationClasses.toList)
  val prototypeComboBox = buildCombo(PrototypeDisplay.implementationClasses.toList) 
  val samplingComboBox = buildCombo(SamplingDisplay.implementationClasses.toList) 
  val environmentComboBox = buildCombo(EnvironmentDisplay.implementationClasses.toList) 
  
  listenTo(`prototype`,`task`,`sampling`,`environment`,`saveButton`,`cancelButton`)
  
  val categoryButtonPanel = new BoxPanel(Orientation.Horizontal) {
    contents.append(task,prototype,sampling,environment)
    border = Swing.EmptyBorder(15, 5, 15, 5)
  }
  
  reactions += {
    case ButtonClicked(`prototype`) =>  {Displays.setAsPrototype; initNewEntity}
    case ButtonClicked(`task`) =>  {Displays.setAsTask; initNewEntity}
    case ButtonClicked(`sampling`) =>  {Displays.setAsSampling; initNewEntity}
    case ButtonClicked(`environment`) =>  {Displays.setAsEnvironment; initNewEntity}
    case ButtonClicked(`saveButton`) =>  save
    case ButtonClicked(`cancelButton`) =>  cancel
  }

  val namePanel = new BoxPanel(Orientation.Horizontal) {
    contents.append(buildLabel("Name :"),nameTextField,saveButton)
    border = Swing.EmptyBorder(10, 5, 10, 5)
  }
  
  val typePanel = new BoxPanel(Orientation.Horizontal) {
    contents.append(buildLabel("Type :"),taskComboBox,cancelButton)
    border = Swing.EmptyBorder(10, 5, 10, 5)
  }
  
  val fixedPanel = new BoxPanel(Orientation.Vertical) {contents.append(categoryButtonPanel,namePanel,typePanel)}
    
  val propertyScrollPane = new ScrollPane{minimumSize = new Dimension(150,200)}
  
  contents.append(fixedPanel,propertyScrollPane)
  border = Swing.EmptyBorder(10, 10, 10, 10)
  
  def buildCombo(l: List[IDataProxyFactory]) = new ComboBox(l) { renderer = Renderer(_.factory.displayName) }
  
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
  
  def hidePanelScrollPane = propertyScrollPane.visible = false
  
  def displayCurrentEntity = {
    typePanel.visible = false
    propertyScrollPane.visible = true
    initEntity = false
    oldName = Displays.name
    nameTextField.text = Displays.dataProxy.dataUI.name
    updateViewport(Displays.buildPanelUI.peer)
    repaint
  }
  
  def updateViewport(panel: JPanel)= {
    propertyScrollPane.peer.getViewport.removeAll
    propertyScrollPane.peer.setViewportView(panel)
  }
  
  def save = {
    if (initEntity) {
      if (nameTextField.text.length != 0) {
        Displays.setAsName(nameTextField.text)
        combo.selection.item.buildDataProxyUI(nameTextField.text)
        displayCurrentEntity
      }
    } else if (oldName != "") {
      Displays.saveContent(oldName, nameTextField.text)
      oldName = nameTextField.text
    }
    MoleSceneTopComponent.getDefault.refreshPalette
    initEntity = false
  }
  
  def cancel = {
    if (initEntity) nameTextField.text = ""
    else displayCurrentEntity
  }
  
  def initNewEntity = {
    typePanel.visible = true
    typePanel.contents.update(1,combo) 
    typePanel.repaint
    Displays.increment
    nameTextField.text = Displays.name
    propertyScrollPane.visible = false
    initEntity = true
  }
  
  def combo = {
    Displays.currentType match {
      case TASK=> taskComboBox
      case PROTOTYPE=> prototypeComboBox
      case SAMPLING=> samplingComboBox
      case ENVIRONMENT=> environmentComboBox
    }
  }
}