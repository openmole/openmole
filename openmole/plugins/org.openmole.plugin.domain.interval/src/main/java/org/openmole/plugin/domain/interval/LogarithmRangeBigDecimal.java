/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain.interval;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.domain.Interval;
import org.openmole.core.implementation.tools.VariableExpansion;
import org.openmole.core.model.job.IContext;
import org.openmole.misc.math.BigDecimalOperations;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class LogarithmRangeBigDecimal extends LogarithmIntervalDomain<BigDecimal>{

    public LogarithmRangeBigDecimal(Interval<BigDecimal> interval, String nbStep) {
        super(interval, nbStep);
    }

    public LogarithmRangeBigDecimal(String min, String max, String nbStep) {
        this(new BigDecimalInterval(min, max), nbStep);
    }

    @Override
    //TODO: use a gopd implementation of bigdecimal operations. Operation currently return double
    public List<BigDecimal> computeValues(IContext ic) throws InternalProcessingError, UserBadDataError {
        BigDecimal min = BigDecimalOperations.log(getInterval().getMin(ic));
        BigDecimal max = BigDecimalOperations.log(getInterval().getMax(ic));
        Integer nbstep = new Integer(VariableExpansion.expandData(ic,getNbStep()));
        BigDecimal step = max.subtract(min).abs().divide(new BigDecimal(nbstep));

        BigDecimal cur = min;
        List<BigDecimal> val = new ArrayList<BigDecimal>(step.intValue()+1);

        for (int i = 0; i <= nbstep; i++) {
            val.add(BigDecimalOperations.exp(cur));
            cur.add(step);
        }

        return val;
    }

    @Override
    public BigDecimal getCenter(IContext ic) throws InternalProcessingError, UserBadDataError {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BigDecimal getRange(IContext ic) throws InternalProcessingError, UserBadDataError {
        return getInterval().getMax(ic).subtract(getInterval().getMin(ic));
    }

}
