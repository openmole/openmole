/*
 * Copyright (C) 2011 leclaire
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

import java.awt.Point
import org.netbeans.api.visual.anchor.PointShape
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.dataproxy.IEnvironmentDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ISamplingDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.panel.PanelMode
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.panel.EnvironmentPanelUI
import org.openmole.ide.core.implementation.panel.PrototypePanelUI
import org.openmole.ide.core.implementation.panel.SamplingPanelUI
import org.openmole.ide.core.implementation.panel.TaskPanelUI
import org.openmole.ide.core.implementation.provider.TransitionMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import org.openmole.ide.core.model.panel.PanelMode._

class BuildMoleScene(n: String = "",
                     id : Int = ScenesManager.countBuild.getAndIncrement) extends MoleScene(n,id) {
  override val isBuildScene = true
  
  def copy =  {
    var capsuleMapping = new HashMap[ICapsuleUI,ICapsuleUI]
    var islots = new HashMap[IInputSlotWidget, IInputSlotWidget]
    val ms = new ExecutionMoleScene(ScenesManager.countExec.get,manager.name+"_"+ScenesManager.countExec.incrementAndGet)
    manager.capsules.foreach(n=> {
        val (caps,islotMapping) = n._2.copy(ms)
        if (n._2.dataUI.startingCapsule) ms.manager.setStartingCapsule(caps)
        SceneItemFactory.createCapsule(caps,ms, new Point(n._2.x.toInt / 2,n._2.y.toInt / 2))
        capsuleMapping+= n._2-> caps
        islots++= islotMapping})
    manager.transitions.foreach(t=> {SceneItemFactory.createTransition(ms,capsuleMapping(t.source), islots(t.target), t.transitionType, t.condition)})
    manager.dataChannels.foreach(dc=>{SceneItemFactory.createDataChannel(ms, capsuleMapping(dc.source),capsuleMapping(dc.target),dc.prototypes)})
    ms
  }
  
  def initCapsuleAdd(w: ICapsuleUI)= {
    obUI= Some(w.asInstanceOf[Widget])
    obUI.get.createActions(CONNECT).addAction(connectAction)
    obUI.get.createActions(CONNECT).addAction(moveAction)
  }
  
  def attachEdgeWidget(e: String)= {
    val connectionWidget = ScenesManager.connectMode match {
      case true=> val x = new ConnectorWidget(this,manager.transition(e))
        x.setEndPointShape(PointShape.SQUARE_FILLED_BIG)
        x.getActions.addAction(ActionFactory.createPopupMenuAction(new TransitionMenuProvider(this,x)))
        x
      case false=> val x = new DataChannelConnectionWidget(this,manager.dataChannel(e))
        x
    }
    
    connectLayer.addChild(connectionWidget);
    //  connectionWidget.getActions.addAction(createSelectAction)
    connectionWidget.getActions.addAction(createObjectHoverAction)
    connectionWidget.getActions.addAction(reconnectAction)
    connectionWidget
  }
}
