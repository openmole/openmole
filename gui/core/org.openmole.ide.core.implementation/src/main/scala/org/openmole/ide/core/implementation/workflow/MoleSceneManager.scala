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

import org.openmole.ide.core.model.data.IMoleDataUI
import org.openmole.ide.core.implementation.data.MoleDataUI
import org.openmole.ide.core.model.workflow._
import scala.collection.mutable.HashSet
import org.openmole.ide.core.model.dataproxy.{ IPrototypeDataProxyUI, ITaskDataProxyUI }
import org.openmole.ide.core.implementation.builder.MoleFactory
import org.openmole.core.model.mole.{ ICapsule, IMole }
import concurrent.stm._
import util.{ Failure, Success }
import org.openmole.ide.misc.tools.util.ID
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.dataproxy.Proxys

class MoleSceneManager(var name: String) extends IMoleSceneManager with ID {

  var startingCapsule: Option[ICapsuleUI] = None
  private val _capsules = TMap[String, ICapsuleUI]()
  private val _connectors = TMap[String, IConnectorUI]()
  private val _capsuleConnections = TMap[String, TSet[IConnectorUI]]()

  var dataUI: IMoleDataUI = new MoleDataUI
  val _cacheMole: Ref[Option[(IMole, Map[ICapsuleUI, ICapsule], Map[IPrototypeDataProxyUI, Prototype[_]])]] = Ref(None)

  def capsules = _capsules.single.toMap
  def capsule(id: String) = _capsules.single.get(id)
  def +=(c: ICapsuleUI): Unit = atomic { implicit ctx ⇒
    _capsules.single put (c.id, c)
    _capsuleConnections.single put (c.id, TSet.empty)
  }
  def -=(c: ICapsuleUI): Unit = atomic { implicit ctx ⇒
    _capsules remove (c.id)
    if (_capsuleConnections.contains(c.id)) _capsuleConnections.remove(c.id)
  }
  def contains(c: ICapsuleUI) = _capsules.single.contains(c.id)

  def connectors = _connectors.single.toMap
  def connector(id: String) = _connectors.single(id)
  def +=(c: IConnectorUI): Unit = {
    _connectors.single put (c.id, c)
    +=(c.source, c)
  }
  def -=(c: IConnectorUI): Unit = atomic { implicit ctx ⇒
    if (contains(c)) {
      _connectors.single remove (c.id)
      -=(c.source, c)
    }
  }
  def update(id: String, c: IConnectorUI) = _connectors.single(id) = c
  def contains(c: IConnectorUI) = _connectors.single.contains(c.id)

  def capsuleConnections = _capsuleConnections.single.toMap
  def capsuleConnections(c: ICapsuleUI) = _capsuleConnections.single(c.id)
  def +=(caps: ICapsuleUI, c: IConnectorUI): Unit = atomic { implicit ctx ⇒
    val l = _capsuleConnections.getOrElseUpdate(caps.id, TSet.empty)
    l += c
  }
  def -=(caps: ICapsuleUI, c: IConnectorUI): Unit = atomic { implicit ctx ⇒
    val l = _capsuleConnections.getOrElseUpdate(caps.id, TSet.empty)
    if (l.contains(c)) l -= c
  }

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

  def cleanUnusedPrototypes = atomic { implicit actx ⇒
    val pUI = (Proxys.tasks.flatMap { t ⇒
      val impl = t.dataUI.implicitPrototypes
      t.dataUI.outputs ::: t.dataUI.inputs ::: impl._1 ::: impl._2
    } :::
      Proxys.hooks.flatMap { h ⇒
        val impl = h.dataUI.implicitPrototypes
        h.dataUI.inputs ::: h.dataUI.outputs ::: impl._1 ::: impl._2
      } :::
      Proxys.sources.flatMap { s ⇒
        val impl = s.dataUI.implicitPrototypes
        s.dataUI.inputs ::: s.dataUI.outputs ::: impl._1 ::: impl._2
      }).distinct
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

  override def registerCapsuleUI(cv: ICapsuleUI) = {
    +=(cv)
    if (capsules.size == 1) setStartingCapsule(cv)
    invalidateCache
  }

  def removeCapsuleUI(capsule: ICapsuleUI): String = atomic { implicit ptx ⇒
    val id = capsule.id
    startingCapsule match {
      case None ⇒
      case Some(caps: ICapsuleUI) ⇒ if (id == caps.id) startingCapsule = None
    }

    removeIncomingTransitions(capsule)

    -=(capsule)
    assignDefaultStartingCapsule
    invalidateCache
    id
  }

  /*def capsuleInMole = atomic { implicit ptx ⇒
    capsuleConnections.foldLeft(Set.empty) { (acc, cc) ⇒
      acc ++ cc._2.toSet.foldLeft(Set.empty) { (acc2, c) ⇒
        acc2 ++ Set(c.source, c.target.capsule)
      }
    }
  }  */

  def capsulesInMole = atomic { implicit ptx ⇒
    val capsuleSet = new HashSet[ICapsuleUI]
    capsuleConnections.foreach {
      case (_, connections) ⇒
        connections.foreach {
          c ⇒
            capsuleSet += c.source
            capsuleSet += c.target.capsule
        }
    }
    capsuleSet
  }

  def capsule(proxy: ITaskDataProxyUI) = capsules.toList.filter {
    _._2.dataUI.task == Some(proxy)
  }.map {
    _._2
  }

  private def removeIncomingTransitions(capsule: ICapsuleUI) =
    connectors.filter {
      _._2.target.capsule == capsule
    }.foreach {
      t ⇒
        removeConnector(t._1)
    }

  def changeConnector(oldConnector: IConnectorUI,
                      connector: IConnectorUI) = {
    update(oldConnector.id, connector)
    -=(oldConnector)
    +=(connector)
  }

  def removeConnector(edgeID: String): Unit = removeConnector(edgeID, connector(edgeID))

  def removeConnector(edgeID: String,
                      connector: IConnectorUI): Unit = {
    -=(connector)
    invalidateCache
  }

  def registerConnector(connector: IConnectorUI) = registerConnector(connector.id, connector)

  def registerConnector(edgeID: String,
                        connector: IConnectorUI) = atomic { implicit ctx ⇒
    +=(connector)
    invalidateCache
  }

  def transitions = connectors.map {
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

  def connectedCapsulesFrom(from: ICapsuleUI) = atomic { implicit ptx ⇒
    def connectedCapsulesFrom0(toVisit: List[ICapsuleUI], connected: List[ICapsuleUI]): List[ICapsuleUI] = {
      if (toVisit.isEmpty) connected
      else {
        val head = toVisit.head
        val conns = capsuleConnections.getOrElse(head.id, TSet.empty)
        connectedCapsulesFrom0(toVisit.tail ::: conns.toList.map {
          _.target.capsule
        }.toList, connected :+ head)
      }
    }
    connectedCapsulesFrom0(List(from), List())
  }
}
