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

import java.awt.Point
import java.awt.Color
import java.awt.Dimension
import java.awt.Container
import java.awt.Font
import java.awt.Graphics2D
import javax.swing.ImageIcon
import scala.swing.Label
import org.openmole.ide.misc.image.ImageTool
import org.openmole.ide.core.implementation.data.AbstractExplorationTaskDataUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IConnectableWidget
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.mutable.ListBuffer

class ConnectableWidget(scene: IMoleScene, val capsule: ICapsuleUI) extends MyWidget(scene, capsule) with IConnectableWidget{

  var islots= ListBuffer.empty[IInputSlotWidget]
  val oslot= new OutputSlotWidget(scene,capsule)
  var samplingWidget: Option[SamplingWidget] = None
  
  val nameLabel = new Label(capsule.toString) {
     icon = new ImageIcon(ImageTool.loadImage("img/edit.png",50,50))
    preferredSize = new Dimension(TASK_CONTAINER_WIDTH,TASK_TITLE_HEIGHT)
    foreground = Color.RED
    background = Color.BLUE}
  val labelWidget = new ComponentWidget(scene.graphScene,nameLabel.peer)
  labelWidget.setPreferredLocation(new Point(50,50))
  addChild(oslot)
  addChild(labelWidget)
  scene.validate
  
  createActions(MOVE).addAction(ActionFactory.createMoveAction)

  implicit def bool2int(b:Boolean) = if (b) 1 else 0
  
  override def x = convertLocalToScene(getLocation).getX
  
  override def y = convertLocalToScene(getLocation).getY
  
  def addInputSlot(iw: InputSlotWidget) {
    islots += iw
    addChild(iw)
    scene.validate
  }
    
  def removeFirstInputSlot = {
    val toBeRemoved = islots.tail(0)
    removeChild(toBeRemoved.widget)
    islots-= toBeRemoved
  }
  
  
  def addSampling= {
    samplingWidget = Some(new SamplingWidget(scene,capsule))
    samplingWidget.get.setPreferredLocation(new Point(TASK_CONTAINER_WIDTH / 2 - 15,TASK_CONTAINER_HEIGHT - 15 ))
    addChild(samplingWidget.get) 
    scene.refresh
  }

  
  override def paintWidget= {
    super.paintWidget
    val graphics = getGraphics.asInstanceOf[Graphics2D]
    graphics.setFont(new Font("Ubuntu", Font.PLAIN, 12))
  //  println("capsule tostring :: " + capsule.toString)
    nameLabel.name = capsule.toString
    capsule.dataProxy match {
      case Some(x : ITaskDataProxyUI)=> x.dataUI match {
          case y : AbstractExplorationTaskDataUI => 
            y.sampling match {
              case Some(z : ISamplingDataProxyUI)=> 
                graphics.setColor(new Color(204,204,204))
                graphics.drawLine(TASK_CONTAINER_WIDTH / 2,
                                  TASK_TITLE_HEIGHT,
                                  TASK_CONTAINER_WIDTH / 2,
                                  TASK_CONTAINER_HEIGHT- 3 + 30)
               // graphics.drawImage(ImageTool.loadImage(z.dataUI.imagePath,30,30), TASK_CONTAINER_WIDTH / 2 - 15,TASK_CONTAINER_HEIGHT + 15 , new Container)
              case _=>
            }
          case _=>
        }
      case _=>
    }
           
    capsule.environment match {
      case Some(x:IEnvironmentDataProxyUI) => graphics.drawImage(ImageTool.loadImage(x.dataUI.imagePath,30,30), TASK_CONTAINER_WIDTH - 10, -10, new Container)
      case _=>
    }            
    revalidate
                        
  }
}
