/*
 *
 *  Copyright © 2008, 2009, Cemagref
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
package org.simexplorer.ide.ui.actions;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import org.openide.util.NbBundle;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;
import org.openide.windows.WindowManager;
import org.simexplorer.ide.ui.wizards.newApplication.NewApplicationPanel;
import org.simexplorer.ui.ide.workflow.model.ExplorationApplication;

public final class NewApplicationAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // test if an application allready exist
        int n = JOptionPane.YES_OPTION;
        if (ApplicationsTopComponent.findInstance().getExplorationApplication() !=
                null) {
            n = JOptionPane.showOptionDialog(WindowManager.getDefault().getMainWindow(),
                    "Do you want to erase the existing Exploration application?",
                    "Erase warning",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.NO_OPTION);
        }
        if (n == JOptionPane.YES_OPTION) {
            NewApplicationPanel newAppPanel = new NewApplicationPanel();
            if (newAppPanel.showDialog(NbBundle.getMessage(NewApplicationAction.class, "CTL_NewApplicationAction"))) {
                try {
                    ExplorationApplication explorationApplication = new ExplorationApplication(newAppPanel.getApplicationName());
                    ApplicationsTopComponent.findInstance().setApplication(explorationApplication);
                } catch (InternalProcessingError ex) {
                    Logger.getLogger(NewApplicationAction.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UserBadDataError ex) {
                    Logger.getLogger(NewApplicationAction.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
