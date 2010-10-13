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
package org.simexplorer.ide.ui.dataexplorer.factors;

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.openmole.core.implementation.plan.Factor;
import org.openmole.core.model.plan.IFactor;

public class ExperimentalFactorsNode extends AbstractNode {

    Collection<? extends IFactor<?,?>> factors;

    public ExperimentalFactorsNode(Collection<? extends IFactor<?,?>> factors) {
        super(new ExperimentalFactorsChildren(factors), Lookups.singleton(factors));
        this.factors = factors;
    }

    @Override
    public Action[] getActions(boolean arg0) {
        return new Action[]{new NewFactorAction()};
    }

    public class NewFactorAction extends AbstractAction {

        private EditFactorAction action;

        public NewFactorAction() {
            putValue(NAME, NbBundle.getMessage(EditFactorAction.class, "CTL_NewFactorAction"));
            action = new EditFactorAction();
        // TODO this should be the best way, but doesn't work
        //wizardAction = (EditFactorAction) Lookups.forPath("Actions/Exploration").lookup(EditFactorAction.class);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.performAction(getLookup().lookup(Factor.class), false);
        }
    }
}
