/*
 * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
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
package org.openmole.ui.ide.dialog;

import java.awt.Dialog;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

/**
 *
 * @author mathieu leclaire <mathieu.leclaire@openmole.org>
 */
public class DialogSupport {

    private DialogDescriptor dialogDescriptor;

    public static void showDialog(javax.swing.JPanel panel) {

        DialogDescriptor dialogDescriptor = new DialogDescriptor(panel, panel.getName());
        dialogDescriptor.setValid(false);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
    }
}
