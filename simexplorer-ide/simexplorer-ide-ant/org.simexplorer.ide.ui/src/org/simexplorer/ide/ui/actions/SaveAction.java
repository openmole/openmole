/*
 *
 *  Copyright (c) 2008, Cemagref
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;
import org.simexplorer.ui.ide.workflow.model.ExplorationApplication;
import org.simexplorer.ui.ide.workflow.model.ExplorationsManager;

public final class SaveAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ExplorationApplication application = ApplicationsTopComponent.findInstance().getExplorationApplication();
        if (application.isSaved()) {
            try {
                ExplorationsManager.saveApplication(application);
            } catch (IOException ex) {
                Logger.getLogger(SaveAction.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            SaveAsAction.doAction();
        }
    }
}
