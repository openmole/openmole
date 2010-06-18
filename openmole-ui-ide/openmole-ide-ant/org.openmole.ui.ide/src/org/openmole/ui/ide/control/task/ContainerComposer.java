/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.ide.control.task;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class ContainerComposer extends JPanel {

    public ContainerComposer(Set<ContainerComposerBuilder.OrientedComponent> ocs,
            int w,
            int h) {
        setMinimumSize(new Dimension(w, h));
        int divider = 0;
        JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        for (ContainerComposerBuilder.OrientedComponent oc : ocs) {
            if (oc.getSplitOrientation() == splitpane.getOrientation()) {
                splitpane.add(oc.getComponent());
            } else {
                divider = 0;
                splitpane = new JSplitPane(oc.getSplitOrientation(),
                        splitpane,
                        oc.getComponent());

            }
            /*  splitpane = new JSplitPane(oc.getOrientation(),
            splitpane,
            oc.getComponent());*/
            splitpane.setOneTouchExpandable(true);
            splitpane.setDividerLocation(divider);
            divider += 150;
        }
        add(splitpane, BorderLayout.WEST);
    }
}
