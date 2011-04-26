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
import org.openmole.ide.core.workflow.model.IEntityUI
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.workflow.implementation.PrototypeUI

class AddExistingPrototypeAction(prototype: IEntityUI,capsuleViewUI: CapsuleViewUI,t: IOType.Value) extends ActionListener{

  override def actionPerformed(ae: ActionEvent) {
    prototype match {
      case p: PrototypeUI=> capsuleViewUI.capsuleModel.taskUI.get.addPrototype(p, t)
    }
    capsuleViewUI.repaint
  }
}
//
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import org.openmole.ide.core.commons.IOType;
//import org.openmole.ide.core.workflow.implementation.PrototypeUI;
//import org.openmole.ide.core.workflow.implementation.CapsuleViewUI;
//import org.openmole.ide.core.workflow.implementation.IEntityUI;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class AddExistingPrototypeAction implements ActionListener {
//
//    private PrototypeUI prototype;
//    private CapsuleViewUI capsuleViewUI;
//    private IOType type;
//
//    public AddExistingPrototypeAction(IEntityUI prototype,
//                                CapsuleViewUI capsuleViewUI,
//                                IOType type) {
//        this.prototype = (PrototypeUI) prototype;
//        this.capsuleViewUI = capsuleViewUI;
//        this.type = type;
//    }
//    
//    @Override
//    public void actionPerformed(ActionEvent ae) {
//        capsuleViewUI.getCapsuleModel().getTaskModel().addPrototype(prototype, type);
//        capsuleViewUI.repaint();
//    }
//
//}