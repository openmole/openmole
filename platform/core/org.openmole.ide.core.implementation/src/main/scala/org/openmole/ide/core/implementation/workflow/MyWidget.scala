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

class MyWidget(scene: IMoleScene,capsule: ICapsuleUI) extends Widget(scene.graphScene) {

  var taskHeight= TASK_CONTAINER_HEIGHT
  val bodyArea = new Rectangle
  val titleArea = new Rectangle
  
  override def paintWidget= {
    val graphics= getGraphics.asInstanceOf[Graphics2D]
    if(capsule.dataProxy.isDefined){
      val tpud = capsule.dataProxy.get.dataUI
      drawBox(graphics,tpud.backgroundColor,tpud.borderColor)
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
