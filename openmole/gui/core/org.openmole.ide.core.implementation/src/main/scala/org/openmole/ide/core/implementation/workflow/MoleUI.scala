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

import org.openmole.ide.core.implementation.builder.MoleFactory
import org.openmole.core.model.mole.{ ICapsule, IMole }
import concurrent.stm._
import util.{ Failure, Success }
import org.openmole.ide.misc.tools.util.ID
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, TaskDataProxyUI, Proxies }
import org.openmole.ide.core.implementation.panelsettings.MolePanelUI
import java.io.File
import org.openmole.core.model.task.PluginSet
import org.openmole.ide.core.implementation.data.DataUI
import org.openmole.core.model.data.{ Prototype, Variable, Context }

class MoleUI(var name: String) extends DataUI with ID { moleUI ⇒

  private val _startingCapsule: Ref[Option[CapsuleUI]] = Ref(Option.empty[CapsuleUI])
  private val _capsules = TMap[String, CapsuleUI]()
  private val _connectors = TMap[String, ConnectorUI]()
  private val _capsuleConnections = TMap[String, TSet[ConnectorUI]]()
  private val _plugins: Ref[List[String]] = Ref(List.empty[String])
  private val _prototypesInContext: Ref[List[(PrototypeDataProxyUI, String)]] = Ref(List.empty[(PrototypeDataProxyUI, String)])

  def plugins = _plugins.single()
  def plugins_=(v: Traversable[String]) = _plugins.single() = v.toList
  def pluginSet: PluginSet = PluginSet(plugins.map { p ⇒ new File(p) }.toSet)

  def prototypesInContext = _prototypesInContext.single()
  def prototypesInContext_=(v: List[(PrototypeDataProxyUI, String)]) = _prototypesInContext.single() = v

  @transient lazy val _cacheMole: Ref[Option[(IMole, Map[CapsuleUI, ICapsule])]] = Ref(None)

  def context = Context(prototypesInContext.map {
    case (proxy, value) ⇒ Variable(proxy.dataUI.coreObject.get.asInstanceOf[Prototype[Any]], value)
  })

  def coreClass = classOf[IMole]

  def buildPanelUI = new MolePanelUI {
    // type DATAUI = moleUI.type
    def dataUI = moleUI
  }

  def capsules = _capsules.single.toMap
  def capsule(id: String) = _capsules.single.get(id)

  def +=(c: CapsuleUI): Unit = atomic { implicit ctx ⇒
    _capsules put (c.id, c)
    _capsuleConnections.single put (c.id, TSet.empty)
  }

  def -=(c: CapsuleUI): Unit = atomic { implicit ctx ⇒
    _capsules remove (c.id)
    if (_capsuleConnections.contains(c.id)) _capsuleConnections.remove(c.id)
  }

  def contains(c: CapsuleUI) = _capsules.single.contains(c.id)

  def connectors = _connectors.single.toMap
  def connector(id: String) = _connectors.single(id)

  def +=(c: ConnectorUI): Unit = atomic { implicit ctx ⇒
    _connectors put (c.id, c)
    +=(c.source, c)
  }

  def -=(c: ConnectorUI): Unit = atomic { implicit ctx ⇒
    if (contains(c)) {
      _connectors.single remove (c.id)
      -=(c.source, c)
    }
  }

  def update(id: String, c: ConnectorUI) = _connectors.single(id) = c
  def contains(c: ConnectorUI) = _connectors.single.contains(c.id)

  def capsuleConnections = _capsuleConnections.single.toMap
  def capsuleConnections(c: CapsuleUI) = _capsuleConnections.single(c.id)

  def +=(caps: CapsuleUI, c: ConnectorUI): Unit = atomic { implicit ctx ⇒
    val l = _capsuleConnections.getOrElseUpdate(caps.id, TSet.empty)
    l += c
  }

  def -=(caps: CapsuleUI, c: ConnectorUI): Unit = atomic { implicit ctx ⇒
    val l = _capsuleConnections.getOrElseUpdate(caps.id, TSet.empty)
    if (l.contains(c)) l -= c
  }

  def invalidateCache = {
    cleanUnusedPrototypes
    _cacheMole.single() = None
  }

  def cacheMole = atomic { implicit actx ⇒
    _cacheMole() match {
      case Some(_) ⇒
      case None ⇒
        MoleFactory.buildMole(this) match {
          case Success((m, cMap, _)) ⇒
            _cacheMole() = Some((m, cMap))
          case Failure(t) ⇒
            _cacheMole() = None
        }
    }
    _cacheMole()
  }

  def cleanUnusedPrototypes = atomic { implicit actx ⇒
    val pUI = (Proxies.instance.tasks.flatMap { t ⇒
      val impl = t.dataUI.implicitPrototypes
      t.dataUI.outputs.toList ::: t.dataUI.inputs.toList ::: impl._1 ::: impl._2
    } :::
      Proxies.instance.hooks.flatMap { h ⇒
        val impl = h.dataUI.implicitPrototypes
        h.dataUI.inputs.toList ::: h.dataUI.outputs.toList ::: impl._1 ::: impl._2
      } :::
      Proxies.instance.sources.flatMap { s ⇒
        val impl = s.dataUI.implicitPrototypes
        s.dataUI.inputs.toList ::: s.dataUI.outputs.toList ::: impl._1 ::: impl._2
      }).distinct
    Proxies.instance.prototypes.diff(pUI).filter { _.generated }.foreach {
      Proxies.instance.remove
    }
  }

  def assignDefaultStartingCapsule =
    if (!startingCapsule.isDefined && !capsules.isEmpty) startingCapsule = Some(capsules.map { _._2 }.head)

  def startingCapsule_=(stCapsule: Option[CapsuleUI]): Unit = {
    _startingCapsule.single() = stCapsule
  }

  def startingCapsule = _startingCapsule.single()

  def registerCapsuleUI(cv: CapsuleUI) = {
    +=(cv)
    if (capsules.size == 1) startingCapsule = Some(cv)
    invalidateCache
  }

  def removeCapsuleUI(capsule: CapsuleUI): String = atomic { implicit ptx ⇒
    val id = capsule.id
    startingCapsule match {
      case None                  ⇒
      case Some(caps: CapsuleUI) ⇒ if (id == caps.id) startingCapsule = None
    }

    removeInAndOutcomingTransitions(capsule)

    -=(capsule)
    assignDefaultStartingCapsule
    invalidateCache
    id
  }

  def capsulesInMole = atomic { implicit ptx ⇒
    cacheMole match {
      case Some((mole: IMole, capsuleMap: Map[CapsuleUI, ICapsule])) ⇒
        val cmap = capsuleMap.map { case (k, v) ⇒ v -> k }
        mole.capsules.map { cmap }.toSet
      case _ ⇒ Iterable()
    }
  }

  def capsule(proxy: TaskDataProxyUI) = capsules.toList.filter {
    _._2.dataUI.task == Some(proxy)
  }.map {
    _._2
  }

  private def removeInAndOutcomingTransitions(capsule: CapsuleUI) =
    connectors.filter { c ⇒
      c._2.target.capsule == capsule || c._2.source == capsule
    }.foreach {
      t ⇒
        removeConnector(t._1)
    }

  def changeConnector(oldConnector: ConnectorUI,
                      connector: ConnectorUI) = atomic { implicit ctx ⇒
    update(oldConnector.id, connector)
  }

  def removeConnector(edgeID: String): Unit = removeConnector(edgeID, connector(edgeID))

  def removeConnector(edgeID: String,
                      connector: ConnectorUI): Unit = atomic { implicit ctx ⇒
    -=(connector)
    invalidateCache
  }

  def registerConnector(connector: ConnectorUI): Unit = registerConnector(connector.id, connector)

  def registerConnector(edgeID: String,
                        connector: ConnectorUI): Unit = atomic { implicit ctx ⇒
    +=(connector)
    invalidateCache
  }

  def transitions = connectors.map {
    _._2
  }.filter {
    _ match {
      case t: TransitionUI ⇒ true
      case _               ⇒ false
    }
  }.toList

  def firstCapsules(caps: List[CapsuleUI]) = caps.diff(transitions.map {
    _.target.capsule
  })

  def lastCapsules(caps: List[CapsuleUI]) = caps.diff(transitions.map {
    _.source
  })

  def connectedCapsulesFrom(from: CapsuleUI) = atomic { implicit ptx ⇒
    def connectedCapsulesFrom0(toVisit: List[CapsuleUI], connected: List[CapsuleUI]): List[CapsuleUI] = {
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

  def puzzlesCompliant: List[List[CapsuleUI]] = groups.map { puzzleCompliant }

  def puzzleCompliant(l: List[CapsuleUI]): List[CapsuleUI] = {
    if (isPuzzleCompliant(l)) l
    else List()
  }

  def isPuzzleCompliant: Boolean = isPuzzleCompliant(capsules.values.toList)

  def isPuzzleCompliant(l: List[CapsuleUI]): Boolean = {
    if (isFirstCompliant(l) && isLastCompliant(l)) true
    else false
  }

  def groups(l: List[CapsuleUI]): List[List[CapsuleUI]] = firstCapsules(l).map(connectedCapsulesFrom)

  def groups: List[List[CapsuleUI]] = groups(capsules.values.toList)

  def isFirstCompliant(firsts: List[CapsuleUI]) = if (firsts.isEmpty || firsts.size > 1) false else true

  def isLastCompliant(lasts: List[CapsuleUI]) = if (lasts.isEmpty || lasts.size > 1) false else true

  def firstCompliant(f: List[CapsuleUI]) = {
    val firsts = firstCapsules(f)
    if (isFirstCompliant(firsts)) List() else firsts
  }

  def lastCompliant(l: List[CapsuleUI]) = {
    val lasts = lastCapsules(l)
    if (isLastCompliant(lasts)) List() else lasts
  }
}
