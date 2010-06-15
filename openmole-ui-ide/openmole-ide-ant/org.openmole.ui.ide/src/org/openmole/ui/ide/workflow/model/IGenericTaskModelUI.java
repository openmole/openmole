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
package org.openmole.ui.ide.workflow.model;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
import java.util.Set;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.ide.commons.IOType;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;

public interface IGenericTaskModelUI<T extends IGenericTask> {
    public void proceed();
    Class <? extends IGenericTask> getCoreTaskClass();
    void setCoreTaskClass(Class <? extends IGenericTask> coreTask);
    void addPrototype(PrototypeUI p,IOType ioType);
    Set<PrototypeUI> getPrototypesIn();
    Set<PrototypeUI> getPrototypesOut();
}