/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.ide.workflow.implementation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.openmole.ui.ide.workflow.implementation.paint.ISlotWidget;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.ui.ide.workflow.model.ICapsuleView;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleSceneManager {

    private BidiMap<String, ICapsuleView> capsuleViews = new DualHashBidiMap<String, ICapsuleView>();
    private BidiMap<String, TransitionUI> transitions = new DualHashBidiMap<String, TransitionUI>();
    private Map<ICapsuleView, Collection<TransitionUI>> capsuleConnection = new HashMap<ICapsuleView, Collection<TransitionUI>>();
    private CapsuleViewUI startingCapsule = null;
    private int nodeID = 0;
    private int edgeID = 0;
    private String name = "";

    public String getNodeID() {
        return "node" + nodeID;
    }

    public String getEdgeID() {
        return "edge" + edgeID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartingCapsule(CapsuleViewUI startingCapsuleView) {
        if (this.startingCapsule != null) {
            startingCapsule.defineAsRegularCapsule();
        }
        this.startingCapsule = startingCapsuleView;
        startingCapsule.defineAsStartingCapsule();
    }

    public ICapsuleModelUI getStartingCapsule() {
        return startingCapsule.getCapsuleModel();
    }

    public Set<ICapsuleView> getCapsuleViews() {
        return capsuleViews.values();
    }

    public void registerCapsuleView(ICapsuleView cv) {
        nodeID++;
        capsuleViews.put(getNodeID(), cv);
        capsuleConnection.put(cv, new HashSet<TransitionUI>());
    }

    public String getCapsuleViewID(ICapsuleView cv) {
        return capsuleViews.getKey(cv);
    }

    public ICapsuleView getCapsuleView(String name) {
        return capsuleViews.get(name);
    }

    public void removeCapsuleView(String nodeID) {
        ICapsuleModelUI model = capsuleViews.get(nodeID).getCapsuleModel();

        for (TransitionUI t : capsuleConnection.get(capsuleViews.get(nodeID))) {
            transitions.removeValue(t);
        }
        capsuleViews.remove(nodeID);
    }

    public Collection<TransitionUI> getTransitions() {
        return transitions.values();
    }

    public TransitionUI getTransition(String edge) {
        return transitions.get(edge);
    }

    public void removeTransition(String edge) {
        transitions.remove(edge);
    }

    public void registerTransition(TransitionUI transition) {
        edgeID++;
        registerTransition(getEdgeID(),transition);
    }

    public void registerTransition(String edgeID,
            TransitionUI transition) {
        transitions.put(edgeID, transition);
        capsuleConnection.get(transition.getSource()).add(transition);
        capsuleConnection.get(transition.getTarget().getCapsuleView()).add(transition);

    }
}
