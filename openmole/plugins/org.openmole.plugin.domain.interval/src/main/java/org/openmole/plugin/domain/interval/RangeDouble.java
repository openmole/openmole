/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.plugin.domain.interval;

import java.util.ArrayList;
import java.util.List;
import org.openmole.core.implementation.domain.Interval;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.tools.VariableExpansion;
import org.openmole.core.model.job.IContext;

public class RangeDouble extends UniformelyDiscretizedIntervalDomain<Double> {

    public RangeDouble() {
        this("0.0","1.0");
    }

    public RangeDouble(String min, String max, String step) {
        this(new DoubleInterval(min, max), step);
    }

    public RangeDouble(String min, String max) {
        this(min, max, "1.0");
    }

    public RangeDouble(Interval<Double> interval, String step) {
        super(interval, step);
    }

    @Override
    public List<Double> computeValues(IContext context) throws InternalProcessingError, UserBadDataError {
        Double min = getInterval().getMin(context);
        Double max = getInterval().getMax(context);
        Double step = new Double(VariableExpansion.getInstance().expandData(context, getStep()));

        int size = new Double(Math.abs(max - min) / step).intValue();

        List<Double> val = new ArrayList<Double>(size);

        double cur = min;

        for (int i = 0; i <= size; i++) {
            val.add(cur);
            cur += step;
        }
        return val;
    }

  

    @Override
    public Double getCenter(IContext context) throws InternalProcessingError, UserBadDataError {
        Double min = getInterval().getMin(context);
        return min + ((getInterval().getMax(context) - min) / 2);
    }

    @Override
    public Double getRange(IContext context) throws InternalProcessingError, UserBadDataError {
        return getInterval().getMax(context) - getInterval().getMin(context);
    }
}
