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

package org.openmole.ui.ide.dialog

import javax.swing.JButton
import javax.swing.JPanel
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer

object DialogSupport {

  def showDialog(panel: JPanel)= {
    val dialogDescriptor = new DialogDescriptor(panel,panel.getName,true,Array[Object](new JButton("Close")),null,DialogDescriptor.DEFAULT_ALIGN,null,null)
    dialogDescriptor.setValid(false)
    val dialog = DialogDisplayer.getDefault.createDialog(dialogDescriptor)
    dialog.setVisible(true)
    dialog.toFront
  }
}


//import java.awt.Dialog;
//import javax.swing.JButton;
//import org.openide.DialogDescriptor;
//import org.openide.DialogDisplayer;
//
///**
// *
// * @author mathieu leclaire <mathieu.leclaire@openmole.org>
// */
//public class DialogSupport {
//
//    public static void showDialog(javax.swing.JPanel panel) {
//        Object[] options = {new JButton("Close")};
//
//        DialogDescriptor dialogDescriptor = new DialogDescriptor(panel,
//                panel.getName(),
//                true,
//                options,
//                null,
//                DialogDescriptor.DEFAULT_ALIGN,
//                null,
//                null);
//
//        dialogDescriptor.setValid(false);
//        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
//        dialog.setVisible(true);
//        dialog.toFront();
//    }
//}