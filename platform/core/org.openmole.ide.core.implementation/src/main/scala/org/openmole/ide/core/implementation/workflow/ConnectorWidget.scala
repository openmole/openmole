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
package org.openmole.ide.core.implementation.workflow

import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.border.LineBorder
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.anchor.AnchorShapeFactory
import org.netbeans.api.visual.border.BorderFactory
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.LabelWidget
import org.netbeans.api.visual.widget.Scene
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.model.commons.Constants
import org.netbeans.api.visual.layout.LayoutFactory
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.workflow.ITransitionUI
import org.openmole.ide.core.model.commons.TransitionType._
import scala.swing.Label
import scala.swing.event.MousePressed

class ConnectorWidget(val scene: IMoleScene,val transition: ITransitionUI) extends ConnectionWidget(scene.graphScene){
  
  val label = new ConnectorLabel
  transition.condition match {
    case Some(x:String)=>
      addConditionLabel
      setConditionLabel(transition.condition)
    case None=>
  }
  drawTransitionType
  
  def addConditionLabel = {
    val componentWidget = new ComponentWidget(scene.graphScene,label.peer)
    setConstraint(componentWidget, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
    componentWidget.setOpaque(true)
    addChild(componentWidget)
    scene.refresh
    label.edit
  }
  
  def setConditionLabel(cond: Option[String])= {
    label.text = cond.getOrElse("")
    setLabelVisible
  }
  
  def setLabelVisible= {
    label.visible = !label.text.isEmpty 
    label.revalidate
    scene.refresh
  }
  
  def drawTransitionType = {
    transition.transitionType match {
      case EXPLORATION_TRANSITION=> setSourceAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.EXPLORATION_TRANSITON,false))
      case AGGREGATION_TRANSITION=> setTargetAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.AGGREGATION_TRANSITON,false))
      case _=> setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)
    }
  }
  
  class ConnectorLabel extends Label {
    foreground = Constants.CONNECTOR_LABEL_FONT_COLOR
    background = Constants.CONNECTOR_LABEL_BACKGROUND_COLOR
    border = new LineBorder(Constants.CONNECTOR_LABEL_BORDER_COLOR,3)
    preferredSize = new Dimension(80,30)
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  
    listenTo(mouse.clicks)
    reactions += {
      case e: MousePressed => edit
    }
    
    def edit = {
        text = DialogFactory.groovyEditor(text)
        revalidate
    }
  }
  
}
  
