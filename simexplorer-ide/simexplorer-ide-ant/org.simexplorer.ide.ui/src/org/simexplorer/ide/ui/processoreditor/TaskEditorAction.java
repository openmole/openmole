/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simexplorer.ide.ui.processoreditor;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 * Action which shows TaskEditor component.
 */
public class TaskEditorAction extends AbstractAction {

    public TaskEditorAction() {
        super(NbBundle.getMessage(TaskEditorAction.class, "CTL_TaskEditorAction"));
//        putValue(SMALL_ICON, new ImageIcon(Utilities.loadImage(TaskEditorTopComponent.ICON_PATH, true)));
    }

    public void actionPerformed(ActionEvent evt) {
        TopComponent win = TaskEditorTopComponent.findInstance();
        win.open();
        win.requestActive();
    }
}
