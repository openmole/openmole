/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.workflow.action

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.workflow.implementation.TaskUI

class AddTaskAction(moleScene: MoleScene ,capsuleView: CapsuleViewUI,taskUI: TaskUI) extends ActionListener{
  override def actionPerformed(ae: ActionEvent)= {
    capsuleView.encapsule(taskUI)
    moleScene.validate
    moleScene.refresh
        
  }
}
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import org.openmole.commons.exception.UserBadDataError;
//import org.openmole.ide.core.exception.MoleExceptionManagement;
//import org.openmole.ide.core.workflow.implementation.MoleScene;
//import org.openmole.ide.core.workflow.implementation.CapsuleViewUI;
//import org.openmole.ide.core.workflow.implementation.TaskUI;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class AddTaskAction implements ActionListener {
//
//    MoleScene moleScene;
//    TaskUI taskUI;
//    private CapsuleViewUI capsuleView;
//
//    public AddTaskAction(MoleScene moleScene,
//            CapsuleViewUI capsuleView,
//            TaskUI taskUI) {
//        this.moleScene = moleScene;
//        this.capsuleView = capsuleView;
//        this.taskUI = taskUI;
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent ae) {
//        try {
//            capsuleView.encapsule(taskUI);
//        } catch (UserBadDataError ex) {
//            MoleExceptionManagement.showException(ex);
//        }
//        moleScene.validate();
//        moleScene.refresh();
//    }
//}