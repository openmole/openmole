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

package org.openmole.ide.core.workflow.implementation

import scala.collection.mutable.HashMap
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.ide.core.workflow.model.ICapsuleView
import scala.collection.JavaConversions
import scala.collection.mutable.HashSet

class MoleSceneManager(var startingCapsule: Option[CapsuleViewUI]= None) {

  var capsuleViews= new DualHashBidiMap[String, ICapsuleView]
  var transitions= new DualHashBidiMap[String, TransitionUI]
  var capsuleConnections= new HashMap[ICapsuleView, HashSet[TransitionUI]]
  var nodeID= 0
  var edgeID= 0
  var name: Option[String]= None
  
  def setStartingCapsule(stCapsule: CapsuleViewUI) = {
    if (startingCapsule.isDefined) startingCapsule.get.defineStartingCapsule(false)
    startingCapsule= Some(stCapsule)
    startingCapsule.get.defineStartingCapsule(true)
  }
  
  def getNodeID: String= "node" + nodeID
  
  def getEdgeID: String= "edge" + edgeID
  
  def registerCapsuleView(cv: ICapsuleView) = {
    nodeID+= 1
    capsuleViews.put(getNodeID,cv)
    capsuleConnections+= cv-> HashSet.empty[TransitionUI]
  }
  
  def removeCapsuleView(nodeID: String) = {
    capsuleConnections(capsuleViews.get(nodeID)).foreach(transitions.removeValue(_))
    capsuleViews.remove(nodeID)
  }
  
  def capsuleViewID(cv: ICapsuleView) = capsuleViews.getKey(cv)
  
  def getTransitions= transitions.values 
  
  def getTransition(edgeID: String) = transitions.get(edgeID)
  
  def removeTransition(edge: String) = transitions.remove(edge)
  
  def registerTransition(transition: TransitionUI): Unit = {
    edgeID+= 1
    registerTransition(getEdgeID,transition)
  }
  
  def registerTransition(edgeID: String,transition: TransitionUI): Unit = {
    transitions.put(edgeID, transition)
    capsuleConnections(transition.source)+= transition
    capsuleConnections(transition.target.capsuleView)+= transition
  }
}