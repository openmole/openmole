/*
 *  Copyright (C) 2010 leclaire
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

import java.util.Collection;
import org.openmole.ui.workflow.implementation.paint.IOType;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.ui.workflow.model.ITaskViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskCompositeModelUI implements ICapsuleModelUI, IGenericTaskModelUI{

    IGenericTaskModelUI<IGenericTask> tModel;
    ICapsuleModelUI<IGenericTaskCapsule> tcModel;

    public TaskCompositeModelUI(IGenericTaskModelUI<IGenericTask> tm,
                                ICapsuleModelUI<IGenericTaskCapsule> tcm) {
        tModel = tm;
        tcModel = tcm;
    }

    @Override
    public void addOutputSlot() {
        tcModel.addInputSlot();
    }

    @Override
    public void addInputSlot() {
        tcModel.addInputSlot();
    }

    @Override
    public int getNbInputslots() {
        return tcModel.getNbInputslots();
    }

    @Override
    public int getNbOutputslots() {
        return tcModel.getNbOutputslots();
    }

    @Override
    public boolean isSlotRemovable(IOType type) {
        return tcModel.isSlotRemovable(type);
    }

    @Override
    public boolean isSlotAddable(IOType type) {
        return tcModel.isSlotAddable(type);
    }

    @Override
    public void removeSlot(IOType type) {
        tcModel.removeSlot(type);
    }

    @Override
    public Collection getFields() {
        return tModel.getFields();
    }

    @Override
    public void setFields() {
        tModel.setFields();
    }

    @Override
    public void setTaskCapsule(IGenericTaskCapsule taskCapsule) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IGenericTaskCapsule getTaskCapsule() {
        return tcModel.getTaskCapsule();
    }

    @Override
    public void setTransitionTo(IGenericTaskCapsule tc) {
        tcModel.setTransitionTo(tc);
    }
}
