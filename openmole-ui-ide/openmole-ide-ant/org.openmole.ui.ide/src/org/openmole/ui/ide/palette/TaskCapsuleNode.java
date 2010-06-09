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

package org.openmole.ui.ide.palette;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.openide.util.datatransfer.ExTransferable;
import org.openmole.ui.ide.commons.ApplicationCustomize;
import org.openmole.ui.ide.palette.Category.CategoryName;
import org.openmole.ui.ide.workflow.implementation.TaskCapsuleModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskCapsuleNode extends GenericNode{

    public TaskCapsuleNode(DataFlavor key) {
        super(key,
              CategoryName.TASK_CAPSULE,
              org.openmole.core.implementation.capsule.TaskCapsule.class);
    }

     //DND start
    @Override
    public Transferable drag() throws IOException {
        ExTransferable retValue = ExTransferable.create( super.drag() );
        retValue.put( new ExTransferable.Single(ApplicationCustomize.TASK_CAPSULE_DATA_FLAVOR) {
            @Override
            protected Object getData() throws IOException, UnsupportedFlavorException {
                return TaskCapsuleModelUI.class;
            }
        });
        return retValue;
    }
    //DND end
}

