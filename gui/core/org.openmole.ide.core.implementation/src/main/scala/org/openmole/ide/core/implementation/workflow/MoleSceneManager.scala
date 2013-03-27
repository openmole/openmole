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
import org.openmole.ide.core.model.dataproxy.{ IPrototypeDataProxyUI, ITaskDataProxyUI }
import org.openmole.ide.core.implementation.builder.MoleFactory
import org.openmole.core.model.mole.{ ICapsule, IMole }
import concurrent.stm._
import util.{ Failure, Success }
import org.openmole.ide.misc.tools.util.ID
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.dataproxy.Proxys
import java.util.concurrent.atomic.AtomicInteger

class MoleSceneManager(var name: String) extends IMoleSceneManager with ID {

  var startingCapsule: Option[ICapsuleUI] = None
  //var _capsules = new HashMap[String, ICapsuleUI]
  private val _capsules = TMap[String, ICapsuleUI]()
  var _connectors = new HashMap[String, IConnectorUI]
  var capsuleConnections = new HashMap[ICapsuleDataUI, HashSet[IConnectorUI]]
  //var nodeID = new AtomicInteger
  var edgeID = 0

  var dataUI: IMoleDataUI = new MoleDataUI
  val _cacheMole: Ref[Option[(IMole, Map[ICapsuleUI, ICapsule], Map[IPrototypeDataProxyUI, Prototype[_]])]] = Ref(None)

  def capsules = _capsules.single.toMap
  def capsules(id: String) = _capsules.single.get(id)
  def +=(c: ICapsuleUI) = _capsules.single put (c.dataUI.id, c)
  def -=(c: ICapsuleUI) = _capsules.single remove (c.dataUI.id)
  def contains(c: ICapsuleUI) = _capsules.single.contains(c.dataUI.id)

  def refreshCache = {
    invalidateCache
    cacheMole
    cleanUnusedPrototypes
  }

  def invalidateCache = {
    _cacheMole.single() = None
  }

  def cacheMole = atomic { implicit actx ⇒
    _cacheMole() match {
      case Some(_) ⇒
      case None ⇒
        _cacheMole() = MoleFactory.buildMole(this) match {
          case Success((m, cMap, pMap, _)) ⇒
            Some((m, cMap, pMap))
          case Failure(t) ⇒
            None
        }
    }
    _cacheMole()
  }

  def cleanUnusedPrototypes
  = atomic { implicit actx ⇒
    val pUI = (Proxys.tasks.flatMap { t ⇒
      val impl = t.dataUI.implicitPrototypes
      t.dataUI.outputs ::: t.dataUI.inputs ::: impl._1 ::: impl._2 } :::
      Proxys.hooks.flatMap { h ⇒
      val impl = h.dataUI.implicitPrototypes
      h.dataUI.inputs ::: h.dataUI.outputs ::: impl._1 ::: impl._2} :::
      Proxys.sources.flatMap { s ⇒
      val impl = s.dataUI.implicitPrototypes
        s.dataUI.inputs ::: s.dataUI.outputs ::: impl._1 ::: impl._2}).distinct
    Proxys.prototypes.diff(pUI).foreach {
      p ⇒ if (p.generated) Proxys -= p
    }
  }

  def assignDefaultStartingCapsule =
    if (!startingCapsule.isDefined && !capsules.isEmpty) setStartingCapsule(capsules.map { _._2 }.head)

  def setStartingCapsule(stCapsule: ICapsuleUI) = {
    startingCapsule match {
      case Some(x: ICapsuleUI) ⇒ x.defineAsStartingCapsule(false)
      case None ⇒
    }
    startingCapsule = Some(stCapsule)
    startingCapsule.get.defineAsStartingCapsule(true)
  }

  //def getNodeID: String = "node" + nodeID

  def getEdgeID: String = "edge" + edgeID

  override def registerCapsuleUI(cv: ICapsuleUI) = {
    // nodeID += 1
    // _capsules += getNodeID -> cv
    +=(cv)
    if (capsules.size == 1) setStartingCapsule(cv)
    invalidateCache
  }

  def removeCapsuleUI(capsule: ICapsuleUI): String = {
    val id = capsule.dataUI.id
    startingCapsule match {
      case None ⇒
      case Some(caps: ICapsuleUI) ⇒ if (id == caps.dataUI.id) startingCapsule = None
    }
    capsuleConnections.getOrElse(capsule.dataUI, List()).foreach {
      x ⇒ removeConnector(connectorID(x))
    }

    removeIncomingTransitions(capsule)

    -=(capsule)
    assignDefaultStartingCapsule
    invalidateCache
    id
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

  def capsule(proxy: ITaskDataProxyUI) = capsules.toList.filter {
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
    if (capsuleConnections.contains(connector.source.dataUI)) {
      capsuleConnections(connector.source.dataUI) -= connector
      if (capsuleConnections(connector.source.dataUI).isEmpty) capsuleConnections -= connector.source.dataUI
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
