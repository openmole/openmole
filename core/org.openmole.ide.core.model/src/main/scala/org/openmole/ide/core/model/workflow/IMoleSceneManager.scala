/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.data.IMoleDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

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

  def dataChannelID(dc: IDataChannelUI): String

  def getDataChannelID: String

  def capsuleID(cv: ICapsuleUI): String

  def capsules: DualHashBidiMap[String, ICapsuleUI]

  def startingCapsule: Option[ICapsuleUI]

  def capsuleConnections: HashMap[ICapsuleDataUI, HashSet[ITransitionUI]]

  def removeCapsuleUI(nodeID: String): String

  def removeCapsuleUI(capslue: ICapsuleUI): String

  def registerCapsuleUI(cv: ICapsuleUI): Unit

  def setStartingCapsule(capsule: ICapsuleUI)

  def transitions: Iterable[ITransitionUI]

  def dataChannels: Iterable[IDataChannelUI]

  def transition(edgeID: String): ITransitionUI

  def dataChannel(dID: String): IDataChannelUI

  def removeTransition(edgeID: String)

  def removeDataChannel(id: String)

  def registerTransition(s: ICapsuleUI,
                         t: IInputSlotWidget,
                         transitionType: TransitionType.Value,
                         cond: Option[String],
                         filtered: List[IPrototypeDataProxyUI]): Boolean

  def registerTransition(edgeID: String,
                         s: ICapsuleUI,
                         t: IInputSlotWidget,
                         transitionType: TransitionType.Value,
                         cond: Option[String],
                         filtered: List[IPrototypeDataProxyUI]): Boolean

  def registerDataChannel(id: String,
                          source: ICapsuleUI,
                          target: IInputSlotWidget,
                          prototypes: List[IPrototypeDataProxyUI]): Boolean

  def registerDataChannel(source: ICapsuleUI,
                          target: IInputSlotWidget,
                          prototypes: List[IPrototypeDataProxyUI]): Boolean
}
