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
import scala.collection.JavaConversions
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.commons.CapsuleType._
import org.openmole.ide.core.palette._
import org.openmole.ide.core.properties._
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.commons.Constants._

class DnDTaskIntoCapsuleProvider(molescene: MoleScene,val capsule: ICapsuleUI) extends DnDProvider(molescene) {
  var encapsulated= false
  
  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= { 

    Displays.dataProxy.dataUI.entityType match {
      case TASK=> if (!encapsulated) ConnectorState.ACCEPT else ConnectorState.REJECT
      case PROTOTYPE=> ConnectorState.ACCEPT
      case SAMPLING=> if (capsule.capsuleType == EXPLORATION_TASK) ConnectorState.ACCEPT else ConnectorState.REJECT
      case ENVIRONMENT=> println("envir"); ConnectorState.ACCEPT
      case _=> throw new GUIUserBadDataError("Unknown entity type")
    }
  }
  
  override def accept(widget: Widget,point: Point,transferable: Transferable)= { 

      Displays.dataProxy match {
      case dpu:TaskDataProxyUI => capsule.encapsule(dpu)
      case dpu:PrototypeDataProxyUI=> { 
          if (point.x < capsule.connectableWidget.widgetWidth / 2) capsuleDataUI.addPrototype(dpu, IOType.INPUT)
          else capsuleDataUI.addPrototype(dpu, IOType.OUTPUT)
        }
      case dpu:SamplingDataProxyUI=> capsuleDataUI.sampling = Some(dpu)
    }
    
    molescene.repaint
    molescene.revalidate
  }
  
  private def capsuleDataUI = capsule.dataProxy.get.dataUI
}