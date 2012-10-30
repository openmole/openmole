/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.data.IMoleDataUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import org.openmole.ide.core.implementation.data.MoleDataUI
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet

class MoleSceneManager(var name: String) extends IMoleSceneManager {

  val id = ScenesManager.countBuild.getAndIncrement
  var startingCapsule: Option[ICapsuleUI] = None
  var _capsules = new HashMap[String, ICapsuleUI]
  var _connectors = new HashMap[String, IConnectorUI]
  var capsuleConnections = new HashMap[ICapsuleDataUI, HashSet[IConnectorUI]]
  var nodeID = 0
  var edgeID = 0

  var dataUI: IMoleDataUI = new MoleDataUI

  def capsules = _capsules.toMap

  def setStartingCapsule(stCapsule: ICapsuleUI) = {
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
    _capsules += getNodeID -> cv
    if (_capsules.size == 1) setStartingCapsule(cv)
    capsuleConnections += cv.dataUI -> HashSet.empty[IConnectorUI]
  }

  def removeCapsuleUI(capsule: ICapsuleUI): String = removeCapsuleUI(capsuleID(capsule))

  def removeCapsuleUI(nodeID: String): String = {
    startingCapsule match {
      case None ⇒
      case Some(caps: ICapsuleUI) ⇒ if (capsules(nodeID) == caps) startingCapsule = None
    }

    capsuleConnections(_capsules(nodeID).dataUI).foreach { x ⇒ connectorIDs -= x }
    capsuleConnections -= _capsules(nodeID).dataUI

    removeIncomingTransitions(_capsules(nodeID))

    _capsules.remove(nodeID)
    nodeID
  }

  def capsulesInMole = {
    val capsuleSet = new HashSet[ICapsuleDataUI]
    capsuleConnections.foreach {
      case (capsuleData, connections) ⇒
        capsuleSet += capsuleData
        connections.foreach { c ⇒
          capsuleSet += c.source.dataUI
          capsuleSet += c.target.capsule.dataUI
        }
    }
    capsuleSet
  }

  def connector(cID: String) = _connectors(cID)

  def connectorIDs = _connectors.map { case (k, v) ⇒ v -> k }

  def connectorID(c: IConnectorUI) = connectorIDs(c)

  def connectors = _connectors.values

  def capsuleID(cv: ICapsuleUI) = _capsules.map { case (k, v) ⇒ v -> k }.get(cv).get

  private def removeIncomingTransitions(capsule: ICapsuleUI) =
    _connectors.filter { _._2.target.capsule == capsule }.foreach { t ⇒
      removeConnector(t._1)
    }

  def changeConnector(oldConnector: IConnectorUI,
                      connector: IConnectorUI) = {
    _connectors(connectorID(oldConnector)) = connector
    capsuleConnections(connector.source.dataUI) -= oldConnector
    capsuleConnections(connector.source.dataUI) += connector
  }

  def removeConnector(edgeID: String): Unit = removeConnector(edgeID, _connectors(edgeID))

  def removeConnector(edgeID: String,
                      connector: IConnectorUI): Unit = {
    _connectors.remove(edgeID)

    capsuleConnections.contains(connector.source.dataUI) match {
      case true ⇒ capsuleConnections(connector.source.dataUI) -= connector
      case _ ⇒
    }
  }

  def registerConnector(connector: IConnectorUI): Boolean = {
    edgeID += 1
    registerConnector(getEdgeID, connector)
  }

  def registerConnector(edgeID: String,
                        connector: IConnectorUI): Boolean = {
    capsuleConnections(connector.source.dataUI) += connector
    if (!_connectors.keys.contains(edgeID)) {
      _connectors += edgeID -> connector
      return true
    }
    false
  }
}
