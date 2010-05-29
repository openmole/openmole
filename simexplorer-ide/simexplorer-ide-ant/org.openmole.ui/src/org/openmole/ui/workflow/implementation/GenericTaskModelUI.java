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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.commons.IOType;
import org.openmole.ui.control.TableType;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public abstract class GenericTaskModelUI<T extends IGenericTask> extends ObjectModelUI implements IGenericTaskModelUI<T> {

    protected transient Collection<TableType.Name> fields;
    private Set<IPrototype> prototypesIn;
    private Set<IPrototype> prototypesOut;
    private final static String category = "Tasks";
    protected IGenericTask coreTask;

    @Override
    public void addPrototype(IPrototype p,
                             IOType ioType){
        if (ioType == IOType.INPUT) addPrototypeIn(p);
        else addPrototypeOut(p);
    }

    private void addPrototypeIn(IPrototype p){
        if (prototypesIn == null) prototypesIn = new HashSet<IPrototype>();
        prototypesIn.add(p);
    }

     private void addPrototypeOut(IPrototype p){
        if (prototypesOut == null) prototypesOut = new HashSet<IPrototype>();
        prototypesOut.add(p);
    }

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
