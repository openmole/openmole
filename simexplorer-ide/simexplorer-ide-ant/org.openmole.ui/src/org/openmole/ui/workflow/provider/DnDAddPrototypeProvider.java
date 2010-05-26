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
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.MoleScene;
import org.openmole.ui.workflow.implementation.TaskCapsuleModelUI;
import org.openmole.ui.workflow.implementation.TaskCapsuleViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class DnDAddPrototypeProvider extends DnDProvider{
    private boolean encapsulated = false;
    private TaskCapsuleViewUI view;

    public DnDAddPrototypeProvider(MoleScene molescene,
                                   TaskCapsuleViewUI view) {
        super(molescene);
        this.view = view;
    }

    public void setEncapsulated(boolean encapsulated) {
        this.encapsulated = encapsulated;
    }

    @Override
    public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
        ConnectorState state = ConnectorState.REJECT;
        if (transferable.isDataFlavorSupported(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR)&&
            encapsulated == true)
            state = ConnectorState.ACCEPT;
        return state;
    }

    @Override
    public void accept(Widget widget, Point point, Transferable t) {
        try {
            System.out.println("Add " + t.getTransferData(ApplicationCustomize.PROTOTYPE_DATA_FLAVOR));
            System.out.println("Widget "+ (view.getTaskModel()));
        } catch (UnsupportedFlavorException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
