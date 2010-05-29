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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.openmole.core.workflow.model.execution.IEnvironmentExecutionStatistics;
import org.openmole.core.workflow.model.execution.batch.SampleType;
import org.openmole.core.workflow.model.execution.IJobStatisticCategory;
import org.openmole.core.workflow.model.execution.IStatistic;
import org.openmole.core.workflow.model.mole.IExecutionContext;

public class EnvironmentExecutionStatistics implements IEnvironmentExecutionStatistics {

    final Map<IExecutionContext, Map<IJobStatisticCategory, IStatistic>> stats = new WeakHashMap<IExecutionContext, Map<IJobStatisticCategory, IStatistic>>();
    final Integer historySize;

    public EnvironmentExecutionStatistics(Integer historySize) {
        super();
        this.historySize = historySize;
    }

    @Override
    public IStatistic getStatFor(IExecutionContext executionContext, IJobStatisticCategory statisticCategory) {
        Map<IJobStatisticCategory, IStatistic> map = getMapForExecution(executionContext);
        if (map == null) {
            return Statistic.EMPTY_STAT;
        }

        IStatistic ret;
        synchronized (map) {
            ret = map.get(statisticCategory);
        }

        if (ret == null) {
            return Statistic.EMPTY_STAT;
        }
        return ret;
    }

    @Override
    public void statusJustChanged(SampleType type, long length, IExecutionContext executionContext, IJobStatisticCategory statisticCategory) {
        //ITicket statTicket = ticket.getParent().isRoot()?ticket:ticket.getParent();

        IStatistic statForTask = getOrConstructStatFor(executionContext, statisticCategory);
        statForTask.sample(type, length);

        //Logger.getLogger(EnvironmentExecutionStatistics.class.getName()).log(Level.INFO, "New sample " + type.getLabel() + " nb samples " + statForTask.getNbSamples(type));

    }

    private IStatistic getOrConstructStatFor(IExecutionContext executionContext, IJobStatisticCategory statisticCategory) {
        IStatistic statForTask;
        Map<IJobStatisticCategory, IStatistic> map = getMapForExecution(executionContext);


        synchronized (map) {
            statForTask = map.get(statisticCategory);

            if (statForTask == null) {
                statForTask = new Statistic(historySize);
                map.put(statisticCategory, statForTask);
            }
        }

        return statForTask;
    }

    private Map<IJobStatisticCategory, IStatistic> getMapForExecution(IExecutionContext executionContext) {

        synchronized (stats) {
            Map<IJobStatisticCategory, IStatistic> ret = stats.get(executionContext);

            if (ret == null) {
                ret = new HashMap<IJobStatisticCategory, IStatistic>();
                stats.put(executionContext, ret);
            }

            return ret;
        }

    }
}




