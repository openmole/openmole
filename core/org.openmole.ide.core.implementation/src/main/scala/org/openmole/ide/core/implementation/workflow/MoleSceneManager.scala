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
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.data.IMoleDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
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
  var transitionMap = new DualHashBidiMap[String, ITransitionUI]
  var dataChannelMap = new DualHashBidiMap[String, IDataChannelUI]
  var capsuleConnections = new HashMap[ICapsuleDataUI, HashSet[ITransitionUI]]
  var nodeID = 0
  var edgeID = 0
  var dataChannelID = 0

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

  def getDataChannelID: String = "dc" + dataChannelID

  override def registerCapsuleUI(cv: ICapsuleUI) = {
    nodeID += 1
    capsules.put(getNodeID, cv)
    if (capsules.size == 1) startingCapsule = Some(cv)
    capsuleConnections += cv.dataUI -> HashSet.empty[ITransitionUI]
  }

  def removeCapsuleUI(capsule: ICapsuleUI): String = removeCapsuleUI(capsuleID(capsule))

  def removeCapsuleUI(nodeID: String): String = {
    startingCapsule match {
      case None ⇒
      case Some(caps: ICapsuleUI) ⇒ if (capsules.get(nodeID) == caps) startingCapsule = None
    }

    //remove following transitionMap
    capsuleConnections(capsules.get(nodeID).dataUI).foreach { x ⇒ transitionMap.removeValue(x) }
    capsuleConnections -= capsules.get(nodeID).dataUI

    //remove incoming transitionMap
    removeIncomingTransitions(capsules.get(nodeID))
    removeDataChannel(capsules.get(nodeID))

    capsules.remove(nodeID)
    nodeID
  }

  def dataChannelID(dc: IDataChannelUI) = dataChannelMap.getKey(dc)

  def capsuleID(cv: ICapsuleUI) = capsules.getKey(cv)

  def transitions = transitionMap.values

  def dataChannels = dataChannelMap.values

  def transition(eID: String) = transitionMap.get(eID)

  def dataChannel(dID: String) = dataChannelMap.get(dID)

  private def removeIncomingTransitions(capsule: ICapsuleUI) =
    transitionMap.filter { _._2.target.capsule == capsule }.foreach { t ⇒
      removeTransition(t._1)
    }

  def removeTransition(edgeID: String) = removeTransition(edgeID, transitionMap.get(edgeID))

  def removeTransition(edgeID: String,
                       transition: ITransitionUI) = {
    transitionMap.remove(edgeID)
    capsuleConnections(transition.source.dataUI) -= transition
  }

  def removeDataChannel(id: String): Unit = dataChannelMap.remove(id)

  def removeDataChannel(capsule: ICapsuleUI): Unit = {
    dataChannelMap.filter { case (k, v) ⇒ (v.source == capsule || v.target == capsule) }.
      foreach { m ⇒ removeDataChannel(m._1) }
  }

  def registerDataChannel(source: ICapsuleUI,
                          target: IInputSlotWidget,
                          filetered: List[IPrototypeDataProxyUI] = List.empty): Boolean = {
    dataChannelID += 1
    registerDataChannel(getDataChannelID, source, target, filetered)
  }

  def registerDataChannel(id: String,
                          source: ICapsuleUI,
                          target: IInputSlotWidget,
                          filetered: List[IPrototypeDataProxyUI]): Boolean = {
    if (!dataChannelMap.keys.contains(id)) { dataChannelMap.put(id, new DataChannelUI(source, target, filetered)); return true }
    false
  }

  def registerTransition(s: ICapsuleUI,
                         t: IInputSlotWidget,
                         transitionType: TransitionType.Value,
                         cond: Option[String],
                         filtered: List[IPrototypeDataProxyUI] = List.empty): Boolean = {
    edgeID += 1
    registerTransition(getEdgeID, s, t, transitionType, cond, filtered)
  }

  def registerTransition(edgeID: String,
                         s: ICapsuleUI,
                         t: IInputSlotWidget,
                         transitionType: TransitionType.Value,
                         cond: Option[String],
                         filtered: List[IPrototypeDataProxyUI]): Boolean = {
    if (!transitionMap.keys.contains(edgeID)) {
      val transition = new TransitionUI(s, t, transitionType, cond, filtered)
      transitionMap.put(edgeID, transition)
      capsuleConnections(transition.source.dataUI) += transition
      return true
    }
    false
  }
}
