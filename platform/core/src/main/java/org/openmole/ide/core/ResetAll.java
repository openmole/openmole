/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;
import org.openmole.ide.core.properties.Displays;
import org.openmole.ide.core.palette.ElementFactories;
import org.openmole.ide.core.control.MoleScenesManager;

@ActionID(category = "File",
id = "org.openmole.ide.core.ResetAll")
@ActionRegistration(displayName = "#CTL_ResetAll")
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 2000)
})
@Messages("CTL_ResetAll=Reset")
public final class ResetAll implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        MoleScenesManager.removeMoleScenes();
        ElementFactories.clearAll();
        MoleScenesManager.display(MoleScenesManager.addMoleScene());
        MoleSceneTopComponent.getDefault().refreshPalette();
      //  Displays.propertyPanel.hidePanelScrollPane();
        // TODO implement action body
        
    }
}
