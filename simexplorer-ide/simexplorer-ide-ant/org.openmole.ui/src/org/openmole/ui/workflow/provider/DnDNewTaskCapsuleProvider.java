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
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.implementation.MoleScene;
import org.openmole.ui.workflow.implementation.UIFactory;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class DnDNewTaskCapsuleProvider extends DnDProvider{
    private IGenericTaskCapsule model;

    public DnDNewTaskCapsuleProvider(MoleScene molescene) {
        super(molescene);
    }

    @Override
    public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
        ConnectorState state = ConnectorState.REJECT;
        if (transferable.isDataFlavorSupported(ApplicationCustomize.TASK_CAPSULE_DATA_FLAVOR))
            state = ConnectorState.ACCEPT;
        return state;
    }

@Override
    public void accept(Widget widget, Point point, Transferable transferable) {
    Widget w = UIFactory.getInstance().createTaskCapsule(scene,
                                                         point).getConnectableWidget();
   // System.out.println("Point " + widget.convertLocalToScene(point).x + ", " + widget.convertLocalToScene(point).y);
   // System.out.println("Point " + widget.convertSceneToLocal(point).x + ", " + widget.convertSceneToLocal(point).y);
   // System.out.println("Point " + point.x + ", " + point.y);
   // w.setPreferredLocation(widget.convertLocalToScene(point));
    scene.repaint();
    scene.revalidate();
}
}

