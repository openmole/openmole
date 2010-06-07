/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.workflow.model;

import org.openmole.commons.aspect.eventdispatcher.IObjectConstructedAsynchronousListener;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.task.IGenericTask;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public interface IUIFactory<T> extends IObjectConstructedAsynchronousListener<T> {
    IGenericTask createCoreTaskInstance(Class<? extends IGenericTask> taskClass) throws UserBadDataError;
    IGenericTaskModelUI createTaskModelInstance(Class<? extends IGenericTaskModelUI> modelClass) throws UserBadDataError ;

    
}
