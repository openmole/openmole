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
import java.awt.Point
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.action.SelectProvider
import org.netbeans.api.visual.action.WidgetAction
import org.netbeans.api.visual.action.WidgetAction._
import org.netbeans.api.visual.anchor.AnchorShapeFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.Scene
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.workflow.IDataChannelUI
import org.openmole.ide.core.implementation.dialog.DataChannelDialog

class DataChannelConnectionWidget(scene: Scene, val dataChannelUI: IDataChannelUI) extends ConnectionWidget(scene){
  setLineColor(new Color(188,188,188))
  setStroke(new BasicStroke(6, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,20.0f, List(10.0f).toArray, 0.0f))
  getActions.addAction(ActionFactory.createSelectAction(new ObjectSelectProvider))
   setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  
  setSourceAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.IMAGE_OUTPUT_DATA_CHANNEL,false))
  setTargetAnchorShape(AnchorShapeFactory.createImageAnchorShape(Images.IMAGE_OUTPUT_DATA_CHANNEL,false))
  
  class ObjectSelectProvider extends SelectProvider {
        
    override def isAimingAllowed(w: Widget,localLocation: Point,invertSelection: Boolean) = false
                
    override def isSelectionAllowed(w: Widget,localLocation: Point,invertSelection: Boolean) = true
        
    override def select(w: Widget,localLocation: Point,invertSelection: Boolean) = DataChannelDialog.display(DataChannelConnectionWidget.this)
  }
}
