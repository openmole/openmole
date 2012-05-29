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

package org.openmole.ide.core.implementation.workflow

import scala.collection.mutable.HashMap
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.data.IMoleDataUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.ide.core.implementation.data.MoleDataUI
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet

class MoleSceneManager(var name: String,
                       val id: Int) extends IMoleSceneManager {

  var startingCapsule: Option[ICapsuleUI] = None
  var capsules = new DualHashBidiMap[String, ICapsuleUI]
  var connectorMap = new DualHashBidiMap[String, IConnectorUI]
  var capsuleConnections = new HashMap[ICapsuleDataUI, HashSet[IConnectorUI]]
  var nodeID = 0
  var edgeID = 0

  var dataUI: IMoleDataUI = new MoleDataUI

  override def setStartingCapsule(stCapsule: ICapsuleUI) = {
    startingCapsule match {
      case Some(x: ICapsuleUI) ⇒ x.defineAsStartingCapsule(false)
      case None ⇒
    }
    startingCapsule = Some(stCapsule)
    startingCapsule.get.defineAsStartingCapsule(true)
  }

  def getNodeID: String = "node" + nodeID

  def getEdgeID: String = "edge" + edgeID

  override def registerCapsuleUI(cv: ICapsuleUI) = {
    nodeID += 1
    capsules.put(getNodeID, cv)
    if (capsules.size == 1) startingCapsule = Some(cv)
    capsuleConnections += cv.dataUI -> HashSet.empty[IConnectorUI]
  }

  def removeCapsuleUI(capsule: ICapsuleUI): String = removeCapsuleUI(capsuleID(capsule))

  def removeCapsuleUI(nodeID: String): String = {
    startingCapsule match {
      case None ⇒
      case Some(caps: ICapsuleUI) ⇒ if (capsules.get(nodeID) == caps) startingCapsule = None
    }

    capsuleConnections(capsules.get(nodeID).dataUI).foreach { x ⇒ connectorMap.removeValue(x) }
    capsuleConnections -= capsules.get(nodeID).dataUI

    removeIncomingTransitions(capsules.get(nodeID))

    capsules.remove(nodeID)
    nodeID
  }

  def connector(cID: String) = connectorMap.get(cID)

  def connectorID(c: IConnectorUI) = connectorMap.getKey(c)

  def connectors = connectorMap.values

  def capsuleID(cv: ICapsuleUI) = capsules.getKey(cv)

  private def removeIncomingTransitions(capsule: ICapsuleUI) =
    connectorMap.filter { _._2.target.capsule == capsule }.foreach { t ⇒
      removeConnector(t._1)
    }

  def removeConnector(edgeID: String): Unit = removeConnector(edgeID, connectorMap.get(edgeID))

  def removeConnector(edgeID: String,
                      connector: IConnectorUI): Unit = {
    connectorMap.remove(edgeID)
    capsuleConnections(connector.source.dataUI) -= connector
  }

  def registerConnector(connector: IConnectorUI): Boolean = {
    edgeID += 1
    registerConnector(getEdgeID, connector)
  }

  def registerConnector(edgeID: String,
                        connector: IConnectorUI): Boolean = {
    if (!connectorMap.keys.contains(edgeID)) {
      connectorMap.put(edgeID, connector)
      capsuleConnections(connector.source.dataUI) += connector
      return true
    }
    false
  }
}
