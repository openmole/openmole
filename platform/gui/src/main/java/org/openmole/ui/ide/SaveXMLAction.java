/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.ui.ide;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openmole.ui.ide.serializer.GUISerializer;

public final class SaveXMLAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
            GUISerializer.serialize("/tmp/mole.xml");
    }
}
