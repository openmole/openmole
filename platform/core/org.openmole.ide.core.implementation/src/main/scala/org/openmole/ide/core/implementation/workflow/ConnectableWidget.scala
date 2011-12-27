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
import java.awt.Container
import java.awt.Font
import java.awt.Graphics2D
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IConnectableWidget
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.IMoleScene
import scala.collection.mutable.ListBuffer

class ConnectableWidget(scene: IMoleScene, val capsule: ICapsuleUI) extends MyWidget(scene, capsule) with IConnectableWidget{

  var islots= ListBuffer.empty[IInputSlotWidget]
  val oslot= new OutputSlotWidget(scene,capsule)
  var samplingWidget: Option[SamplingWidget] = None
  
  addChild(oslot)
  
  createActions(MOVE).addAction(ActionFactory.createMoveAction)

  implicit def bool2int(b:Boolean) = if (b) 1 else 0
  
  override def x = convertLocalToScene(getLocation).getX
  
  override def y = convertLocalToScene(getLocation).getY
  
  override def setDetailedView= {
    setWidthHint
    oslot.setDetailedView(taskWidth)
    if (samplingWidget.isDefined) samplingWidget.get.setDetailedView(taskWidth)
  }
  
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
    addChild(samplingWidget.get) 
    taskHeight += 50
    setWidthHint
    setDetailedView
  }

  
  override def paintWidget= {
    super.paintWidget
    val graphics = getGraphics.asInstanceOf[Graphics2D]
    graphics.setFont(new Font("Ubuntu", Font.PLAIN, 12))
    
    if (capsule.dataProxy.isDefined) {
      val dataUI = capsule.dataProxy.get.dataUI
      var x = taskWidth / 2 + 5
      var i= 0
      var otherColumn = true
      (dataUI.prototypesIn.toList:::dataUI.prototypesOut.toList).foreach(p=> {
          if (i >= dataUI.prototypesIn.size && otherColumn == true) {
            i= 0
            x += taskWidth / 2 - 1
            otherColumn = false
          }
          var st = p.dataUI.displayTypedName
          if (st.length> 10) st = st.substring(0, 8).concat("...")
          val h = 5 + TASK_TITLE_HEIGHT + i * Images.THUMB_SIZE
          graphics.drawImage(Images.thumb(p.dataUI.imagePath),x - taskWidth / 2, h ,new Container)
          graphics.setColor(new Color(102,102,102))
          if (capsule.detailedView) graphics.drawString(st, 1 + x - taskWidth / 2 +  Images.THUMB_SIZE, h + Images.THUMB_SIZE / 2)
          i+= 1
        })

      graphics.setColor(new Color(204,204,204))
      val newH= scala.math.max(dataUI.prototypesIn.size, dataUI.prototypesOut.size) * 22 + 45
      val delta= bodyArea.height - newH
      if (delta < 0) {
        bodyArea.setSize(bodyArea.width, newH)
        enlargeWidgetArea(0, -delta)
      }
      var lineH = 0
      if (samplingWidget.isDefined) lineH = samplingWidget.get.capsule.dataProxy.get.dataUI.sampling.isDefined * 40
      graphics.drawLine(taskWidth / 2,
                        TASK_TITLE_HEIGHT,
                        taskWidth / 2,
                        math.max(TASK_CONTAINER_HEIGHT- 3,newH) + lineH)
      
      if (dataUI.environment.isDefined) graphics.drawImage(Images.thumb(dataUI.environment.get.dataUI.imagePath), TASK_CONTAINER_WIDTH - 10, -10, new Container)
    }
    revalidate
  }
  
}
