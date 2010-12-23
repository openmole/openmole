/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model;

import org.openmole.commons.aspect.eventdispatcher.IObjectListener;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public interface IUIFactory<T> extends IObjectListener<T> {
    IGenericTaskModelUI createTaskModelInstance(Class<? extends IGenericTaskModelUI> modelClass) throws UserBadDataError ;

    
}
