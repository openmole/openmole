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

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.openmole.core.model.data.IPrototype;

class VariablesNode extends AbstractNode {

    VariablesNode(Collection<IPrototype> variables) {
        super(new VariablesChildren(variables), Lookups.singleton(variables));
    }
    @Override
    public Action[] getActions(boolean arg0) {
            return new Action[]{new EditAction()};
    }

    private class EditAction extends AbstractAction {

        private EditVariableAction action;

        public EditAction() {
            putValue(NAME, NbBundle.getMessage(EditVariableAction.class, "CTL_NewVariableAction"));
            action = new EditVariableAction();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.actionPerformed(e);
        }
    }


}
