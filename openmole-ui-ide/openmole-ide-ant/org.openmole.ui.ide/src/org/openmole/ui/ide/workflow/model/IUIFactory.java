/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model;

import org.openmole.commons.aspect.eventdispatcher.IObjectListener;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.ide.workflow.implementation.TaskUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public interface IUIFactory<T> extends IObjectListener<T> {
    IGenericTaskModelUI createTaskModelInstance(Class<? extends IGenericTaskModelUI> modelClass,TaskUI task) throws UserBadDataError ;    
}
