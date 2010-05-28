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

import org.openmole.core.workflow.implementation.capsule.TaskCapsule;
import org.openmole.core.workflow.implementation.transition.SingleTransition;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskCapsuleModelUI extends CapsuleModelUI<IGenericTaskCapsule> {

    TaskCapsule taskCapsule;

    public TaskCapsuleModelUI() {
        setTaskCapsule(new TaskCapsule());
    }

    @Override
    public IGenericTaskCapsule getTaskCapsule() {
        if (taskCapsule == null) {
            setTaskCapsule();
        }
        return taskCapsule;
    }

    @Override
    public void setTransitionTo(IGenericTaskCapsule tc) {
        new SingleTransition(taskCapsule, tc);
    }

    @Override
    public void setTaskCapsule(IGenericTaskCapsule taskCapsule) {
        this.taskCapsule = (TaskCapsule) taskCapsule;
    }

    private void setTaskCapsule() {
        taskCapsule = new TaskCapsule();
    }
}
