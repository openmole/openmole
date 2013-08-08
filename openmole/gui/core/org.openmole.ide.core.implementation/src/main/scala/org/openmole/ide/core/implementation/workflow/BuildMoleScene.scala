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
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.implementation.data.{ CheckData }
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.provider.{ MoleSceneMenuProvider, ConnectorMenuProvider }
import scala.collection.mutable.HashMap
import scala.Some
import org.openmole.ide.core.implementation.dataproxy.{ TaskDataProxyUI, ProxyFreezer }

object BuildMoleScene {
  def apply(name: String) = new BuildMoleScene(new MoleUI(name))
}

class BuildMoleScene(val dataUI: IMoleUI) extends MoleScene { buildMoleScene ⇒

  getActions.addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)))

  val isBuildScene = true

  override def refresh {
    dataUI.invalidateCache
    CheckData.checkMole(this)
    dataUI.capsules.foreach { case (_, c) ⇒ c.update }
    super.refresh
  }

  def copyScene = {
    def copy(caspuleUI: CapsuleUI, sc: MoleScene) = {
      val c = CapsuleUI(sc)
      val slotMapping = caspuleUI.islots.map(i ⇒ i -> c.addInputSlot).toMap
      (c, slotMapping)
    }

    def deepcopy(caspuleUI: CapsuleUI, sc: MoleScene) = {
      val ret = copy(caspuleUI, sc)
      caspuleUI.dataUI.task match {
        case Some(x: TaskDataProxyUI) ⇒
          ret._1.encapsule(ProxyFreezer.freeze(x))
          if (caspuleUI.dataUI.environment.isDefined) ret._1.environment_=(ProxyFreezer.freeze(caspuleUI.dataUI.environment))
        case _ ⇒
      }
      ret
    }

    var capsuleMapping = new HashMap[CapsuleUI, CapsuleUI]
    var islots = new HashMap[InputSlotWidget, InputSlotWidget]
    val ms = ExecutionMoleScene(dataUI.name + "_" + ScenesManager.countExec.incrementAndGet)
    dataUI.capsules.foreach(n ⇒ {
      val (caps, islotMapping) = deepcopy(n._2, ms)
      if (dataUI.startingCapsule == Some(n._2)) ms.dataUI.startingCapsule = Some(caps)
      ms.add(caps, new Point(n._2.x.toInt / 2, n._2.y.toInt / 2))
      capsuleMapping += n._2 -> caps
      islots ++= islotMapping
      caps.setAsValid
    })
    dataUI.connectors.values.foreach {
      _ match {
        case t: TransitionUI ⇒
          val transition = new TransitionUI(
            capsuleMapping(t.source),
            islots(t.target),
            t.transitionType,
            t.condition,
            t.filteredPrototypes)
          ms.add(transition)
        case dc: DataChannelUI ⇒
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

  def initCapsuleAdd(w: CapsuleUI) = {
    obUI = Some(w.asInstanceOf[Widget])
    obUI.get.getActions.addAction(connectAction)
    obUI.get.getActions.addAction(dataChannelAction)
    obUI.get.getActions.addAction(createSelectAction)
    obUI.get.getActions.addAction(moveAction)
  }

  def attachEdgeWidget(e: String) = {
    val connectionWidget = new ConnectorWidget(this, dataUI.connector(e))
    connectionWidget.setEndPointShape(PointShape.SQUARE_FILLED_BIG)
    connectionWidget.getActions.addAction(ActionFactory.createPopupMenuAction(new ConnectorMenuProvider(this, connectionWidget)))
    connectionWidget.setRouter(new MoleRouter(capsuleLayer))
    connectLayer.addChild(connectionWidget)
    connectionWidget.getActions.addAction(createObjectHoverAction)
    connectionWidget
  }

  def removeSelectedWidgets = ScenesManager.selection.foreach { c ⇒
    graphScene.removeNodeWithEdges(dataUI.removeCapsuleUI(c))
    CheckData.checkMole(buildMoleScene)
  }
}
