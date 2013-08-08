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

package org.openmole.ide.core.implementation.workflow

import concurrent.stm._
import org.openmole.core.model.mole.{ ICapsule, IMole }
import org.openmole.ide.misc.tools.util._
import org.openmole.core.model.task.PluginSet
import java.io.File
import org.openmole.ide.core.implementation.data.DataUI
import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyUI

trait IMoleUI extends DataUI with ID {

  override def toString: String = name

  def coreClass = classOf[IMole]

  def plugins: Iterable[String]
  def plugins_=(v: Traversable[String])

  def pluginSet: PluginSet = PluginSet(plugins.map { p ⇒ new File(p) }.toSet)

  def name: String

  def name_=(n: String)

  def cacheMole: Option[(IMole, Map[CapsuleUI, ICapsule])]

  def invalidateCache: Unit

  def refreshCache: Unit

  def startingCapsule_=(n: Option[CapsuleUI])

  def capsules: Map[String, CapsuleUI]

  def capsule(proxy: TaskDataProxyUI): List[CapsuleUI]

  def startingCapsule: Option[CapsuleUI]

  def capsuleConnections: Map[String, TSet[ConnectorUI]]

  def removeCapsuleUI(capslue: CapsuleUI): String

  def registerCapsuleUI(cv: CapsuleUI): Unit

  def connectors: Map[String, ConnectorUI]

  def connector(dID: String): ConnectorUI

  def removeConnector(edgeID: String)

  def registerConnector(connector: ConnectorUI): Unit

  def registerConnector(edgeID: String,
                        connector: ConnectorUI): Unit

  def changeConnector(oldConnector: ConnectorUI,
                      connector: ConnectorUI)

  def capsulesInMole: Iterable[CapsuleUI]

  def firstCapsules(caps: List[CapsuleUI]): List[CapsuleUI]

  def lastCapsules(caps: List[CapsuleUI]): List[CapsuleUI]

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

  def connectedCapsulesFrom(from: CapsuleUI): List[CapsuleUI]

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
