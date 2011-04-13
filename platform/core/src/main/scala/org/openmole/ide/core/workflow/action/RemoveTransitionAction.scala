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

import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import org.openmole.ide.core.workflow.implementation.MoleScene

class RemoveTransitionAction(scene: MoleScene, edgeID: String) extends ActionListener{

  override def actionPerformed(ae: ActionEvent)= {
    scene.manager.removeTransition(edgeID)
    scene.removeEdge(edgeID)
  }
}
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import org.openmole.ide.core.workflow.implementation.MoleScene;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
// */
//public class RemoveTransitionAction implements ActionListener{
//    private MoleScene scene;
//    private String edgeID;
//
//    public RemoveTransitionAction(MoleScene scene, String edgeID) {
//        this.scene = scene;
//        this.edgeID = edgeID;
//    }
//    
//    @Override
//    public void actionPerformed(ActionEvent ae) {
//        scene.getManager().removeTransition(edgeID);
//        scene.removeEdge(edgeID);
//    }
//}