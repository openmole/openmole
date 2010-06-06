/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.workflow.model;

import org.openmole.core.workflow.model.task.IGenericTask;
import org.openmole.misc.eventdispatcher.IObjectConstructedAsynchronousListener;
import org.openmole.misc.exception.UserBadDataError;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public interface IUIFactory<T> extends IObjectConstructedAsynchronousListener<T> {
    IGenericTask createCoreTaskInstance(Class<? extends IGenericTask> taskClass) throws UserBadDataError;
    IGenericTaskModelUI createTaskModelInstance(Class<? extends IGenericTaskModelUI> modelClass) throws UserBadDataError ;

    
}
