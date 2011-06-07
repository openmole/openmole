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

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Color
import java.awt.Rectangle
import java.awt.Font
import java.awt.Graphics2D
import org.netbeans.api.visual.widget._
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.TaskPanelUIData
import org.openmole.ide.core.workflow.implementation.MoleScene
import java.awt.BasicStroke
import org.openmole.ide.core.workflow.model.ICapsuleView

class MyWidget(scene: MoleScene,capsule: ICapsuleView) extends Widget(scene) {

  var taskWidth= Constants.TASK_CONTAINER_WIDTH
  var taskHeight= Constants.TASK_CONTAINER_HEIGHT
  var taskImageOffset= Constants.TASK_IMAGE_WIDTH_OFFSET
  val bodyArea = new Rectangle
  val widgetArea= new Rectangle
  val titleArea = new Rectangle
  setWidthHint
  
  def widgetWidth= widgetArea.width
  
  
  def setWidthHint= {
    if (MoleScenesManager.detailedView) {
      taskWidth = Constants.EXPANDED_TASK_CONTAINER_WIDTH
      taskImageOffset = Constants.EXPANDED_TASK_IMAGE_WIDTH_OFFSET
    }
    else {
      taskWidth = Constants.TASK_CONTAINER_WIDTH
      taskImageOffset = Constants.TASK_IMAGE_WIDTH_OFFSET
    }
    bodyArea.setBounds(new Rectangle(0, 0,taskWidth,Constants.TASK_CONTAINER_HEIGHT))
    widgetArea.setBounds(new Rectangle(-12, -1,taskWidth + 24,taskHeight+ 2))
    titleArea.setBounds(new Rectangle(0, 0,taskWidth,Constants.TASK_TITLE_HEIGHT))
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
      val tpud = capsule.dataProxy.get.panelUIData.asInstanceOf[TaskPanelUIData]
      drawBox(graphics,tpud.backgroundColor,tpud.borderColor)
      graphics.fill(titleArea)
      graphics.setColor(Color.WHITE)
      graphics.setFont(new Font("Ubuntu", Font.PLAIN, 15))
      graphics.drawString(tpud.name, 10, 15)
    }
    else drawBox(graphics,new Color(204,204,204,128),new Color(204,204,204))

    /* if(capsuleModel.taskUI.isDefined) graphics.drawImage(ImageUtilities.loadImage(capsuleModel.taskUI.get.factory.imagePath),
     taskImageOffset,
     Constants.TASK_IMAGE_HEIGHT_OFFSET,
     Constants.TASK_IMAGE_WIDTH,
     Constants.TASK_IMAGE_HEIGHT,
     capsuleModel.taskUI.get.factory.backgroundColor,
     new Container)*/
  }
  
  def drawBox(graphics: Graphics2D,c1: Color, c2: Color) = {
    graphics.setColor(c1)
    graphics.fill(bodyArea)
    graphics.setColor(c2)
    graphics.draw(new BasicStroke(1.3f, 1, 1).createStrokedShape(bodyArea))
  }
  
}
