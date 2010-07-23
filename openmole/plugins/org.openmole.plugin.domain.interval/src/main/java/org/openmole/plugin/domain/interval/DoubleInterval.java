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
public class DoubleInterval extends Interval<Double>{

    public DoubleInterval(String min, String max) {
        super(min, max);
    }

    @Override
    public Double getMax(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
         return new Double(expandData(global, context, getMax()));
    }

    @Override
    public Double getMin(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
         return new Double(expandData(global, context, getMin()));
    }

}
