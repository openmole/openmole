/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.simexplorer.ide.ui.applicationexplorer;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 * Action which shows Applications component.
 */
public class ApplicationsAction extends AbstractAction {

    public ApplicationsAction() {
        super(NbBundle.getMessage(ApplicationsAction.class, "CTL_ApplicationsAction"));
//        putValue(SMALL_ICON, new ImageIcon(Utilities.loadImage(ApplicationsTopComponent.ICON_PATH, true)));
    }

    public void actionPerformed(ActionEvent evt) {
        TopComponent win = ApplicationsTopComponent.findInstance();
        win.open();
        win.requestActive();
    }
}
