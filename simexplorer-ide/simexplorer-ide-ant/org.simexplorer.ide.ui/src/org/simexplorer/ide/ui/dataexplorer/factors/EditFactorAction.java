/*
 *  Copyright © 2008, 2009, Cemagref
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
package org.simexplorer.ide.ui.dataexplorer.factors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openmole.commons.exception.InternalProcessingError;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;
import org.openmole.core.workflow.implementation.plan.Factor;

public final class EditFactorAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        performAction(null, false);
    }

    public void performAction(Factor factor, boolean isDuplicated) {
        try {
            NewFactorPanel panel = new NewFactorPanel(factor, isDuplicated);
            if (panel.showDialog(NbBundle.getMessage(EditFactorAction.class, "CTL_NewFactorAction"))) {
                if ((factor == null) || (isDuplicated)) {
                    ApplicationsTopComponent.findInstance().getExplorationApplication().getFactors().add(panel.getFactor());
                }
                panel.getFactor();
                FactorsExplorerTopComponent.findInstance().applicationUpdated();
            }
        } catch (InternalProcessingError ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}

