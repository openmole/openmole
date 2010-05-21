/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.workflow.model;

import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.ui.workflow.implementation.TaskViewUI;

/**
 *
 * @author mathieu
 */
public interface IMoleScene{

    void setLayout();
    IConnectable createTaskCapsule();
    TaskViewUI createTask(IGenericTask obj);
    void refresh();
    void setMovable(boolean b);
}
