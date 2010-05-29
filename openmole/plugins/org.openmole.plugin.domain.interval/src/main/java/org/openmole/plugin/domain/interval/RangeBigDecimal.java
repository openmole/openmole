/*
 *  Copyright (C) 2010 reuillon
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
import org.openmole.core.implementation.domain.Interval;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.tools.VariableExpansion;
import org.openmole.core.workflow.model.job.IContext;

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
    public List<BigDecimal> computeValues(IContext context) throws InternalProcessingError, UserBadDataError {
        BigDecimal min = getInterval().getMin(context);
        BigDecimal max = getInterval().getMax(context);
        BigDecimal step = new BigDecimal(VariableExpansion.getInstance().expandData(context, getStep()));

        int size = max.subtract(min).abs().divide(step).intValue();
        BigDecimal cur = min;


        List<BigDecimal> val = new ArrayList<BigDecimal>(size);

        for (int i = 0; i <= size; i++) {
            val.add(cur);
            cur = cur.add(step);
        }

        return val;
    }

 
    @Override
    public BigDecimal getCenter(IContext context) throws InternalProcessingError, UserBadDataError {
        BigDecimal min = getInterval().getMin(context);
        return min.add((getInterval().getMax(context).subtract(min).divide(new BigDecimal(2.0))));
    }

    @Override
    public BigDecimal getRange(IContext context) throws InternalProcessingError, UserBadDataError {
        return getInterval().getMax(context).subtract(getInterval().getMin(context));
    }
}
