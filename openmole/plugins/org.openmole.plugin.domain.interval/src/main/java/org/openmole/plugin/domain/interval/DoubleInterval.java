/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.domain.interval;

import org.openmole.core.workflow.implementation.domain.Interval;
import org.openmole.core.workflow.implementation.tools.VariableExpansion;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class DoubleInterval extends Interval<Double>{

    public DoubleInterval(String min, String max) {
        super(min, max);
    }

    @Override
    public Double getMax(IContext context) throws InternalProcessingError, UserBadDataError {
         return new Double(VariableExpansion.getInstance().expandData(context, getMax()));
    }

    @Override
    public Double getMin(IContext context) throws InternalProcessingError, UserBadDataError {
         return new Double(VariableExpansion.getInstance().expandData(context, getMin()));
    }

}
