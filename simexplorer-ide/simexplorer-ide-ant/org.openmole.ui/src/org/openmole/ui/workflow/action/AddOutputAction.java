package org.openmole.ui.workflow.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openmole.ui.workflow.model.IConnectable;

/**
 *
 * @author mathieu
 */
public class AddOutputAction implements ActionListener {
     private IConnectable connectable;

     public AddOutputAction(IConnectable c){
        connectable = c;
     }

    @Override
    public void actionPerformed(ActionEvent ae) {
        connectable.addOutputSlot();
    }
}
