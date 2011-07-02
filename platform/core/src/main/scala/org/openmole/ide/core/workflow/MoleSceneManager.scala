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

package org.openmole.ide.core.workflow

import scala.collection.mutable.HashMap
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.ide.core.commons.TransitionType
import org.openmole.ide.core.commons.Constants._
import scala.collection.JavaConversions._
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

class MoleSceneManager(var startingCapsule: Option[ICapsuleUI]= None) {

  var capsules= new DualHashBidiMap[String, ICapsuleUI]
  var transitions= new DualHashBidiMap[String, TransitionUI]
  var capsuleConnections= new HashMap[ICapsuleUI, HashSet[TransitionUI]]
  var nodeID= 0
  var edgeID= 0
  var name: Option[String]= None
  
  def setStartingCapsule(stCapsule: ICapsuleUI) = {
    if (startingCapsule.isDefined) startingCapsule.get.addInputSlot(false)
    startingCapsule= Some(stCapsule)
    startingCapsule.get.addInputSlot(true)
    removeTransitonsBeforeStartingCapsule
  }
  
  def getNodeID: String= "node" + nodeID
  
  def getEdgeID: String= "edge" + edgeID
  
  def registerCapsuleUI(cv: ICapsuleUI) = {
    nodeID+= 1
    capsules.put(getNodeID,cv)
    capsuleConnections+= cv-> HashSet.empty[TransitionUI]
  }
  
  def removeCapsuleUI(nodeID: String) = {
    //remove following transitions
    capsuleConnections(capsules.get(nodeID)).foreach(transitions.removeValue(_))
    
    //remove incoming transitions
    removeIncomingTransitions(capsules.get(nodeID))
    
    capsules.remove(nodeID)
  }
  
  def capsuleID(cv: ICapsuleUI) = capsules.getKey(cv)
  
  def getTransitions= transitions.values 
  
  def getTransition(edgeID: String) = transitions.get(edgeID)
  
  private def removeIncomingTransitions(capsule: ICapsuleUI) = transitions.foreach(t => {if (t._2.target.capsule.equals(capsule)) removeTransition(t._1)})
  
  
  def removeTransition(edge: String) = transitions.remove(edge)
  
  def registerTransition(s: ICapsuleUI, t:ISlotWidget,transitionType: TransitionType.Value,cond: Option[String]): Boolean = {
    edgeID+= 1
    registerTransition(getEdgeID,s,t,transitionType,cond)
  }
  
  //def registerTransition(edgeID: String,transition: TransitionUI): Unit = {
  def registerTransition(edgeID: String,s: ICapsuleUI, t:ISlotWidget,transitionType: TransitionType.Value,cond: Option[String]): Boolean = {
    if (!isTransition(s, t.capsule)) {
      val transition = new TransitionUI(s,t,transitionType,cond)
      transitions.put(edgeID, transition)
      capsuleConnections(transition.source)+= transition
      return true
    }
    false
    // capsuleConnections(transition.target.capsule)+= transition
  }
  
  private def isTransition(source: ICapsuleUI, target: ICapsuleUI): Boolean = {
    capsuleConnections(source).foreach(trans=> {if (trans.target.capsule.equals(target)) return true})
    false
  }
  
  private def removeTransitonsBeforeStartingCapsule = {
    val l = new HashSet[String]
    transitions.foreach{t=> 
      if (t._2.target.capsule.equals(startingCapsule.get)) l += t._1}
    l.foreach{removeTransition(_)}
    l
  } 
}
