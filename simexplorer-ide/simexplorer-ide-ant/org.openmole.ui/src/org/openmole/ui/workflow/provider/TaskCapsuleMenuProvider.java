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
package org.openmole.ui.workflow.provider;

import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JMenuItem;
import org.openmole.ui.workflow.action.AddInputAction;
import org.openmole.ui.workflow.action.AddOutputAction;
import org.openmole.ui.workflow.model.IConnectable;
import org.openmole.ui.workflow.model.ITaskCapsuleViewUI;

/**
 *
 * @author mathieu
 */
public class TaskCapsuleMenuProvider extends GenericMenuProvider {

    public TaskCapsuleMenuProvider(IConnectable connectable) {
        super();

        JMenuItem mItemI = new JMenuItem("an input slot");
        mItemI.addActionListener(new AddInputAction(connectable));


        JMenuItem mItemO = new JMenuItem("an output slot");
        mItemO.addActionListener(new AddOutputAction(connectable));

        Collection<JMenuItem> colI = new ArrayList<JMenuItem>();
        colI.add(mItemI);
        colI.add(mItemO);

        menus.add(PopupMenuProviderFactory.addSubMenu("Add ",
                colI));
    }
}
