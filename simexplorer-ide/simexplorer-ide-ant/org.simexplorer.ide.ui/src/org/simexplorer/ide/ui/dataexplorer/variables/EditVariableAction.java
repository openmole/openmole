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
import java.awt.event.ActionListener;
import org.openide.util.NbBundle;
import org.openmole.core.model.data.IPrototype;

public final class EditVariableAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        performAction(null);
    }

    public void performAction(IPrototype variable) {
        NewVariablePanel newVariablePanel = new NewVariablePanel(variable);
        String title = variable == null ? NbBundle.getMessage(EditVariableAction.class, "CTL_NewVariableAction") : NbBundle.getMessage(EditVariableAction.class, "CTL_EditVariableAction");
        if (newVariablePanel.showDialog(title)) {
            IPrototype newVariable = newVariablePanel.getVariable();
            if (variable == null) {
                VariablesTopComponent.findInstance().addOrSet(newVariable);
            } else {
                VariablesTopComponent.findInstance().applicationUpdated();
            }
        }
    }
}
