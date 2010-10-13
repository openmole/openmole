/*
 *
 *  Copyright © 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
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

import java.awt.Dialog;
import java.awt.Image;
import java.beans.PropertyVetoException;
import org.openide.util.Exceptions;
import org.simexplorer.ide.ui.ServicesProvider;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.simexplorer.ide.ui.ProcessorChooserPanel;
import org.simexplorer.ui.ide.workflow.model.TasksList;
import org.openmole.core.implementation.task.Task;

public class ProcessorsListNode<T extends TasksList> extends AbstractProcessorNode<T> {

    private ProcessorsListChildren children;
    private Action[] actions;

    public ProcessorsListNode(T processorsList) {
        super(processorsList, new ProcessorsListChildren(processorsList), Lookups.singleton(processorsList));
        children = (ProcessorsListChildren) this.getChildren();
        String label = processorsList.getName();
        setDisplayName(label);
        if (ServicesProvider.getProcessors(label) != null) {
            actions = new Action[]{new AddProcessorAction()};
        } else {
            actions = new Action[]{};
        }
    }

    @Override
    public Action[] getActions(boolean arg0) {
        return actions;
    }

    @Override
    public Image getIcon(int type) {
        Image im = ImageUtilities.loadImage("icons/" + processor.getClass().getSimpleName() + ".png");
        return (im != null) ? im : ImageUtilities.loadImage("icons/view_tree.png");
    }

    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }

    private class AddProcessorAction extends AbstractAction {

        public AddProcessorAction() {
            putValue(NAME, "Add a processor…");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TasksList selectedProcessor = getLookup().lookup(TasksList.class);
            ProcessorChooserPanel processorChooserPanel = new ProcessorChooserPanel(selectedProcessor);
            DialogDescriptor dialogDescriptor = new DialogDescriptor(processorChooserPanel, getName());
            dialogDescriptor.setValid(false);
            processorChooserPanel.setDialogDescriptor(dialogDescriptor);
            Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
            dialog.setVisible(true);
            dialog.toFront();
            if (dialogDescriptor.getValue() == DialogDescriptor.OK_OPTION) {
                Task p = processorChooserPanel.getProcessor();
                children.add(p);
                try {
// TODO use the lookup api
                    ApplicationsTopComponent.findInstance().getExplorerManager().setSelectedNodes(new Node[]{children.getNodeAt(children.indexOf(p))});
                } catch (PropertyVetoException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
}
