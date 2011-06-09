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

package org.openmole.ide.core.provider

import java.awt.Point
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.widget.Widget
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.workflow.model.ICapsuleUI
import org.openmole.ide.core.palette.DataProxyUI
import org.openmole.ide.core.commons.CapsuleType._
import org.openmole.core.model.sampling.ISampling
import org.openmole.ide.core.properties._
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.commons.Constants

class DnDTaskIntoCapsuleProvider(molescene: MoleScene,val capsule: ICapsuleUI) extends DnDProvider(molescene) {
  var encapsulated= false
  
  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= { 
//    val ent = transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[IDataProxyUI].dataUI.entityType
//    var state= ConnectorState.REJECT
//    if (!encapsulated){
//      if (ent == Constants.TASK) state = ConnectorState.ACCEPT
//    }
//    else {
//      ent match {
//        case Constants.PROTOTYPE=> state = ConnectorState.ACCEPT
//        case Constants.SAMPLING=> if (capsule.capsuleType == EXPLORATION_TASK) state = ConnectorState.ACCEPT
//        case Constants.ENVIRONMENT=> println("envir"); state = ConnectorState.ACCEPT
//        case _=> throw new GUIUserBadDataError("Unknown entity type")
//      }
//    }
//    state
    
    transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[DataProxyUI[_<:IDataUI]].dataUI.entityType match {
      case Constants.TASK=> if (!encapsulated) ConnectorState.ACCEPT else ConnectorState.REJECT
      case Constants.PROTOTYPE=> ConnectorState.ACCEPT
      case Constants.SAMPLING=> if (capsule.capsuleType == EXPLORATION_TASK) ConnectorState.ACCEPT else ConnectorState.REJECT
      case Constants.ENVIRONMENT=> println("envir"); ConnectorState.ACCEPT
      case _=> throw new GUIUserBadDataError("Unknown entity type")
    }
  }
  
  override def accept(widget: Widget,point: Point,transferable: Transferable)= { 
//     val dpu = transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[IDataProxyUI]
//    dpu.dataUI.entityType match{
//      case Constants.TASK=> capsule.encapsule(dpu)
//      case Constants.PROTOTYPE=> { 
//          println("PROTO !!" )
//          if (point.x < capsule.connectableWidget.widgetWidth / 2) capsuleDataUI.addPrototype(dpu, IOType.INPUT)
//          else capsuleDataUI.addPrototype(dpu, IOType.OUTPUT)
//        }
//      case Constants.SAMPLING=> capsuleDataUI.sampling = Some(dpu.asInstanceOf[DataProxyUI[DataUI[ISampling]]])
//    }
    
    val dpu = transferable.getTransferData(Constants.ENTITY_DATA_FLAVOR).asInstanceOf[DataProxyUI[_<:IDataUI]]
    dpu.dataUI.entityType match{
      case Constants.TASK=> capsule.encapsule(dpu.asInstanceOf[DataProxyUI[ITaskDataUI]])
      case Constants.PROTOTYPE=> { 
          println("PROTO !!" )
          if (point.x < capsule.connectableWidget.widgetWidth / 2) capsuleDataUI.addPrototype(dpu.asInstanceOf[DataProxyUI[IPrototypeDataUI]], IOType.INPUT)
          else capsuleDataUI.addPrototype(dpu.asInstanceOf[DataProxyUI[IPrototypeDataUI]], IOType.OUTPUT)
        }
      case Constants.SAMPLING=> capsuleDataUI.sampling = Some(dpu.asInstanceOf[DataProxyUI[ISamplingDataUI]])
    }
    molescene.repaint
    molescene.revalidate
  }
  
  def capsuleDataUI = capsule.dataProxy.get.dataUI
}
