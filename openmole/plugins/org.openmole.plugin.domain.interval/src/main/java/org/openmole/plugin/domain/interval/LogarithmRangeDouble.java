/*
 *  Copyright (C) 2010 leclaire
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

import java.util.ArrayList;
import java.util.List;
import org.openmole.core.implementation.domain.Interval;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import static org.openmole.core.implementation.tools.VariableExpansion.*;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class LogarithmRangeDouble extends LogarithmIntervalDomain<Double>{

    LogarithmRangeDouble(){
        this("1","1000","100");
    }

    public LogarithmRangeDouble(String min, String max, String nbStep) {
        this(new DoubleInterval(min,max), nbStep);
    }

    public LogarithmRangeDouble(Interval<Double> interval, String nbStep) {
        super(interval, nbStep);
    }

    @Override
    public Double getRange(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
        return getInterval().getMax(global, context) - getInterval().getMin(global,context);
    }

    @Override
    public List<Double> computeValues(IContext global, IContext context) throws InternalProcessingError, UserBadDataError {
        Double min = Math.log(getInterval().getMin(global, context));
        Double max = Math.log(getInterval().getMax(global, context));
        Integer nbstep = new Integer(expandData(global, context,getNbStep()));
        Double step = new Double(Math.abs(max - min)) / nbstep;

        List<Double> val = new ArrayList<Double>(step.intValue()+1);
        Double cur = min;
        
        for (int i = 0; i <= nbstep; i++) {
            val.add(Math.exp(cur));
            cur += step;
        }

        return val;
    }

}
