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
package org.openmole.ide.core.implementation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;
import org.openmole.ide.core.implementation.control.PasswordListner;
import org.openmole.ide.core.implementation.control.TopComponentsManager;
import org.openmole.ide.core.implementation.preference.PreferenceContent;

@ActionID(category = "Edit",
id = "org.openmole.ide.core.implementation.PreferenceAction")
@ActionRegistration(displayName = "#CTL_PreferenceAction")
@ActionReferences({
    @ActionReference(path = "Menu/Tools", position = 1300),
    @ActionReference(path = "Shortcuts", name = "D-P")
})
@Messages("CTL_PreferenceAction=Preferences")
public final class PreferenceAction implements ActionListener {

    static {
        PasswordListner.apply();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        PreferenceContent pc = new PreferenceContent();
        DialogDescriptor dd = new DialogDescriptor(pc.peer(), "Preferences");
        dd.setOptions(new Object[]{DialogDescriptor.OK_OPTION});
        Object result = DialogDisplayer.getDefault().notify(dd);
        if (result.equals(NotifyDescriptor.OK_OPTION)) {
            pc.save();
        }
    }
}
