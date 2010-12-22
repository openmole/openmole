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

import java.util.Set;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.ui.ide.workflow.model.ICapsuleView;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleSceneManager {

    private BidiMap<String, ICapsuleView> capsuleViews = new DualHashBidiMap<String, ICapsuleView>();
    private ICapsuleModelUI startingCapsule = CapsuleModelUI.EMPTY_CAPSULE_MODEL;
    private int nodeCounter = 0;
    private int nodeID = 0;

    public void incrementNodeName() {
        nodeCounter++;
    }

    public String getNodeName() {
        return "task" + nodeCounter;
    }

    public String getNodeID() {
        return "node" + nodeID;
    }

    public void setStartingCapsule(ICapsuleModelUI startingCapsule) {
        if (this.startingCapsule != CapsuleModelUI.EMPTY_CAPSULE_MODEL){
            this.startingCapsule.defineAsRegularCapsule();
        }
        this.startingCapsule = startingCapsule;
        startingCapsule.defineAsStartingCapsule();
    }

    public ICapsuleModelUI getStartingCapsule() {
        return startingCapsule;
    }

    public Set<ICapsuleView> getCapsuleViews(){
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

    public void printTaskC() {
        for (String t : capsuleViews.keySet()) {
            System.out.println("TASKC :: " + t);
        }
    }
}
