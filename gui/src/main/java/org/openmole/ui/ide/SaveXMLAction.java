/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.ui.ide;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.serializer.GUISerializer;

public final class SaveXMLAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            GUISerializer.getInstance().serialize("/tmp/mole.xml");
        } catch (IOException ex) {
            MoleExceptionManagement.showException(ex);
        }
    }
}
