/*
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
package org.simexplorer.ide.ui.dataexplorer.variables;

import java.util.Collection;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openmole.core.model.data.IPrototype;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;

class VariablesChildren extends Children.Keys<IPrototype> {

    private Collection<IPrototype> variables;

    public VariablesChildren(Collection<IPrototype> variables) {
        this.variables = variables;
        // TODO maybe fix the complexdata converter for correctly init heap at deserialization
        if ((variables != null)) {
            setKeys(variables);
        }
    }

    @Override
    protected Node[] createNodes(IPrototype variable) {
        return new Node[]{new VariableNode(variable, this)};
    }

    void removeVariable(IPrototype variable) {
        ApplicationsTopComponent.findInstance().getExplorationApplication().removeContract(variable);
        setKeys(variables);
    }
}
