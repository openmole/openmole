/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.ui.ide.workflow.provider;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.netbeans.api.visual.widget.Widget;
import org.openmole.ui.ide.commons.IOType;
import org.openmole.ui.ide.workflow.action.AddExistingPrototype;
import org.openmole.ui.ide.workflow.action.AddInputAction;
import org.openmole.ui.ide.workflow.action.AddOutputAction;
import org.openmole.ui.ide.workflow.action.AddTaskAction;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.Preferences;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.TaskCapsuleViewUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class TaskCapsuleMenuProvider extends GenericMenuProvider {

    private JMenu inPrototypeMenu;
    private JMenu outPrototypeMenu;
    private TaskCapsuleViewUI capsuleView = null;
    private boolean encapsulated = false;

    @Override
    public JPopupMenu getPopupMenu(Widget widget, Point point) {
        if (encapsulated) {
            Collection<JMenuItem> colI = fillPrototypeMenu(IOType.INPUT);
            Collection<JMenuItem> colO = fillPrototypeMenu(IOType.OUTPUT);
            if (!colI.isEmpty()) {
                menus.remove(inPrototypeMenu);
                menus.remove(outPrototypeMenu);
                inPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an input prototype ", colI);
                outPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an output prototype ", colO);
                menus.add(inPrototypeMenu);
                menus.add(outPrototypeMenu);
            }
        }
        return super.getPopupMenu(widget, point);
    }

    public TaskCapsuleMenuProvider(MoleScene scene,
            TaskCapsuleViewUI capsuleView) {
        super();
        this.capsuleView = capsuleView;
        JMenuItem mItemI = new JMenuItem("an input slot");
        mItemI.addActionListener(new AddInputAction(capsuleView));


        JMenuItem mItemO = new JMenuItem("an output slot");
        mItemO.addActionListener(new AddOutputAction(capsuleView));

        Collection<JMenuItem> colI = new ArrayList<JMenuItem>();
        colI.add(mItemI);
        colI.add(mItemO);


        Collection<JMenuItem> colTask = new ArrayList<JMenuItem>();
        for (Class c : Preferences.getInstance().getCoreTaskClasses()) {
            JMenuItem it = new JMenuItem(c.getSimpleName());
            it.addActionListener(new AddTaskAction(scene,
                    capsuleView,
                    c));
            colTask.add(it);
        }

        menus.add(PopupMenuProviderFactory.addSubMenu("Encapsulate a task ", colTask));
        menus.add(PopupMenuProviderFactory.addSubMenu("Add ",
                colI));
        JMenuItem itR = new JMenuItem("Remove");
        items.add(itR);
    }

    public void addTaskMenus() {
        encapsulated = true;
    }

    public Collection<JMenuItem> fillPrototypeMenu(IOType type) {
        Collection<JMenuItem> prototypeCol = new ArrayList<JMenuItem>();
        for (PrototypeUI p : Preferences.getInstance().getPrototypes()) {
            JMenuItem it = new JMenuItem(p.getName());
            it.addActionListener(new AddExistingPrototype(p, capsuleView,type));
            prototypeCol.add(it);
        }
        return prototypeCol;
    }
}
