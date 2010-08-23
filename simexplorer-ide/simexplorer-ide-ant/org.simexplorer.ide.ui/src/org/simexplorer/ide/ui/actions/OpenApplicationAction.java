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
package org.simexplorer.ide.ui.actions;

import org.simexplorer.ui.tools.ActionsUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;
import java.io.FileNotFoundException;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.openide.windows.WindowManager;
import org.simexplorer.ui.ide.workflow.model.ExplorationApplication;
import org.simexplorer.ui.ide.workflow.model.ExplorationsManager;

public final class OpenApplicationAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        int n = JOptionPane.YES_OPTION;
        ExplorationApplication application = ApplicationsTopComponent.findInstance().getExplorationApplication();
        if (application != null) {
            n = JOptionPane.showOptionDialog(WindowManager.getDefault().getMainWindow(),
                    "Do you want to erase the existing Exploration application?",
                    "Erase warning",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.NO_OPTION);

        }
        if (n == JOptionPane.YES_OPTION) {
            JFileChooser fc = ActionsUtils.getJFileChooser("Open", application !=
                    null ? application.getFileSaved() : null,"SimExplorer file type","xml");
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                try {
                    ExplorationApplication explorationApplication = ExplorationsManager.loadApplication(fc.getSelectedFile());
                    ApplicationsTopComponent.findInstance().setApplication(explorationApplication);
                // TODO dispatch R connection, see IDEPilot.loadIDEApplication(FileReader filereader)
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(OpenApplicationAction.class.getName()).log(java.util.logging.Level.SEVERE, "Input/Output error while trying to load your file : " +
                            fc.getSelectedFile(), ex);
                }
            }
        }
    }
}
