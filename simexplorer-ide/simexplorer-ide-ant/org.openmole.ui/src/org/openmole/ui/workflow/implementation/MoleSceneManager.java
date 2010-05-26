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

import java.util.Map;
import java.util.WeakHashMap;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;
import org.openmole.ui.workflow.model.IObjectViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleSceneManager {

    private Map<String, ICapsuleModelUI> taskCapsuleModels = new WeakHashMap<String, ICapsuleModelUI>();
    private Map<String, IGenericTaskModelUI> taskModels = new WeakHashMap<String, IGenericTaskModelUI>();

    public void registerTaskCapsuleModel(String nodeName,
            ICapsuleModelUI cm) {
      //  System.out.println("REGISTER " + nodeName + ", " + cm);
        taskCapsuleModels.put(nodeName, cm);
    }

    public void registerTaskModel(String nodeName,
                                  IGenericTaskModelUI tm) {
        System.out.println("REGISTESR " + nodeName);
        taskModels.put(nodeName,tm);
    }
    
    public void setTransition(String start,
            String end) throws UserBadDataError {
        testTransitionAbility(start);
        testTransitionAbility(end);
        taskCapsuleModels.get(start).setTransitionTo(taskCapsuleModels.get(end).getTaskCapsule());
    }

    private boolean testTaskCapsuleExistence(String capsule){
        System.out.println("testTaskCapsuleExistence " +taskCapsuleModels.containsKey(capsule) );
        return taskCapsuleModels.containsKey(capsule);
    }

    public  boolean testTaskExistence(String task){
        return taskModels.containsKey(task);
    }

    public void testTransitionAbility(String capsule) throws UserBadDataError {
        if (!testTaskCapsuleExistence(capsule)) {
            throw new UserBadDataError(capsule + " is not a Task capsule. The transition can not be completed");
        }
    }

    public boolean isObjectRenamable(String object){
        if (testTaskExistence(object)) return true;
        else if(testTaskCapsuleExistence(object)) return false;
        else return false;
    }

    IGenericTaskModelUI getTaskModelUI(String st){
        if (testTaskExistence(st)) return taskModels.get(st);
        else return null;
    }

    public void printTask(){
        for(String t: taskModels.keySet()){
            System.out.println("TASK :: " + t);
        }
    }
}
