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
package org.openmole.ui.palette;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.openide.util.datatransfer.ExTransferable;

import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.PropertyManager;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskNode extends GenericNode {

    private Class<? extends IGenericTask> coreTask;

    public TaskNode(DataFlavor key,
                    Class<? extends IGenericTask> coreTask) {
        super(key,PropertyManager.TASK,coreTask);

        this.coreTask = coreTask;
    }

     //DND start
    @Override
    public Transferable drag() throws IOException {
        ExTransferable retValue = ExTransferable.create( super.drag() );
        retValue.put( new ExTransferable.Single(ApplicationCustomize.TASK_DATA_FLAVOR) {
            @Override
            protected Object getData() throws IOException, UnsupportedFlavorException 
            {return coreTask;}
            
        });
        return retValue;
    }
    //DND end
}
