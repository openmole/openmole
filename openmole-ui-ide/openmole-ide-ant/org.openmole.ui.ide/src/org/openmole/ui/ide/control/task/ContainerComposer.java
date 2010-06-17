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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class ContainerComposer extends JPanel{
    JSplitPane splitpane;


    public ContainerComposer(Component...cmps) {
        splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
       // setLayout(new BorderLayout());
        int divider = 0;
        for(Component co : cmps){
            splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				      splitpane,
                                      co);
            splitpane.setOneTouchExpandable(true);
        splitpane.setDividerLocation(divider);
        divider += 150;
        }
        add(splitpane,BorderLayout.EAST);
    }

    
}
