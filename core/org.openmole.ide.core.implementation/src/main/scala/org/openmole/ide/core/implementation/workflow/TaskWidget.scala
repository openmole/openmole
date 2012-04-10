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

import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow._
import scala.swing.Action
import java.awt.Color
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Graphics2D
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.RenderingHints
import org.openmole.ide.core.model.panel.PanelMode._
import scala.swing.Panel

class TaskWidget(scene: IMoleScene,
                 val capsule : ICapsuleUI) extends Panel {
  peer.setLayout(new BorderLayout)
  preferredSize = new Dimension(TASK_CONTAINER_WIDTH,TASK_CONTAINER_HEIGHT)
  
  override def paint(g : Graphics2D) = {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON)
    g.setColor(backColor)
    g.fillRect(0, 0, preferredSize.width, preferredSize.height)
    g.setColor(borderColor)
    g.setStroke(new BasicStroke(5))
    g.draw(new Rectangle(bounds.x,bounds.y,bounds.width-1,bounds.height-1))
    g.fillRect(0, 0, preferredSize.width, TASK_TITLE_HEIGHT)
  }
  
  def backColor : Color =  { 
    capsule.dataUI.task match {
      case Some(x : ITaskDataProxyUI) => 
        scene match {
          case y:BuildMoleScene=> x.dataUI.backgroundColor
          case _=> new Color(215,238,244,64)
        }
      case _=> 
        new Color(204,204,204,128)
    }
  }
  
  def borderColor : Color =  { 
    capsule.dataUI.task match {
      case Some(x : ITaskDataProxyUI) => 
        scene match {
          case y: BuildMoleScene=> x.dataUI.borderColor
          case _=> new Color(44,137,160,64)
        }
      case _=> new Color(204,204,204)
    }
  }
}
