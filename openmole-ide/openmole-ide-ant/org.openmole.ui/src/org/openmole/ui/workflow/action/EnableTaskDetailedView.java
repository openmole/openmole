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

package org.openmole.ui.workflow.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import org.openmole.ui.workflow.implementation.MoleScene;
import org.openmole.ui.workflow.model.ITaskCapsuleView;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class EnableTaskDetailedView implements ActionListener{

    private MoleScene scene;

    public EnableTaskDetailedView(MoleScene scene) {
        this.scene = scene;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        AbstractButton button = (AbstractButton)ae.getSource();
        scene.setDetailedView(button.isSelected());
        for(ITaskCapsuleView cv:scene.getManager().getTaskViews()){
            cv.getConnectableWidget().setWidthHint();
        }
        scene.validate();
        scene.refresh();
        button.setText(button.isSelected() ? "Show detailed view":"Hide detailed view");
    }

}
