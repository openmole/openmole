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

trait IMoleSceneManager {
  override def toString = name

  def id: Int

  def name: String

  def name_=(n: String)

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

  def capsuleGroups: List[List[ICapsuleUI]]

  def firstCapsules(caps: List[ICapsuleUI]): List[ICapsuleUI]

  def lastCapsules(caps: List[ICapsuleUI]): List[ICapsuleUI]
}
