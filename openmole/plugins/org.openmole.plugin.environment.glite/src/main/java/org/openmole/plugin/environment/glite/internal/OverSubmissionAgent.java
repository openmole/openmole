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
package org.openmole.plugin.environment.glite.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections15.multimap.MultiHashMap;

import org.openmole.core.implementation.execution.Statistic;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.batch.IBatchExecutionJob;
import org.openmole.misc.updater.IUpdatable;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.execution.IStatistic;
import org.openmole.core.model.execution.batch.SampleType;
import org.openmole.core.model.job.IJob;
import org.openmole.commons.tools.cache.AssociativeCache;
import org.openmole.commons.tools.cache.ICachable;
import org.openmole.core.implementation.execution.JobStatisticCategory;
import org.openmole.core.model.execution.IExecutionJobRegistry;
import org.openmole.core.model.execution.IJobStatisticCategory;
import org.openmole.plugin.environment.glite.GliteEnvironment;
import scala.Tuple2;

public class OverSubmissionAgent implements IUpdatable {

    int minNumberOfJobsByCategory;
    int numberOfSimultaneousExecutionForAJobWhenUnderMinJob;
    GliteEnvironment checkedEnv;
    IWorkloadManagmentStrategy strategy;

    public OverSubmissionAgent(GliteEnvironment checkedEnv, IWorkloadManagmentStrategy strategy, Integer minNumberOfJobsByCategory, Integer numberOfSimultaneousExecutionForAJobWhenUnderMinJob) {
        super();
        this.checkedEnv = checkedEnv;
        this.strategy = strategy;
        this.numberOfSimultaneousExecutionForAJobWhenUnderMinJob = numberOfSimultaneousExecutionForAJobWhenUnderMinJob;
        this.minNumberOfJobsByCategory = minNumberOfJobsByCategory;
    }


    @Override
    public boolean update() {

        final IExecutionJobRegistry<IBatchExecutionJob> registry = checkedEnv.getJobRegistry();

        synchronized (registry) {
            Map<IJobStatisticCategory, AtomicInteger> nbJobsByCategory = new HashMap<IJobStatisticCategory, AtomicInteger>();

            Long curTime = System.currentTimeMillis();
            AssociativeCache<Tuple2<IJobStatisticCategory, SampleType>, Long> timeCache = new AssociativeCache<Tuple2<IJobStatisticCategory, SampleType>, Long>(AssociativeCache.HARD, AssociativeCache.HARD);

            for (final IJob job : registry.getAllJobs()) {
                 if (!job.allMoleJobsFinished()) {

                    final IJobStatisticCategory jobStatisticCategory = new JobStatisticCategory(job);
                    final IBatchExecutionJob lastJob = registry.getLastExecutionJobForJob(job);
                    final ExecutionState executionState = lastJob.getState();
                    final SampleType sampleType = getSampleType(executionState);

                    if (sampleType != null) {

                        final long jobTime = curTime - lastJob.getBatchJob().getTimeStemp(executionState);

                        Tuple2<IJobStatisticCategory, SampleType> key = new Tuple2<IJobStatisticCategory, SampleType>(jobStatisticCategory, sampleType);
                        try {
                            Long limitTime = timeCache.getCache(this, key, new ICachable<Long>() {

                                @Override
                                public Long compute() throws InternalProcessingError, InterruptedException {
                                    List<Long> finishedStat = checkedEnv.getStatistics().getStatFor(job).getSamples(sampleType);
                                    List<Long> runningStat = computeStat(sampleType, registry.getExecutionJobsForTheCategory(jobStatisticCategory));
                                    return strategy.getWhenJobShouldBeResubmited(sampleType, finishedStat, runningStat);
                                }
                            });


                            if (jobTime > limitTime) {
                                checkedEnv.submit(job);
                            }

                        } catch (InternalProcessingError e) {
                            Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.WARNING, "Oversubmission failed.", e);
                        } catch (InterruptedException e) {
                            Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.WARNING, "Oversubmission failed.", e);
                        } catch (UserBadDataError e) {
                            Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.WARNING, "Submission of job failed, oversubmission failed.", e);
                        }
                    }

                    // Count nb execution
                    AtomicInteger executionJobCounter = nbJobsByCategory.get(jobStatisticCategory);
                    if (executionJobCounter == null) {
                        // Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.INFO,job.toString() + " mew counter.");

                        executionJobCounter = new AtomicInteger();
                        nbJobsByCategory.put(jobStatisticCategory, executionJobCounter);
                    }
                    executionJobCounter.addAndGet(registry.getNbExecutionJobsForJob(job));
                }
            }


            for (Map.Entry<IJobStatisticCategory, AtomicInteger> entry : nbJobsByCategory.entrySet()) {
                int nbRessub = minNumberOfJobsByCategory - entry.getValue().get();
                IJobStatisticCategory jobStatisticCategory = entry.getKey();
                //Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.INFO,nbRessub + " " + entry.getValue().get());

                if (nbRessub > 0) {
                    // Resubmit nbRessub jobs in a fair manner
                    MultiHashMap<Integer, IJob> order = new MultiHashMap<Integer, IJob>();
                    SortedSet<Integer> keys = new TreeSet<Integer>();

                    for (IJob job : registry.getJobsForTheCategory(jobStatisticCategory)) {
                        Integer nb = registry.getNbExecutionJobsForJob(job);
                        if (nb < numberOfSimultaneousExecutionForAJobWhenUnderMinJob) {
                            order.put(nb, job);
                            keys.add(nb);
                        }
                    }

                    if (!keys.isEmpty()) {
                        while (nbRessub > 0 & keys.first() < numberOfSimultaneousExecutionForAJobWhenUnderMinJob) {
                            Integer key = keys.first();
                            Collection<IJob> jobs = order.get(keys.first());
                            Iterator<IJob> it = jobs.iterator();
                            IJob job = it.next();

                            //Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "Resubmit : running " + key + " nbRessub " + nbRessub);

                            try {
                                checkedEnv.submit(job);
                            } catch (InternalProcessingError e) {
                                Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.WARNING, "Submission of job failed, oversubmission failed.", e);
                            } catch (UserBadDataError e) {
                                Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.WARNING, "Submission of job failed, oversubmission failed.", e);
                            }

                            jobs.remove(job);
                            if (jobs.isEmpty()) {
                                keys.remove(key);
                            }

                            key++;
                            order.put(key, job);
                            keys.add(key);
                            nbRessub--;
                        }
                    }
                }
            }
        }
        
        return true;
    }
   
    
    private List<Long> computeStat(SampleType type, Collection<IBatchExecutionJob> allExecutionjobs) {
                  
        long curTime = System.currentTimeMillis();
        List<Long> stat = new LinkedList<Long>();
        
        switch(type) {
            case WAITING:
                for (IBatchExecutionJob executionJob : allExecutionjobs) {
                    if(executionJob.getState() == ExecutionState.SUBMITED) {
                        stat.add(curTime - executionJob.getBatchJob().getTimeStemp(ExecutionState.SUBMITED));
                    }
                }
                break;
            case RUNNING:
                for (IBatchExecutionJob executionJob : allExecutionjobs) {
                    if(executionJob.getState() == ExecutionState.RUNNING) {
                        stat.add(curTime - executionJob.getBatchJob().getTimeStemp(ExecutionState.RUNNING));
                    }
                }
                break;
        }
        
        return stat;
    }

    private SampleType getSampleType(ExecutionState executionState) {
        switch (executionState) {
            case SUBMITED:
                return SampleType.WAITING;
            case RUNNING:
                return SampleType.RUNNING;
            default:
                return null;
        }
    }
}
