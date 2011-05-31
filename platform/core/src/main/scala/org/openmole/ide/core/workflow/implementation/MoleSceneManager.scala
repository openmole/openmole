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

//
//MoleSceneManager {
//
//    private BidiMap<String, ICapsuleView> capsuleViews = new DualHashBidiMap<String, ICapsuleView>();
//    private BidiMap<String, TransitionUI> transitions = new DualHashBidiMap<String, TransitionUI>();
//    private Map<ICapsuleView, Collection<TransitionUI>> capsuleConnection = new HashMap<ICapsuleView, Collection<TransitionUI>>();
//    private CapsuleViewUI startingCapsule = null;
//    private int nodeID = 0;
//    private int edgeID = 0;
//    private String name = "";
//
//    public String getNodeID() {
//        return "node" + nodeID;
//    }
//
//    public String getEdgeID() {
//        return "edge" + edgeID;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public void setStartingCapsule(CapsuleViewUI startingCapsuleView) {
//        if (this.startingCapsule != null) {
//            startingCapsule.defineAsRegularCapsule();
//        }
//        this.startingCapsule = startingCapsuleView;
//        startingCapsule.defineAsStartingCapsule();
//    }
//
//    public ICapsuleModelUI getStartingCapsule() {
//        return startingCapsule.getCapsuleModel();
//    }
//
//    public Set<ICapsuleView> getCapsuleViews() {
//        return capsuleViews.values();
//    }
//
//    public void registerCapsuleView(ICapsuleView cv) {
//        nodeID++;
//        capsuleViews.put(getNodeID(), cv);
//        capsuleConnection.put(cv, new HashSet<TransitionUI>());
//    }
//
//    public String getCapsuleViewID(ICapsuleView cv) {
//        return capsuleViews.getKey(cv);
//    }
//
//    public ICapsuleView getCapsuleView(String name) {
//        return capsuleViews.get(name);
//    }
//
//    public void removeCapsuleView(String nodeID) {
//        ICapsuleModelUI model = capsuleViews.get(nodeID).getCapsuleModel();
//
//        for (TransitionUI t : capsuleConnection.get(capsuleViews.get(nodeID))) {
//            transitions.removeValue(t);
//        }
//        capsuleViews.remove(nodeID);
//    }
//
//    public Collection<TransitionUI> getTransitions() {
//        return transitions.values();
//    }
//
//    public TransitionUI getTransition(String edge) {
//        return transitions.get(edge);
//    }
//
//    public void removeTransition(String edge) {
//        transitions.remove(edge);
//    }
//
//    public void registerTransition(TransitionUI transition) {
//        edgeID++;
//        registerTransition(getEdgeID(),transition);
//    }
//
//    public void registerTransition(String edgeID,
//            TransitionUI transition) {
//        transitions.put(edgeID, transition);
//        capsuleConnection.get(transition.getSource()).add(transition);
//        capsuleConnection.get(transition.getTarget().getCapsuleView()).add(transition);
//
//    }
//}