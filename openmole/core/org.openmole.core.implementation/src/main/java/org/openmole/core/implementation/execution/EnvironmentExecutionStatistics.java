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
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.collections15.keyvalue.MultiKey;

import org.openmole.core.model.execution.IEnvironmentExecutionStatistics;
import org.openmole.core.model.execution.batch.SampleType;
import org.openmole.core.model.execution.IStatistic;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.task.IGenericTask;

public class EnvironmentExecutionStatistics implements IEnvironmentExecutionStatistics {

    private class StatisticKey {
        final MultiKey<IGenericTask> key;

        StatisticKey(IJob job) {
            IGenericTask[] tasks = new IGenericTask[job.size()];
            int i = 0;

            for (IMoleJob moleJob : job.getMoleJobs()) {
                tasks[i++] = moleJob.getTask();
            }

            this.key = new MultiKey<IGenericTask>(tasks);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return key.equals(other);
        }
    }
    
    final Map<IMoleExecution, Map<StatisticKey, IStatistic>> stats = Collections.synchronizedMap(new WeakHashMap<IMoleExecution, Map<StatisticKey, IStatistic>>());
    final Integer historySize;

    public EnvironmentExecutionStatistics(Integer historySize) {
        super();
        this.historySize = historySize;
    }

    @Override
    public IStatistic getStatFor(IJob job) {
        Map<StatisticKey, IStatistic> map = stats.get(JobRegistry.getInstance().getMoleExecutionForJob(job));
        if (map == null) {
            return Statistic.EMPTY_STAT;
        }

        IStatistic ret = map.get(new StatisticKey(job));

        if (ret == null) {
            return Statistic.EMPTY_STAT;
        }
        return ret;
    }

    @Override
    public void statusJustChanged(SampleType type, long length, IJob job) {
        IStatistic statForTask = getOrConstructStatistic(job);
        statForTask.sample(type, length);
    }

    private synchronized IStatistic getOrConstructStatistic(IJob job) {
        Map<StatisticKey, IStatistic> map = getOrConstructStatisticMap(JobRegistry.getInstance().getMoleExecutionForJob(job));

        IStatistic statistic = map.get(new StatisticKey(job));
        if (statistic == null) {
            statistic = new Statistic(historySize);
            map.put(new StatisticKey(job), statistic);
        }

        return statistic;
    }

    private synchronized Map<StatisticKey, IStatistic> getOrConstructStatisticMap(IMoleExecution moleExecution) {
        Map<StatisticKey, IStatistic> map = stats.get(moleExecution);

        if (map == null) {
            map = Collections.synchronizedMap(new HashMap<StatisticKey, IStatistic>());
            stats.put(moleExecution, map);
        }

        return map;
    }
}




