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
   // private Collection<TransitionUI> transitions = new ArrayList<TransitionUI>();
    private Map<String,TransitionUI> transitions = new HashMap<String, TransitionUI>();
    private CapsuleViewUI startingCapsule = null;
    private int nodeID = 0;
    private String name = "";

    public String getNodeID() {
        return "node" + nodeID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartingCapsule(CapsuleViewUI startingCapsuleView) {
        if (this.startingCapsule != null) {
            System.out.println("-- define as regular");
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
    }

    public String getCapsuleViewID(ICapsuleView cv) {
        return capsuleViews.getKey(cv);
    }

    public ICapsuleView getCapsuleView(String name) {
        return capsuleViews.get(name);
    }

    public void removeCapsuleView(ICapsuleView cv) {
        capsuleViews.remove(cv);
    }

    public Collection<TransitionUI> getTransitions() {
        //return transitions;
        return transitions.values();
    }

    public TransitionUI getTransition(String edge){
        return transitions.get(edge);
    }

    public void removeTransition(String edge){
        transitions.remove(edge);
    }

//    public void addTransition(ICapsuleModelUI source,
//            ICapsuleModelUI target,
//            int targetSlotNumber) {
        public void addTransition(String edgename,
                CapsuleViewUI source,
            ISlotWidget target) {
            transitions.put(edgename, new TransitionUI(source, target));
           // transitions.add(new TransitionUI(source, target));
       // transitions.add(new TransitionUI(source, target, targetSlotNumber));
    }



    public void printTaskC() {
        for (String t : capsuleViews.keySet()) {
            System.out.println("TASKC :: " + t);
        }
    }
}
