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

package org.openmole.ui.workflow.provider;

import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Exceptions;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.MoleScene;
import org.openmole.ui.workflow.implementation.TaskCapsuleViewUI;
import org.openmole.ui.workflow.implementation.UIFactory;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class DnDNewTaskProvider extends DnDProvider{
    private TaskCapsuleViewUI capsuleView;

    public DnDNewTaskProvider(MoleScene molescene,
                              TaskCapsuleViewUI cv) {
        super(molescene);
        this.capsuleView = cv;
    }

    @Override
    public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
        ConnectorState state = ConnectorState.REJECT;
        if (transferable.isDataFlavorSupported(ApplicationCustomize.TASK_DATA_FLAVOR))
            state = ConnectorState.ACCEPT;
        return state;
    }

@Override
    public void accept(Widget widget, Point point, Transferable transferable) {
        try {
            capsuleView.encapsule((Class<? extends IGenericTask>) transferable.getTransferData(ApplicationCustomize.TASK_DATA_FLAVOR));
        } catch (UnsupportedFlavorException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
           
        scene.repaint();
        scene.revalidate();
    }
}
