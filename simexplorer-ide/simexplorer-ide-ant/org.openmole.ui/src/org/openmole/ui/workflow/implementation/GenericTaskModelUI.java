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

import java.util.ArrayList;
import java.util.Collection;
import org.openmole.core.workflow.implementation.task.GenericTask;
import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.ui.control.TableType;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;
import org.openmole.ui.workflow.model.ITaskViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public abstract class GenericTaskModelUI<T extends IGenericTask> extends ObjectModelUI implements IGenericTaskModelUI<T> {

    protected transient Collection<TableType.Name> fields;
    private Collection<ITaskViewUI> views = new ArrayList<ITaskViewUI>();
    private final static String category = "Tasks";
    protected IGenericTask coreTask;

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public IGenericTask getTask() {
        return this.coreTask;
    }

    @Override
    public void setTask(IGenericTask coreTask) {
        this.coreTask = coreTask;
    }


}
