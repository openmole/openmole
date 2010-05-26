/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.workflow.model.domain;

import org.openmole.core.workflow.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IInterval<T> {

    /**
     *
     * Get the sting representing the upper bound of this domain before evaluation.
     *
     * @return the sting representing the upper bound of this domain befor evaluation
     */
    String getMax();

    /**
     *
     * Get the sting representing the lower bound of this domain before evaluation.
     *
     * @return the sting representing the lower bound of this domain befor evaluation
     */
    String getMin();

    T getMax(IContext context) throws InternalProcessingError, UserBadDataError;
    T getMin(IContext context) throws InternalProcessingError, UserBadDataError;
}
