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
import org.openmole.core.workflow.implementation.capsule.TaskCapsule;
import org.openmole.ui.workflow.implementation.MoleScene;
import org.openmole.ui.workflow.implementation.UIFactory;
import org.openmole.ui.workflow.model.ITaskCapsuleView;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class AddTaskCapsuleAction implements ActionListener {

    MoleScene moleScene;

    public AddTaskCapsuleAction(MoleScene moleScene) {
        this.moleScene = moleScene;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {


            System.out.println("-----------------------SOUSCE " + moleScene.getLocation().x);

       // IConnectable c = moleScene.createTaskCapsule();
        ITaskCapsuleView c = UIFactory.getInstance().createTaskCapsule(moleScene,
                                                                      moleScene.getLocation());
        c.setTaskCapsule(new TaskCapsule());
        moleScene.refresh();
    }

}
