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
import org.openmole.ide.core.model.workflow._
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.implementation.data.{ CheckData }
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.provider.{ MoleSceneMenuProvider, ConnectorMenuProvider }
import org.openmole.ide.core.model.commons.Constants._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import org.netbeans.api.visual.action.WidgetAction.State._
import org.openmole.ide.core.implementation.builder.SceneFactory
import scala.Some
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.implementation.dataproxy.ProxyFreezer

object BuildMoleScene {
  def apply(name: String) = new BuildMoleScene(new MoleUI(name))
}

class BuildMoleScene(val manager: IMoleUI) extends MoleScene with IBuildMoleScene { buildMoleScene ⇒

  getActions.addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)))

  override val isBuildScene = true

  override def refresh {
    manager.invalidateCache
    CheckData.checkMole(this)
    manager.capsules.foreach { case (_, c) ⇒ c.update }
    super.refresh
  }

  def copyScene = {
    def copy(caspuleUI: ICapsuleUI, sc: IMoleScene) = {
      val c = CapsuleUI(sc)
      val slotMapping = caspuleUI.islots.map(i ⇒ i -> c.addInputSlot).toMap
      (c, slotMapping)
    }

    def deepcopy(caspuleUI: ICapsuleUI, sc: IMoleScene) = {
      val ret = copy(caspuleUI, sc)
      import caspuleUI.dataUI
      dataUI.task match {
        case Some(x: ITaskDataProxyUI) ⇒
          ret._1.encapsule(ProxyFreezer.freeze(x))
          if (dataUI.environment.isDefined) ret._1.environment_=(ProxyFreezer.freeze(dataUI.environment))
        case _ ⇒
      }
      ret
    }

    var capsuleMapping = new HashMap[ICapsuleUI, ICapsuleUI]
    var islots = new HashMap[IInputSlotWidget, IInputSlotWidget]
    val ms = ExecutionMoleScene(manager.name + "_" + ScenesManager.countExec.incrementAndGet)
    manager.capsules.foreach(n ⇒ {
      val (caps, islotMapping) = deepcopy(n._2, ms)
      if (manager.startingCapsule == Some(n._2)) ms.manager.startingCapsule = Some(caps)
      ms.add(caps, new Point(n._2.x.toInt / 2, n._2.y.toInt / 2))
      capsuleMapping += n._2 -> caps
      islots ++= islotMapping
      caps.setAsValid
    })
    manager.connectors.foreach { c ⇒
      c match {
        case t: ITransitionUI ⇒
          val transition = new TransitionUI(
            capsuleMapping(t.source),
            islots(t.target),
            t.transitionType,
            t.condition,
            t.filteredPrototypes)
          ms.add(transition)
        case dc: IDataChannelUI ⇒
          val dataC = new DataChannelUI(
            capsuleMapping(dc.source),
            islots(dc.target),
            dc.filteredPrototypes)
          ms.add(dataC)
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
