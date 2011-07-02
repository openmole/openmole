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
import org.openmole.ide.core.dataproxy._
import org.openmole.ide.core.data.IDataUI
import org.openmole.ide.core.data.ITaskDataUI
import org.openmole.ide.core.workflow.MoleScene
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.control.MoleScenesManager
import org.netbeans.api.visual.action.ConnectorState



class DnDNewTaskProvider(molescene: MoleScene) extends DnDProvider(molescene) {
  

  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= {
    transferable.getTransferData(Constants.TASK_DATA_FLAVOR).asInstanceOf[TaskDataProxyUI].dataUI.entityType match {
      case Constants.TASK=> ConnectorState.ACCEPT
      case _=> ConnectorState.REJECT
    }
  }
 
  override def accept(widget: Widget,point: Point,transferable: Transferable)= {
    val capsule = MoleScenesManager.createCapsule(molescene,point)
    capsule.encapsule(transferable.getTransferData(Constants.TASK_DATA_FLAVOR).asInstanceOf[TaskDataProxyUI])
    capsule.addInputSlot(false)
    
    molescene.repaint
    molescene.revalidate
  }
}