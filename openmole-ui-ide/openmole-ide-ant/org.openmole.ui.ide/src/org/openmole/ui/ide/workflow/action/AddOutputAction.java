package org.openmole.ui.ide.workflow.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openmole.ui.ide.workflow.model.ICapsuleView;

/**
 *
 * @author mathieu
 */
public class AddOutputAction implements ActionListener {
     private ICapsuleView connectable;

     public AddOutputAction(ICapsuleView c){
        connectable = c;
     }

    @Override
    public void actionPerformed(ActionEvent ae) {
        connectable.addOutputSlot();
    }
}
