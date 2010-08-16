/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
import org.openmole.core.implementation.domain.Interval;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.job.IContext;

import static org.openmole.core.implementation.tools.VariableExpansion.*;


public class RangeBigDecimal extends UniformelyDiscretizedIntervalDomain<BigDecimal> {

    public RangeBigDecimal(String min, String max, String step) {
        this(new BigDecimalInterval(min, max), step);
    }

    public RangeBigDecimal(String min, String max) {
        this(min, max, "1");
    }

    public RangeBigDecimal(Interval<BigDecimal> interval, String step) {
        super(interval, step);
    }

    @Override
    public List<BigDecimal> computeValues(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
        BigDecimal min = getInterval().getMin(global, context);
        BigDecimal max = getInterval().getMax(global, context);
        BigDecimal step = new BigDecimal(expandData(global, context, getStep()));

        int size = max.subtract(min).abs().divide(step,RoundingMode.HALF_UP).intValue();
        BigDecimal cur = min;


        List<BigDecimal> val = new ArrayList<BigDecimal>(size);

        for (int i = 0; i <= size; i++) {
            val.add(cur);
            cur = cur.add(step);
        }

        return val;
    }

 
    @Override
    public BigDecimal getCenter(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
        BigDecimal min = getInterval().getMin(global, context);
        return min.add((getInterval().getMax(global, context).subtract(min).divide(new BigDecimal(2.0))));
    }

    @Override
    public BigDecimal getRange(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
        return getInterval().getMax(global, context).subtract(getInterval().getMin(global, context));
    }
}
