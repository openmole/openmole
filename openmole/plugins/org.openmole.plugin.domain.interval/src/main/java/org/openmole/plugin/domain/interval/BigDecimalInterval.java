/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.domain.interval;

import java.math.BigDecimal;
import org.openmole.core.implementation.domain.Interval;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import static org.openmole.core.implementation.tools.VariableExpansion.*;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class BigDecimalInterval extends Interval<BigDecimal> {

    public BigDecimalInterval(String min, String max) {
        super(min, max);
    }

    @Override
    public BigDecimal getMax(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
         return new BigDecimal(expandData(global, context, getMax()));
    }

    @Override
    public BigDecimal getMin(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
         return new BigDecimal(expandData(global, context, getMin()));
    }

}
