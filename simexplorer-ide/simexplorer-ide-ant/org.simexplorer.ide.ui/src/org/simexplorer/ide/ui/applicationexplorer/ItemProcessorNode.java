/*
 *  Copyright © 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.ide.ui.applicationexplorer;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.Children;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.openmole.core.implementation.task.Task;

class ItemProcessorNode extends AbstractProcessorNode<Task> {

    protected ProcessorsListChildren processorsListChildren;

    public ItemProcessorNode(Task processor, ProcessorsListChildren processorsListChildren) {
        super(processor, Children.LEAF, Lookups.singleton(processor));
        this.processorsListChildren = processorsListChildren;
        String label = processor.getName();
        setDisplayName(label != null ? label : processor.getClass().getSimpleName());
    }

    @Override
    public Action[] getActions(boolean arg0) {
        List<Action> actions = new ArrayList<Action>();
        if (processorsListChildren.indexOf(processor) > 0) {
            actions.add(new MoveUpAction());
        }
        if (processorsListChildren.indexOf(processor) < processorsListChildren.size() - 1) {
            actions.add(new MoveDownAction());
        }
        actions.add(new DeleteItemAction());
        return actions.toArray(new Action[0]);
    }

    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("icons/" + processor.getClass().getSimpleName() + ".png");
    }

    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }

    private class DeleteItemAction extends AbstractAction {

        public DeleteItemAction() {
            putValue(NAME, "Remove");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            processorsListChildren.removeProcessor(getLookup().lookup(Task.class));
        }
    }

    private class MoveUpAction extends AbstractAction {

        public MoveUpAction() {
            putValue(NAME, "Move up");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            processorsListChildren.moveUp(getLookup().lookup(Task.class));
        }
    }

    private class MoveDownAction extends AbstractAction {

        public MoveDownAction() {
            putValue(NAME, "Move down");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            processorsListChildren.moveDown(getLookup().lookup(Task.class));
        }
    }
}
