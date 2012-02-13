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
import java.awt.Stroke
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import org.netbeans.api.visual.action.WidgetAction
import org.netbeans.api.visual.action.WidgetAction._
import org.netbeans.api.visual.anchor.AnchorShapeFactory
import org.netbeans.api.visual.border.BorderFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.Scene
import org.netbeans.api.visual.widget.Widget
import scala.swing.UIElement
import scala.swing.event.MousePressed

class DataChannelConnectionWidget(scene: Scene) extends ConnectionWidget(scene){
  //setBorder(BorderFactory.createDashedBorder(Color.red,3,3))
  setLineColor(Color.gray)
  setStroke(new BasicStroke(10, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,20.0f, List(10.0f).toArray, 0.0f))
  setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  
  setSourceAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.IMAGE_OUTPUT_DATA_CHANNEL,false))
  setTargetAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.IMAGE_OUTPUT_DATA_CHANNEL,false))
  
  def mousePressed (widget: Widget,event: WidgetAction.WidgetMouseEvent) : State  = {
    println("pressed")
    State.CONSUMED
  }
  
  def mouseClicked (widget: Widget,event: WidgetAction.WidgetMouseEvent) : State  = {
    println("pressed")
    State.CONSUMED
  }
}
