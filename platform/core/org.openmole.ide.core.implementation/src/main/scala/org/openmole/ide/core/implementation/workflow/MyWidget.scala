/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.workflow

import java.awt.Color
import java.awt.Rectangle
import java.awt.Font
import java.awt.Graphics2D
import org.netbeans.api.visual.widget._
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.ICapsuleUI
import java.awt.BasicStroke
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.widget.LinkLabel
import scala.swing.Action

class MyWidget(scene: IMoleScene,capsule: ICapsuleUI) extends Widget(scene.graphScene) {

  var taskWidth= TASK_CONTAINER_WIDTH
  var taskHeight= TASK_CONTAINER_HEIGHT
  var taskImageOffset= TASK_IMAGE_WIDTH_OFFSET
  val bodyArea = new Rectangle
  val widgetArea= new Rectangle
  val titleArea = new Rectangle
  setWidthHint
//  val wi = new Widget(scene.graphScene)
//  wi.addChild(new ComponentWidget(scene.graphScene,
//                                  new LinkLabel(capsule.dataProxy.get.dataUI.name,new Action("") { def apply = scene.displayPropertyPanel(capsule.dataProxy.get,EDIT)}).peer))
//
//  
  def widgetWidth= widgetArea.width
  
  
  def setWidthHint= {
    if (capsule.detailedView) {
      taskWidth = EXPANDED_TASK_CONTAINER_WIDTH
      taskImageOffset = EXPANDED_TASK_IMAGE_WIDTH_OFFSET
    }
    else {
      taskWidth = TASK_CONTAINER_WIDTH
      taskImageOffset = TASK_IMAGE_WIDTH_OFFSET
    }
    bodyArea.setBounds(new Rectangle(0, 0,taskWidth,TASK_CONTAINER_HEIGHT))
    widgetArea.setBounds(new Rectangle(-12, -11,taskWidth + 26,taskHeight+ 16))
    titleArea.setBounds(new Rectangle(0, 0,taskWidth,TASK_TITLE_HEIGHT))
    setPreferredBounds(widgetArea)
    revalidate
    repaint
  }
  
  def enlargeWidgetArea(y: Int,height: Int) {
    widgetArea.height += height
    widgetArea.y -= y
  }
  
  override def paintWidget= {
    val graphics= getGraphics.asInstanceOf[Graphics2D]
    if(capsule.dataProxy.isDefined){
      val tpud = capsule.dataProxy.get.dataUI
      drawBox(graphics,tpud.backgroundColor,tpud.borderColor)
      //graphics.fill(titleArea)
      graphics.setColor(Color.WHITE)
      graphics.setFont(new Font("Ubuntu", Font.PLAIN, 15))
      graphics.drawString(tpud.name, 10, 15)
    }
    else drawBox(graphics,new Color(204,204,204,128),new Color(204,204,204))
  }
  
  def drawBox(graphics: Graphics2D,c1: Color, c2: Color) = {
    scene match {
      case x: BuildMoleScene=> graphics.setColor(c1)
      case _=> graphics.setColor(new Color(215,238,244,64))
    }
    graphics.fill(bodyArea)
    scene match {
      case x: BuildMoleScene=> graphics.setColor(c2)
      case _=> graphics.setColor(new Color(44,137,160,64))
    }
    graphics.draw(new BasicStroke(1.3f, 1, 1).createStrokedShape(bodyArea))
  }
  
}
