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
package org.openmole.ui.ide.workflow.implementation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.ide.commons.IOType;
import org.openmole.ui.ide.control.TableType;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public abstract class GenericTaskModelUI<T extends IGenericTask> extends ObjectModelUI implements IGenericTaskModelUI<T> {

    protected transient Collection<TableType.Name> fields;
    private Set<PrototypeUI> prototypesIn;
    private Set<PrototypeUI> prototypesOut;
    private final static String category = "Tasks";
    protected Class<? extends IGenericTask> coreTask;

    @Override
    public Set<PrototypeUI> getPrototypesIn() {
        if (prototypesIn == null) prototypesIn = new HashSet<PrototypeUI>();
        return prototypesIn;
    }

    @Override
    public Set<PrototypeUI> getPrototypesOut() {
        if (prototypesOut == null) prototypesOut = new HashSet<PrototypeUI>();
        return prototypesOut;
    }

    @Override
    public void addPrototype(PrototypeUI p,
                             IOType ioType){
        if (ioType == IOType.INPUT) addPrototypeIn(p);
        else addPrototypeOut(p);
    }

    private void addPrototypeIn(PrototypeUI p){
        if (prototypesIn == null) prototypesIn = new HashSet<PrototypeUI>();
        prototypesIn.add(p);
    }

     private void addPrototypeOut(PrototypeUI p){
        if (prototypesOut == null) prototypesOut = new HashSet<PrototypeUI>();
        prototypesOut.add(p);
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public Class <? extends IGenericTask> getCoreTaskClass() {
        return this.coreTask;
    }

    @Override
    public void setCoreTaskClass(Class <? extends IGenericTask> coreTask) {
        this.coreTask = coreTask;
    }


}
