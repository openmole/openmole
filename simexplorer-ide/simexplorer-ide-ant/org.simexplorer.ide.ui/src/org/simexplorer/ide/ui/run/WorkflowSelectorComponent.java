/*
 *
 *  Copyright (c) 2009, Cemagref
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
package org.simexplorer.ide.ui.run;

import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.TreeTableView;
import org.openide.nodes.Node.Property;
import org.simexplorer.ide.ui.applicationexplorer.AbstractProcessorNode;
import org.simexplorer.ide.ui.applicationexplorer.ProcessorsListNode;
import org.simexplorer.ui.ide.workflow.model.ExplorationApplication;
import org.simexplorer.ui.ide.workflow.model.TasksList;

public class WorkflowSelectorComponent extends TreeTableView implements ExplorerManager.Provider {

    private final ExplorerManager explorerManager = new ExplorerManager();
    private ExplorationApplication explorationApplication;

    public WorkflowSelectorComponent(ExplorationApplication application) {
        super();
        this.explorationApplication = application;
        explorerManager.setRootContext(new ProcessorsListNode<TasksList>(application.getTreeRoot()));
        this.setRootVisible(true);
        this.setProperties(new Property[]{new AbstractProcessorNode.RunProperty()});
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }
}
