/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.workflow.model;

import org.netbeans.api.visual.widget.Widget;
import org.openmole.ui.workflow.implementation.MoleSceneManager;

/**
 *
 * @author mathieu
 */
public interface IMoleScene{

    void setLayout();
   // IConnectable createTaskCapsule();
   // TaskViewUI createTask(IGenericTask obj);
    void refresh();
    void setMovable(boolean b);
    MoleSceneManager getManager();
    void initCompositeAdd(Widget w);
    void initCapsuleAdd(Widget w);
}
