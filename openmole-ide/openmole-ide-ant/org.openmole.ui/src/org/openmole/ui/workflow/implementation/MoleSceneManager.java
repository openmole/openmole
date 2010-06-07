/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
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
package org.openmole.ui.workflow.implementation;

import java.util.Set;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.openmole.ui.workflow.model.ITaskCapsuleView;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleSceneManager {

    private BidiMap<String, ITaskCapsuleView> taskViews = new DualHashBidiMap<String, ITaskCapsuleView>();
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

    public Set<ITaskCapsuleView> getTaskViews(){
        return taskViews.values();
    }

    public void registerTaskView(ITaskCapsuleView cv) {
        taskViews.put(getNodeID(), cv);
        nodeID++;
    }

    public String getTaskViewID(ITaskCapsuleView cv) {
        return taskViews.getKey(cv);
    }

    public ITaskCapsuleView getTaskView(String name) {
        return taskViews.get(name);
    }

    public void removeTaskView(ITaskCapsuleView cv) {
        taskViews.remove(cv);
    }

    public void setTransition(String start,
            String end) {
    //    taskViews.get(start).setTransitionTo(taskViews.get(end).getTaskCapsule());
    }

    public void printTaskC() {
        for (String t : taskViews.keySet()) {
            System.out.println("TASKC :: " + t);
        }
    }
}
