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
package org.openmole.core.implementation.execution;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openmole.core.model.execution.IStatistic;
import org.openmole.core.model.execution.batch.SampleType;
import org.openmole.commons.tools.stat.FixedSizeList;

public class Statistic implements IStatistic {

    public static IStatistic EMPTY_STAT = new Statistic(0);

    Map<SampleType, FixedSizeList> averages = Collections.synchronizedMap(new TreeMap<SampleType, FixedSizeList>());
    Integer historySize;

    public Statistic(Integer historySize) {
        super();
        this.historySize = historySize;
    }

    @Override
    public List<Long> getSamples(SampleType type) {
        FixedSizeList av = averages.get(type);
        if(av == null) return Collections.EMPTY_LIST;
        return av.getValues();
    }

    @Override
    public synchronized void sample(SampleType type, Long length) {
        FixedSizeList av = averages.get(type);
        if (av == null) {
            av = new FixedSizeList(historySize);
            averages.put(type, av);
        }
        av.add(length);
    }

}
