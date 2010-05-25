package org.openmole.ui.workflow.provider;

import javax.swing.JMenuItem;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author mathieu
 */
public class TaskMenuProvider extends GenericMenuProvider {

    public TaskMenuProvider(IGenericTaskModelUI tmodel) {
       // super(tmodel);
        super();
        items.add(new JMenuItem("TO BE IMPLEMENTED"));
    }
}
