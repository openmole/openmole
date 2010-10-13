/*
 *  Copyright © 2009, Cemagref
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

import java.io.IOException;
import org.simexplorer.ui.tools.ActionsUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import org.simexplorer.ui.ide.workflow.model.ExplorationsManager;

public final class SaveAsAction implements ActionListener {
    private static String fileExtension="xml";

    @Override
    public void actionPerformed(ActionEvent e) {
        doAction();
    }

    public static void doAction() {
        JFileChooser fc = ActionsUtils.getJFileChooser("Save as", ApplicationsTopComponent.findInstance().getExplorationApplication().getFileSaved(),
                "SimExplorer file type",fileExtension);
        if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                File file=fc.getSelectedFile();
                if (!file.getName().endsWith("."+fileExtension)) file=new File(file.getPath()+"."+fileExtension);
                ExplorationsManager.saveApplication(ApplicationsTopComponent.findInstance().getExplorationApplication(),file);
            } catch (IOException ex) {
                Logger.getLogger(SaveAsAction.class.getName()).log(Level.SEVERE, "Input/Output error while trying to save your application to : ", ex);
            }
        }
    }
}
