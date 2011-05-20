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
import java.awt.Container
import java.awt.Font
import java.awt.Graphics2D
import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.commons.Constants
import scala.collection.mutable.HashSet

class ConnectableWidget(scene: MoleScene, val capsuleView: CapsuleViewUI) extends MyWidget(scene, capsuleView.capsuleModel){

  var islots= HashSet.empty[ISlotWidget]
  val oslot= new OSlotWidget(scene,capsuleView)
  
  addChild(oslot)
  createActions(scene.MOVE).addAction(ActionFactory.createMoveAction)
  
  def setDetailedView= {
    setWidthHint
    oslot.setDetailedView(taskWidth)
  }
  
  def addInputSlot(iw: ISlotWidget) {
    islots.add(iw)
    addChild(iw)
    scene.validate
  }
    
  def clearInputSlots= {
    islots.foreach(removeChild(_))
    islots.clear
  }
 
  override def paintWidget= {
    super.paintWidget
    val graphics = getGraphics.asInstanceOf[Graphics2D]
    graphics.setColor(Color.WHITE)
    graphics.setFont(new Font("Ubuntu", Font.PLAIN, 12))
    
    if (capsuleView.capsuleModel.taskUI.isDefined) {
      graphics.drawLine(taskWidth / 2,
                        Constants.TASK_TITLE_HEIGHT,
                        taskWidth / 2,
                        widgetArea.height - 3)

      graphics.setColor(new Color(0, 0, 0))
      var x = taskWidth / 2 + 11
      var i= 0
      var otherColumn = true
      (capsuleView.capsuleModel.taskUI.get.prototypesIn.toList:::capsuleView.capsuleModel.taskUI.get.prototypesOut.toList).foreach(p=> {
          if (i >= capsuleView.capsuleModel.taskUI.get.prototypesIn.size && otherColumn == true) {
            i= 0
            x += taskWidth / 2 - 1
            otherColumn = false
          }
          var st = p.panelUIData.name
          if (st.length> 10) st = st.substring(0, 8).concat("...")
          val h = 5 + Constants.TASK_TITLE_HEIGHT + i * Images.THUMB_SIZE
          graphics.drawImage(Images.thumb(p.factoryUI.imagePath),x - taskWidth / 2, h ,new Container)
          if (MoleScenesManager.detailedView) graphics.drawString(st, 1 + x - taskWidth / 2 +  Images.THUMB_SIZE, h + Images.THUMB_SIZE / 2)
          i+= 1
        })

      val newH= scala.math.max(capsuleView.capsuleModel.taskUI.get.prototypesIn.size, capsuleView.capsuleModel.taskUI.get.prototypesOut.size) * 22 + 45
      val delta= bodyArea.height - newH
      if (delta < 0) {
        bodyArea.setSize(bodyArea.width, newH)
        enlargeWidgetArea(0, -delta)
      }
    }
    revalidate
  }
  
}