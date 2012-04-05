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
import java.awt.BasicStroke
import java.awt.Rectangle
import java.awt.RenderingHints
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.anchor.AnchorShapeFactory
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.layout.LayoutFactory
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.workflow.ITransitionUI
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.misc.widget.dialog.DialogFactory
import scala.swing.Label
import scala.swing.event.MousePressed
import org.openmole.ide.misc.tools.image.Images._

class ConnectorWidget(val scene: IMoleScene,val transition: ITransitionUI, var toBeEdited: Boolean = false) extends ConnectionWidget(scene.graphScene){
  
  val label = new ConnectorLabel
  val componentWidget = new ConditionWidget(scene,label)
  setConstraint(componentWidget, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
  addChild(componentWidget)
    
  transition.condition match {
    case Some(x:String)=>label.text = x
    case None=>
  }
  
  setLabelVisible
  drawTransitionType
  toBeEdited = true
  
  def setLabelVisible= {
    componentWidget.setVisible(!label.text.isEmpty)
    label.revalidate
    scene.refresh
  }
  
  def drawTransitionType = {
    transition.transitionType match {
      case EXPLORATION_TRANSITION=> setSourceAnchorShape(AnchorShapeFactory.createImageAnchorShape(EXPLORATION_TRANSITON,false))
      case AGGREGATION_TRANSITION=> setTargetAnchorShape(AnchorShapeFactory.createImageAnchorShape(AGGREGATION_TRANSITON,false))
      case _=> setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)
    }
  }
  
  class ConditionWidget(scene: IMoleScene,label: ConnectorLabel) extends ComponentWidget(scene.graphScene,label.peer) {
    setPreferredBounds(new Rectangle(81, 31))
    setOpaque(true)
    override def paintBackground = {
      val g = scene.graphScene.getGraphics
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON)
      g.setColor(new Color(0, 0, 0, 200))
      g.fillRoundRect(0, 0, label.size.width, label.size.height,10, 10)
      revalidate
    }
  
    override def paintBorder = {
      val g = scene.graphScene.getGraphics
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON)
      g.setStroke(new BasicStroke(3f))
      g.setColor(new Color(200,200,200))
      g.drawRoundRect(0,0,label.size.width + 1,label.size.height+1,10,10)
      revalidate
    }
  }
  
  class ConnectorLabel extends Label {
    foreground = Color.white
    preferredSize = new Dimension(80,30)
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  
    listenTo(mouse.clicks)
    reactions += {
      case e: MousePressed => edit
    }
    
    def edit = {
      if (toBeEdited) {
        text = DialogFactory.groovyEditor("Condition",text)
        ConnectorWidget.this.transition.condition = Some(text)
        revalidate
      }
    }
  }
}
  
