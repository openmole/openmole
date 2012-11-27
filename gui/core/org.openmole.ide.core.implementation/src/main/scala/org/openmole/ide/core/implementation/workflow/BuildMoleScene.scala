/*
 * Copyright (C) 2011 Mathieu Leclaire
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
import org.openmole.ide.core.model.panel.PanelMode
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IDataChannelUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.provider.ConnectorMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.ITransitionUI
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import org.openmole.ide.core.model.panel.PanelMode._
import org.netbeans.api.visual.action.WidgetAction.State._

class BuildMoleScene(n: String = "") extends MoleScene(n) { buildMoleScene ⇒
  override val isBuildScene = true

  def copyScene = {
    var capsuleMapping = new HashMap[ICapsuleUI, ICapsuleUI]
    var islots = new HashMap[IInputSlotWidget, IInputSlotWidget]
    val ms = new ExecutionMoleScene(manager.name + "_" + ScenesManager.countExec.incrementAndGet)
    manager.capsules.foreach(n ⇒ {
      val (caps, islotMapping) = n._2.deepcopy(ms)
      if (manager.startingCapsule == Some(n._2)) ms.manager.setStartingCapsule(caps)
      SceneItemFactory.createCapsule(caps, ms, new Point(n._2.x.toInt / 2, n._2.y.toInt / 2))
      capsuleMapping += n._2 -> caps
      islots ++= islotMapping
      caps.setAsValid
    })
    manager.connectors.foreach { c ⇒
      c match {
        case t: ITransitionUI ⇒
          SceneItemFactory.createTransition(ms, capsuleMapping(t.source),
            islots(t.target),
            t.transitionType,
            t.condition,
            t.filteredPrototypes)
        case dc: IDataChannelUI ⇒
          SceneItemFactory.createDataChannel(ms, capsuleMapping(dc.source),
            islots(dc.target),
            dc.filteredPrototypes)
        case _ ⇒
      }
    }
    ms
  }

  def initCapsuleAdd(w: ICapsuleUI) = {
    obUI = Some(w.asInstanceOf[Widget])
    obUI.get.getActions.addAction(connectAction)
    obUI.get.getActions.addAction(dataChannelAction)
    obUI.get.getActions.addAction(createSelectAction)
    obUI.get.getActions.addAction(moveAction)
  }

  def attachEdgeWidget(e: String) = {
    val connectionWidget = new ConnectorWidget(this, manager.connector(e))
    connectionWidget.setEndPointShape(PointShape.SQUARE_FILLED_BIG)
    connectionWidget.getActions.addAction(ActionFactory.createPopupMenuAction(new ConnectorMenuProvider(this, connectionWidget)))
    connectionWidget.setRouter(new MoleRouter(capsuleLayer))
    connectLayer.addChild(connectionWidget)
    connectionWidget.getActions.addAction(createObjectHoverAction)
    connectionWidget
  }

  def removeSelectedWidgets = ScenesManager.selection.foreach { c ⇒
    graphScene.removeNodeWithEdges(manager.removeCapsuleUI(c))
    CheckData.checkMole(buildMoleScene)
  }
}
