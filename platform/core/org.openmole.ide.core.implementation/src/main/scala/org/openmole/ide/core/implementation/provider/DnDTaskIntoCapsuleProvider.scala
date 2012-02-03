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

package org.openmole.ide.core.implementation.provider

import java.awt.Point
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.widget.Widget
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.implementation.display.TaskDisplay
import org.openmole.ide.core.model.commons.CapsuleType._
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.display.Displays
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.commons.Constants._

class DnDTaskIntoCapsuleProvider(molescene: IMoleScene,val capsule: ICapsuleUI) extends DnDProvider(molescene) {
  
  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= { 
    Displays.dataProxy.get.dataUI.entityType match {
      case TASK=> if (capsule.capsuleType == CAPSULE) ConnectorState.ACCEPT else ConnectorState.REJECT
      case PROTOTYPE=> ConnectorState.ACCEPT
      case SAMPLING=> if (capsule.capsuleType == EXPLORATION_TASK) ConnectorState.ACCEPT else ConnectorState.REJECT
      case ENVIRONMENT=> if (capsule.capsuleType != CAPSULE) ConnectorState.ACCEPT else ConnectorState.REJECT
      case _=> throw new UserBadDataError("Unknown entity type")
    }
  }
  
  override def accept(widget: Widget,point: Point,transferable: Transferable)= { 
//    Displays.dataProxy.get match {
//      case dpu:ITaskDataProxyUI => capsule.encapsule(transferable.getTransferData(TASK_DATA_FLAVOR).asInstanceOf[TaskDataProxyUI])
//        if (molescene.manager.capsules.size == 1) capsule.defineAsStartingCapsule(true)
//      case dpu:IPrototypeDataProxyUI=> { 
//          if (point.x < capsule.connectableWidget.widgetWidth / 2) capsuleDataUI.addPrototype(dpu, IOType.INPUT)
//          else capsuleDataUI.addPrototype(dpu, IOType.OUTPUT)
//        }
//      case dpu:ISamplingDataProxyUI=> capsuleDataUI.sampling = Some(dpu)
//      case dpu:IEnvironmentDataProxyUI=> capsuleDataUI.environment = Some(dpu)
//    }
//    // selectTask
//    molescene.graphScene.repaint
//    molescene.graphScene.validate
    true
  }
  
  private def selectTask{
    if(capsule.dataProxy.isDefined){
      Displays.currentType = TASK
      TaskDisplay.currentDataProxy = capsule.dataProxy
      Displays.propertyPanel.displayCurrentEntity}
  }
  
  private def capsuleDataUI = capsule.dataProxy.get.dataUI
}