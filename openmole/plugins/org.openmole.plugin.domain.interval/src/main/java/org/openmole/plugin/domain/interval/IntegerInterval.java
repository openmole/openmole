/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.domain.interval;

import org.openmole.core.implementation.domain.Interval;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import static org.openmole.core.implementation.tools.VariableExpansion.*;
/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class IntegerInterval extends Interval<Integer> {

    public IntegerInterval(String min, String max) {
        super(min, max);
    }

    @Override
    public Integer getMax(IContext context) throws InternalProcessingError, UserBadDataError {
         return new Integer(expandData(context, getMax()));
    }

    @Override
    public Integer getMin(IContext context) throws InternalProcessingError, UserBadDataError {
       return new Integer(expandData(context, getMin()));
    }



}
