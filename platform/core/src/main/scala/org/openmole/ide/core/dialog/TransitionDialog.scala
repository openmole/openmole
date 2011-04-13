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

package org.openmole.ide.core.dialog

import javax.swing.JOptionPane
import org.openmole.ide.core.workflow.implementation.TransitionUI
import org.openmole.ide.core.workflow.implementation.paint.LabeledConnectionWidget

object TransitionDialog {
  def displayTransitionDialog(transition: TransitionUI,connectionWidget: LabeledConnectionWidget) {
    val cond = JOptionPane.showInputDialog(null, "Edit transition condition:", transition.condition)
    if (cond.isEmpty) transition.condition = None
    else transition.condition = Some(cond)
    connectionWidget.setConditionLabel(transition.condition)
    connectionWidget.scene.validate
  }
}

//import javax.swing.JOptionPane;
//import org.openmole.ide.core.workflow.implementation.TransitionUI;
//import org.openmole.ide.core.workflow.implementation.paint.LabeledConnectionWidget;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
// */
//public class TransitionDialog {
//
//    public static void displayTransitionDialog(TransitionUI transition, LabeledConnectionWidget connectionWidget) {
//        String cond = JOptionPane.showInputDialog(null, "Edit transition condition:", transition.getCondition());
//        if (cond != null) {
//            connectionWidget.setConditionLabel(cond);
//            transition.setCondition(cond);
//            connectionWidget.getScene().validate();
//        }
//    }
//}