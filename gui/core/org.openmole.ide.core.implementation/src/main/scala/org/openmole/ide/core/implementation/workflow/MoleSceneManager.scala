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
import org.openmole.ide.core.implementation.data.MoleDataUI
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.workflow._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.implementation.builder.MoleFactory
import org.openmole.core.model.mole.{ ICapsule, IMole }
import concurrent.stm._
import util.{ Failure, Success }
import org.openmole.ide.misc.tools.util.ID

class MoleSceneManager(var name: String) extends IMoleSceneManager with ID {

  var startingCapsule: Option[ICapsuleUI] = None
  var _capsules = new HashMap[String, ICapsuleUI]
  var _connectors = new HashMap[String, IConnectorUI]
  var capsuleConnections = new HashMap[ICapsuleDataUI, HashSet[IConnectorUI]]
  var nodeID = 0
  var edgeID = 0

  var dataUI: IMoleDataUI = new MoleDataUI

  val _cacheMole: Ref[Option[(IMole, Map[ICapsuleUI, ICapsule])]] = Ref(None)

  def invalidateCache = _cacheMole.single() = None

  def cacheMole = atomic { implicit actx ⇒
    _cacheMole() match {
      case Some(_) ⇒
      case None ⇒
        _cacheMole() = MoleFactory.buildMole(this) match {
          case Success((m, cMap, _, _)) ⇒ Some((m, cMap))
          case Failure(t) ⇒
            None
        }
    }
    _cacheMole()
  }

  def capsules = _capsules.toMap

  def assignDefaultStartingCapsule =
    if (!startingCapsule.isDefined) setStartingCapsule(_capsules.values.head)

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
    invalidateCache
  }

  def removeCapsuleUI(capsule: ICapsuleUI): String = removeCapsuleUI(capsuleID(capsule))

  def removeCapsuleUI(nodeID: String): String = {
    startingCapsule match {
      case None ⇒
      case Some(caps: ICapsuleUI) ⇒ if (capsules(nodeID) == caps) startingCapsule = None
    }
    capsuleConnections(_capsules(nodeID).dataUI).foreach {
      x ⇒
        removeConnector(connectorID(x))
    }
    capsuleConnections -= _capsules(nodeID).dataUI

    removeIncomingTransitions(_capsules(nodeID))

    _capsules.remove(nodeID)
    assignDefaultStartingCapsule
    invalidateCache
    nodeID
  }

  def capsulesInMole = {
    val capsuleSet = new HashSet[ICapsuleDataUI]
    capsuleConnections.foreach {
      case (capsuleData, connections) ⇒
        capsuleSet += capsuleData
        connections.foreach {
          c ⇒
            capsuleSet += c.source.dataUI
            capsuleSet += c.target.capsule.dataUI
        }
    }
    capsuleSet
  }

  def connector(cID: String) = _connectors(cID)

  private def connectorIDs = _connectors.map {
    case (k, v) ⇒ v -> k
  }

  def connectorID(c: IConnectorUI) = connectorIDs(c)

  def connectors = _connectors.values

  def capsuleID(cv: ICapsuleUI) = _capsules.map {
    case (k, v) ⇒ v -> k
  }.get(cv).get

  def capsule(proxy: ITaskDataProxyUI) = _capsules.toList.filter {
    _._2.dataUI.task == Some(proxy)
  }.map {
    _._2
  }

  private def removeIncomingTransitions(capsule: ICapsuleUI) =
    _connectors.filter {
      _._2.target.capsule == capsule
    }.foreach {
      t ⇒
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
    invalidateCache
  }

  def registerConnector(connector: IConnectorUI): Boolean = {
    edgeID += 1
    registerConnector(getEdgeID, connector)
  }

  def registerConnector(edgeID: String,
                        connector: IConnectorUI): Boolean = {
    capsuleConnections.getOrElseUpdate(connector.source.dataUI, HashSet(connector))
    if (!_connectors.keys.contains(edgeID)) {
      _connectors += edgeID -> connector
      return true
    }
    invalidateCache
    false
  }

  def transitions = _connectors.map {
    _._2
  }.filter {
    _ match {
      case t: ITransitionUI ⇒ true
      case _ ⇒ false
    }
  }.toList

  def firstCapsules(caps: List[ICapsuleUI]) = caps.diff(transitions.map {
    _.target.capsule
  })

  def lastCapsules(caps: List[ICapsuleUI]) = caps.diff(transitions.map {
    _.source
  })

}
