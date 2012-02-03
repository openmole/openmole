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
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.display.Displays
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.implementation.workflow.SceneItemFactory
import org.netbeans.api.visual.action.ConnectorState



class DnDNewTaskProvider(molescene: IMoleScene) extends DnDProvider(molescene) {

  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= {
    Displays.dataProxy.get.dataUI.entityType match {
      case TASK=> ConnectorState.ACCEPT
      case _=> ConnectorState.REJECT
    }
  }
 
  override def accept(widget: Widget,point: Point,transferable: Transferable)= 
//    val capsule = SceneItemFactory.createCapsule(molescene,point)
//    capsule.encapsule(transferable.getTransferData(TASK_DATA_FLAVOR).asInstanceOf[TaskDataProxyUI])
//    capsule.addInputSlot(molescene.manager.capsules.size == 1)
//    
//    molescene.graphScene.repaint
//    molescene.graphScene.revalidate
//  }
  true
}