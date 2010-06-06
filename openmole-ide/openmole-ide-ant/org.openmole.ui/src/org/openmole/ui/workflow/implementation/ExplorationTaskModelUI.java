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

import java.util.ArrayList;
import java.util.Collection;
import org.openmole.ui.control.TableType;
import org.openmole.ui.control.TableType.Name;
import org.openmole.core.workflow.model.task.IExplorationTask;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class ExplorationTaskModelUI<T extends IExplorationTask> extends GenericTaskModelUI<T> {

    @Override
    public synchronized Collection<Name> getFields() {
        setFields();
        return fields;
    }

    @Override
    public synchronized void setFields() {
        if (fields == null) {
            fields = new ArrayList<TableType.Name>();
            fields.add(Name.INPUT_PARAMETER);
            fields.add(Name.OUTPUT_PARAMETER);
            fields.add(Name.DESIGN_OF_EXPERIMENT);
        }
    }
}
