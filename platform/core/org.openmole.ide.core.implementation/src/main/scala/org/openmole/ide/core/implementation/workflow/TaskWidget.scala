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
import scala.swing.Panel
import javax.swing.BorderFactory
import java.awt.Color
import java.awt.BorderLayout
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.Dimension
import java.awt.RenderingHints
import org.openmole.ide.core.model.panel.PanelMode._

class TaskWidget(scene: IMoleScene,
                 val capsule : ICapsuleUI) extends Panel {
  peer.setLayout(new BorderLayout)
  if(capsule.dataProxy.isDefined) println("capsule name " + capsule.dataProxy.get.dataUI.name)
  val widgetArea = new Rectangle
  widgetArea.setBounds(new Rectangle(-10, -10, TASK_CONTAINER_WIDTH + 20,TASK_CONTAINER_HEIGHT + 20)) 
   
  val taskArea = new Rectangle
  taskArea.setBounds(new Rectangle(0, 0, TASK_CONTAINER_WIDTH,TASK_CONTAINER_HEIGHT)) 
  preferredSize = new Dimension(widgetArea.getBounds.width,widgetArea.getBounds.height)
  
  val titleLabel = new LinkLabel(capsule.toString, new Action(""){ 
      def apply = {
        capsule.dataProxy match {
          case Some(x : ITaskDataProxyUI) => scene.displayPropertyPanel(x,EDIT)
          case _=>
        }
      }
    }){
    foreground = Color.WHITE
  }
    
  peer.add(titleLabel.peer,BorderLayout.NORTH)
  
  override def paintComponent(g : Graphics2D) = {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON)
    g.setColor(backColor)
    g.fillRect(0, 0, preferredSize.width, preferredSize.height)
    val bo = borderColor
    g.setColor(bo)
    g.fillRect(0, 0, preferredSize.width, TASK_TITLE_HEIGHT)
    border = BorderFactory.createLineBorder(bo, 2)
    repaint
    revalidate
  }
  
  def backColor : Color =  { 
    capsule.dataProxy match {
      case Some(x : ITaskDataProxyUI) => 
        titleLabel.text = x.dataUI.name
        scene match {
          case y:BuildMoleScene=> x.dataUI.backgroundColor
          case _=> new Color(215,238,244,64)
        }
      case _=> new Color(204,204,204,128)
    }
  }
  
  def borderColor : Color =  { 
    capsule.dataProxy match {
      case Some(x : ITaskDataProxyUI) => 
        scene match {
          case y: BuildMoleScene=> x.dataUI.borderColor
          case _=> new Color(44,137,160,64)
        }
      case _=> new Color(204,204,204)
    }
  }
}
