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
package org.openmole.plugin.environment.glite.internal;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.SampleType;


public class WorkloadOnAverages implements IWorkloadManagmentStrategy {

    Integer minStat;
    Integer historySize;

    IBatchEnvironment<?> environment;
    Map<SampleType, Double> resubmitRatio = new EnumMap<SampleType, Double>(SampleType.class);


    public WorkloadOnAverages(Integer minStat, Double resubmitRatioWaiting, Double resubmitRatioRunning, IBatchEnvironment<?> environment) {
        super();
        this.minStat = minStat;
        
        this.environment = environment;

        this.resubmitRatio.put(SampleType.WAITING, resubmitRatioWaiting);
        this.resubmitRatio.put(SampleType.RUNNING, resubmitRatioRunning);

    }

    
    @Override
    public Long getWhenJobShouldBeResubmited(SampleType type, List<Long> finishedStat, List<Long> runningStat) {

        //IStatistic stat = stats.getStatFor(capsule);
        if (finishedStat.size() < minStat) {
            return Long.MAX_VALUE;
        }

       /* SampleType type;
        switch (job.getState()) {
            case SUBMITED:
                type = SampleType.WAITING;
                break;
            case RUNNING:
                type = SampleType.RUNNING;
                break;
            default:
                return false;
        }*/

     

        long avg = 0;

        for (Long sample : finishedStat) {
            avg += sample / finishedStat.size();
        }

        return (long) (avg * resubmitRatio.get(type));

        /*Long lastStatusChangeDate = job.getBatchJobLastStatusChangedTime();
        long length = System.currentTimeMillis() - lastStatusChangeDate;

        if (length >= avg * maxRatio.get(type)) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "kill " + type.getLabel() + " avg = " + avg + " length = " + length);
            return true;
        }

        return false;*/
    }



  /*  @Override
    public boolean resubmitExecutionJobFor(IJob job, IGenericTaskCapsule<?, ?> capsule) {
        IBatchExecutionJobRegistry registry = environment.getJobRegistry();

        int nbExecutionJobForJob = registry.getNbExecutionJobsForJob(job);

        if (nbExecutionJobForJob >= maxNumberOfSimultaneousExecutionForAJob) {
            return false;
        }
        return resubmitExecutionJobFor(job, capsule, registry, environment.getStatistics());
    }

    @Override
    public boolean shouldBeKilled(IBatchExecutionJob<?> job, IGenericTaskCapsule<?, ?> capsule) {
        return shouldBeKilled(job, capsule, environment.getStatistics());
    }*/
}
