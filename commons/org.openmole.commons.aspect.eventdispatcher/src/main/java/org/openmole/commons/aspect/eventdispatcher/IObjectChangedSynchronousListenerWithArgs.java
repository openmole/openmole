/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.commons.aspect.eventdispatcher;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author reuillon
 */
public interface IObjectChangedSynchronousListenerWithArgs<T> extends IObjectChangedListener<T> {
      void objectChanged(T obj, Object[] args) throws InternalProcessingError, UserBadDataError;
}
