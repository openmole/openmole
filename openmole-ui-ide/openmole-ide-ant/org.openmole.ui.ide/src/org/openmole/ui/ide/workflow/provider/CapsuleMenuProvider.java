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
import org.openmole.ui.ide.workflow.action.AddExistingPrototypeAction;
import org.openmole.ui.ide.workflow.action.AddInputAction;
import org.openmole.ui.ide.workflow.action.AddTaskAction;
import org.openmole.ui.ide.workflow.action.DefineMoleStartAction;
import org.openmole.ui.ide.workflow.action.RemoveCapsuleAction;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;
import org.openmole.ui.ide.workflow.implementation.IEntityUI;
import org.openmole.ui.ide.workflow.implementation.PrototypesUI;
import org.openmole.ui.ide.workflow.implementation.TaskUI;
import org.openmole.ui.ide.workflow.implementation.TasksUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class CapsuleMenuProvider extends GenericMenuProvider {

    private JMenu inPrototypeMenu;
    private JMenu outPrototypeMenu;
    private JMenu taskMenu;
    private CapsuleViewUI capsuleView = null;
    private boolean encapsulated = false;
    JMenuItem itIS = new JMenuItem();
    MoleScene scene;

    @Override
    public JPopupMenu getPopupMenu(Widget widget, Point point) {
        //Update prototypes
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
        //Update tasks
        if (!TasksUI.getInstance().getAll().isEmpty()) {
            menus.remove(taskMenu);
            Collection<JMenuItem> colTask = new ArrayList<JMenuItem>();
            for (IEntityUI c : TasksUI.getInstance().getAll()) {
                JMenuItem it = new JMenuItem(c.getName() + " :: " + c.getType().getSimpleName());
                it.addActionListener(new AddTaskAction(scene,
                        capsuleView, (TaskUI) c));
                colTask.add(it);
            }
            taskMenu = PopupMenuProviderFactory.addSubMenu("Encapsulate a task ", colTask);
            menus.add(taskMenu);
        }
        itIS.setEnabled(!capsuleView.getCapsuleModel().isStartingCapsule());

        return super.getPopupMenu(widget, point);
    }

    public CapsuleMenuProvider(MoleScene scene,
            CapsuleViewUI capsuleView) {
        super();
        this.capsuleView = capsuleView;
        this.scene = scene;

        itIS = new JMenuItem("Add an input slot");
        itIS.addActionListener(new AddInputAction(capsuleView));
        JMenuItem itR = new JMenuItem("Remove capsule");
        itR.addActionListener(new RemoveCapsuleAction(scene,capsuleView));
        JMenuItem itStart = new JMenuItem("Define as starting capsule");
        itStart.addActionListener(new DefineMoleStartAction(scene, capsuleView));

        items.add(itIS);
        items.add(itR);
        items.add(itStart);

    }

    public void addTaskMenus() {
        encapsulated = true;
    }

    public Collection<JMenuItem> fillPrototypeMenu(IOType type) {
        Collection<JMenuItem> prototypeCol = new ArrayList<JMenuItem>();
        for (IEntityUI p : PrototypesUI.getInstance().getAll()) {
            JMenuItem it = new JMenuItem(p.getName() + " :: " + p.getType().getSimpleName());
            it.addActionListener(new AddExistingPrototypeAction(p, capsuleView, type));
            prototypeCol.add(it);
        }
        return prototypeCol;
    }
}
