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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.domain.Interval;
import org.openmole.core.model.job.IContext;
import org.openmole.misc.math.BigDecimalOperations;

import static org.openmole.core.implementation.tools.VariableExpansion.*;


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
    public List<BigDecimal> computeValues(IContext global, IContext ic) throws InternalProcessingError, UserBadDataError {
        BigDecimal min = BigDecimalOperations.log(getInterval().getMin(global, ic));
        BigDecimal max = BigDecimalOperations.log(getInterval().getMax(global, ic));
        Integer nbstep = new Integer(expandData(global, ic,getNbStep()));
        BigDecimal step = max.subtract(min).abs().divide(new BigDecimal(nbstep),RoundingMode.HALF_UP);

        BigDecimal cur = min;
        List<BigDecimal> val = new ArrayList<BigDecimal>(step.intValue()+1);

        for (int i = 0; i <= nbstep; i++) {
            val.add(BigDecimalOperations.exp(cur));
            cur = cur.add(step);
        }

        return val;
    }

    @Override
    public BigDecimal getRange(IContext global, IContext ic) throws InternalProcessingError, UserBadDataError {
        return getInterval().getMax(global, ic).subtract(getInterval().getMin(global, ic));
    }

}
