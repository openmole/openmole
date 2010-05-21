/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simexplorer.ide.ui.run;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.simexplorer.ui.ide.workflow.model.ExplorationsManager;

public final class StopExplorationAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        ExplorationsManager.stop();
    }
}
