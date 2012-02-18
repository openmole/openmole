/*
 * Copyright (C) 2012 dumoulin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.core.implementation.toolbar;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import org.openmole.ide.core.implementation.panel.ConceptMenu;

@ActionID(category = "Edit",
id = "org.openmole.ide.core.implementation.toolbar.SamplingsAction")
@ActionRegistration(displayName = "#CTL_SamplingsAction")
@ActionReferences({
    @ActionReference(path = "Toolbars/File", position = 240)
})
@Messages("CTL_SamplingsAction=Samplings")
public class SamplingsAction extends AbstractAction implements Presenter.Toolbar {

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public Component getToolbarPresenter() {
        return  ConceptMenu.samplingMenu().peer();
    }

}
