/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.workflow

import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.data.IMoleDataUI
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.core.model.mole.{ ICapsule, IMole }
import org.openmole.ide.misc.tools.util._

trait IMoleSceneManager {
  override def toString = name

  def id: ID.Type

  def name: String

  def name_=(n: String)

  def cacheMole: Option[(IMole, Map[ICapsuleUI, ICapsule])]

  def invalidateCache: Unit

  def dataUI: IMoleDataUI

  def dataUI_=(dataUI: IMoleDataUI)

  def startingCapsule_=(n: Option[ICapsuleUI])

  def getNodeID: String

  def getEdgeID: String

  def connectorID(dc: IConnectorUI): String

  def capsuleID(cv: ICapsuleUI): String

  def capsules: Map[String, ICapsuleUI]

  def capsule(proxy: ITaskDataProxyUI): List[ICapsuleUI]

  def startingCapsule: Option[ICapsuleUI]

  def capsuleConnections: HashMap[ICapsuleDataUI, HashSet[IConnectorUI]]

  def removeCapsuleUI(nodeID: String): String

  def removeCapsuleUI(capslue: ICapsuleUI): String

  def registerCapsuleUI(cv: ICapsuleUI): Unit

  def setStartingCapsule(capsule: ICapsuleUI)

  def connectors: Iterable[IConnectorUI]

  def connector(dID: String): IConnectorUI

  def removeConnector(edgeID: String)

  def registerConnector(connector: IConnectorUI): Boolean

  def registerConnector(edgeID: String,
                        connector: IConnectorUI): Boolean

  def changeConnector(oldConnector: IConnectorUI,
                      connector: IConnectorUI)

  def capsulesInMole: Iterable[ICapsuleDataUI]

  def firstCapsules(caps: List[ICapsuleUI]): List[ICapsuleUI]

  def lastCapsules(caps: List[ICapsuleUI]): List[ICapsuleUI]

  def puzzlesCompliant: List[List[ICapsuleUI]] = groups.map { puzzleCompliant }

  def puzzleCompliant(l: List[ICapsuleUI]): List[ICapsuleUI] = {
    if (isPuzzleCompliant(l)) l
    else List()
  }

  def isPuzzleCompliant: Boolean = isPuzzleCompliant(capsules.values.toList)

  def isPuzzleCompliant(l: List[ICapsuleUI]): Boolean = {
    if (isFirstCompliant(l) && isLastCompliant(l)) true
    else false
  }

  def groups(l: List[ICapsuleUI]): List[List[ICapsuleUI]] = firstCapsules(l).map(connectedCapsulesFrom)

  def groups: List[List[ICapsuleUI]] = groups(capsules.values.toList)

  def connectedCapsulesFrom(from: ICapsuleUI): List[ICapsuleUI] = {
    def connectedCapsulesFrom0(toVisit: List[ICapsuleUI], connected: List[ICapsuleUI]): List[ICapsuleUI] = {
      if (toVisit.isEmpty) connected
      else {
        val head = toVisit.head
        val connectors = capsuleConnections(head.dataUI)
        connectedCapsulesFrom0(toVisit.tail ::: connectors.map {
          _.target.capsule
        }.toList, connected :+ head)
      }
    }
    connectedCapsulesFrom0(List(from), List())
  }

  def isFirstCompliant(firsts: List[ICapsuleUI]) = if (firsts.isEmpty || firsts.size > 1) false else true

  def isLastCompliant(lasts: List[ICapsuleUI]) = if (lasts.isEmpty || lasts.size > 1) false else true

  def firstCompliant(f: List[ICapsuleUI]) = {
    val firsts = firstCapsules(f)
    if (isFirstCompliant(firsts)) List() else firsts
  }

  def lastCompliant(l: List[ICapsuleUI]) = {
    val lasts = lastCapsules(l)
    if (isLastCompliant(lasts)) List() else lasts
  }
}
