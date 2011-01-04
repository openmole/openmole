/*
 *  Copyright (C) 2010 leclaire
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

import java.util.HashSet;
import java.util.Set;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.core.model.capsule.IGenericCapsule;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class CapsuleModelUI<T extends IGenericCapsule> extends ObjectModelUI implements ICapsuleModelUI {

    public static CapsuleModelUI EMPTY_CAPSULE_MODEL = new CapsuleModelUI();
    private IGenericTaskModelUI<IGenericTask> taskModel;
    private transient int nbInputSlots = 0;
    private boolean startingCapsule = false;
    private final static String category = "Task Tapsules";
    private Set<ICapsuleModelUI> connectedTo = new HashSet<ICapsuleModelUI>();
    private boolean containsTask = false;

    CapsuleModelUI() {
        this(TaskModelUI.EMPTY_TASK_MODEL);
    }

    CapsuleModelUI(IGenericTaskModelUI<IGenericTask> taskModel) {
        this.taskModel = taskModel;
    }

    public boolean containsTask() {
        return containsTask;
    }

    @Override
    public IGenericTaskModelUI<IGenericTask> getTaskModel() {
        return taskModel;
    }

    @Override
    public void setTaskModel(IGenericTaskModelUI taskModel) {
        this.taskModel = taskModel;
        this.containsTask = true;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public int getNbInputslots() {
        return nbInputSlots;
    }

    @Override
    public void addInputSlot() {
        nbInputSlots++;
    }

    @Override
    public boolean isSlotRemovable() {
        return (nbInputSlots > 1 ? true : false);
    }

    @Override
    public boolean isSlotAddable() {
        return (nbInputSlots < ApplicationCustomize.NB_MAX_SLOTS ? true : false);
    }

    @Override
    public void removeInputSlot() {
        nbInputSlots -= 1;
    }

    @Override
    public void eventOccured(Object t) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addTransition(ICapsuleModelUI taskmodel) {
        connectedTo.add(taskmodel);
    }

    @Override
    public void defineAsStartingCapsule() {
        nbInputSlots = 1;
        startingCapsule = true;
    }

    @Override
    public void defineAsRegularCapsule() {
        startingCapsule = false;
    }

    @Override
    public boolean isStartingCapsule() {
        return startingCapsule;
    }

    @Override
    public boolean hasChild() {
        return !connectedTo.isEmpty();
    }

    @Override
    public Set getChilds() {
        return connectedTo;
    }
}
