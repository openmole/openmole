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

package org.openmole.ide.core.implementation.workflow

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import java.awt.Rectangle
import javax.swing.border.LineBorder
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.action.SelectProvider
import org.netbeans.api.visual.action.WidgetAction
import org.netbeans.api.visual.action.WidgetAction._
import org.netbeans.api.visual.anchor.AnchorShapeFactory
import org.netbeans.api.visual.layout.LayoutFactory
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.panel.ConceptMenu
import org.openmole.ide.core.model.commons.Constants
import org.openmole.ide.core.model.workflow.IDataChannelUI
import org.openmole.ide.core.implementation.dialog.DataChannelDialog
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.widget._
import scala.swing.Action
import scala.swing.TextArea
import scala.swing.event.MousePressed

class DataChannelConnectionWidget(scene: IMoleScene, val dataChannelUI: IDataChannelUI) extends ConnectionWidget(scene.graphScene){
  
  setLineColor(new Color(188,188,188))
  setStroke(new BasicStroke(6, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,20.0f, List(10.0f).toArray, 0.0f))
  //getActions.addAction(ActionFactory.createSelectAction(new ObjectSelectProvider))
  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  
  setSourceAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.IMAGE_OUTPUT_DATA_CHANNEL,false))
  setTargetAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.IMAGE_OUTPUT_DATA_CHANNEL,false))
  var labeled = false
  
  var protoPanel = new PrototypePanel
  val componentWidget = new ComponentWidget(scene.graphScene,protoPanel.peer)
  setConstraint(componentWidget, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
  componentWidget.setOpaque(true)
  addChild(componentWidget)
  scene.refresh
//  val textArea = new PrototypeTextArea
//  dataChannelUI.prototypes.isEmpty match {
//    case false=>
//      addPrototypeLabel
//      setPrototypes
//    case true=>
//  }
  
  def addPrototypeLabel = {
    labeled match {
      case false =>
//        val componentWidget = new ComponentWidget(scene.graphScene,textArea.peer)
//        setConstraint(componentWidget, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
//        componentWidget.setOpaque(true)
//        addChild(componentWidget)
//        scene.refresh
        labeled = true
      case true=>
    }
    DataChannelDialog.display(DataChannelConnectionWidget.this)
  }
  
  def setPrototypes = {
    var t = ""
//    dataChannelUI.prototypes.foreach{p=>t+=p.dataUI.name}
//    textArea.text = t
//    textArea.revalidate
    scene.refresh
  }
  
  def edit = {
    println("edit ... ")
    DataChannelDialog.display(DataChannelConnectionWidget.this)
    protoPanel = new PrototypePanel
    revalidate
    repaint
    scene.refresh
  }
  
//  class ObjectSelectProvider extends SelectProvider {
//        
//    override def isAimingAllowed(w: Widget,localLocation: Point,invertSelection: Boolean) = false
//                
//    override def isSelectionAllowed(w: Widget,localLocation: Point,invertSelection: Boolean) = true
//        
//    override def select(w: Widget,localLocation: Point,invertSelection: Boolean) = DataChannelConnectionWidget.this.edit
//  }
  
  class PrototypePanel extends MigPanel("wrap"){
    contents+= (new MainLinkLabel("edit",new Action("") { def apply = DataChannelConnectionWidget.this.edit}),"align right")
    
    background = Constants.DATA_CHANNEL_LABEL_BACKGROUND_COLOR
    border = new LineBorder(Constants.DATA_CHANNEL_LABEL_BORDER_COLOR,3)
    
    val subPanel = new MigPanel("wrap","align center",""){
      background = Constants.DATA_CHANNEL_LABEL_BACKGROUND_COLOR
      border = new LineBorder(Constants.DATA_CHANNEL_LABEL_BORDER_COLOR,3)
      preferredSize = new Dimension(80,DataChannelConnectionWidget.this.dataChannelUI.prototypes.size*20)
    }
    DataChannelConnectionWidget.this.dataChannelUI.prototypes.foreach{p=>
      subPanel.contents += new LinkLabel(p.dataUI.displayName,new Action("") { def apply = ConceptMenu.display(p,EDIT)})
    }
    if (contents.size == 2) contents.remove(1)
    contents += subPanel
    
    preferredSize = new Dimension(80,DataChannelConnectionWidget.this.dataChannelUI.prototypes.size*20 + 30)
  }
  
  
  
//  class PrototypeTextArea extends TextArea {
//    foreground = Constants.CONNECTOR_LABEL_FONT_COLOR
//    background = Constants.CONNECTOR_LABEL_BACKGROUND_COLOR
//    border = new LineBorder(Constants.CONNECTOR_LABEL_BORDER_COLOR,3)
//    preferredSize = new Dimension(80,30)
//    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
//  
//    listenTo(mouse.clicks)
//    reactions += {
//      case e: MousePressed => DataChannelConnectionWidget.this.edit
//    }
//  }
}
