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
package org.openmole.plugin.environmentprovider.glite.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections15.multimap.MultiHashMap;

import org.openmole.core.workflow.implementation.execution.Statistic;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.execution.batch.IBatchExecutionJob;
import org.openmole.core.workflow.model.execution.IExecutionJobRegistries;
import org.openmole.misc.updater.IUpdatable;
import org.openmole.core.workflow.model.execution.ExecutionState;
import org.openmole.core.workflow.model.execution.IJobStatisticCategory;
import org.openmole.core.workflow.model.execution.IStatistic;
import org.openmole.core.workflow.model.execution.batch.SampleType;
import org.openmole.core.workflow.model.job.IJob;
import org.openmole.core.workflow.model.mole.IExecutionContext;
import org.openmole.commons.tools.cache.AssociativeCache;
import org.openmole.commons.tools.cache.ICachable;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.structure.Trio;
import org.openmole.plugin.environmentprovider.glite.GliteEnvironment;

public class OverSubmissionAgent implements IUpdatable {

    long checkInterval;
    int minNumberOfJobsByCategory;
    int numberOfSimultaneousExecutionForAJobWhenUnderMinJob;
    GliteEnvironment checkedEnv;
    IWorkloadManagmentStrategy strategy;

    public OverSubmissionAgent(GliteEnvironment checkedEnv, IWorkloadManagmentStrategy strategy, Integer minNumberOfJobsByCategory, Integer numberOfSimultaneousExecutionForAJobWhenUnderMinJob, long checkInterval) {
        super();
        this.checkedEnv = checkedEnv;
        this.strategy = strategy;
        this.checkInterval = checkInterval;
        this.numberOfSimultaneousExecutionForAJobWhenUnderMinJob = numberOfSimultaneousExecutionForAJobWhenUnderMinJob;
        this.minNumberOfJobsByCategory = minNumberOfJobsByCategory;
        //  this.numberOfSimultaneousExecutionForAJobWhenUnderMinJob = numberOfSimultaneousExecutionForAJobWhenUnderMinJob;
    }

    @Override
    public long getUpdateInterval() {
        return checkInterval;
    }

    @Override
    public void update() {

        final IExecutionJobRegistries<IBatchExecutionJob> registries = checkedEnv.getJobRegistries();
        //Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "Total " + registry.getNbExecutionJobs());

        synchronized (registries) {


            Map<Duo<IExecutionContext, IJobStatisticCategory>, AtomicInteger> nbJobsByCategory = new HashMap<Duo<IExecutionContext, IJobStatisticCategory>, AtomicInteger>();

            Long curTime = System.currentTimeMillis();
            AssociativeCache<Trio<IExecutionContext, IJobStatisticCategory, SampleType>, Long> timeCache = new AssociativeCache<Trio<IExecutionContext, IJobStatisticCategory, SampleType>, Long>(AssociativeCache.HARD, AssociativeCache.HARD);

            for (Trio<IExecutionContext, IJobStatisticCategory, IJob> trio : registries.getAllJobs()) {
                final IJobStatisticCategory jobStatisticCategory = trio.getCenter();
                final IExecutionContext executionContext = trio.getLeft();
                final IJob job = trio.getRight();

                //Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO,j.toString() + " " + registry.getNbExecutionJobsForJob(j));

                if (!job.allMoleJobsFinished()) {

                    IBatchExecutionJob lastJob = registries.findLastExecutionJob(executionContext, jobStatisticCategory, job);

                    if (lastJob == null) {
                        Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.WARNING, "Bug: last execution job should never be null.");
                        continue;
                    }

                    ExecutionState executionState = lastJob.getState();
                    final SampleType sampleType = getSampleType(executionState);

                    if (sampleType != null) {

                        Long jobTime = curTime - lastJob.getBatchJob().getTimeStemp(executionState);


                        Trio<IExecutionContext, IJobStatisticCategory, SampleType> key = new Trio<IExecutionContext, IJobStatisticCategory, SampleType>(executionContext, jobStatisticCategory, sampleType);
                        try {

                            Long limitTime = timeCache.getCache(this, key, new ICachable<Long>() {

                                @Override
                                public Long compute() throws InternalProcessingError, InterruptedException {
                                    IStatistic finishedStat = checkedEnv.getStatistics().getStatFor(executionContext, jobStatisticCategory);
                                    IStatistic runningStat = computeStat(registries.getAllExecutionJobs(executionContext, jobStatisticCategory));
                                    Long t = strategy.getWhenJobShouldBeResubmited(sampleType, finishedStat, runningStat);
                                    //               Logger.getLogger(OverSubmissionAgent.class.getName()).log(Level.INFO, sampleType + " t = "+ t + " ; nbfinishedStat = " + finishedStat.getNbSamples(sampleType) + " ; runningStat = " + runningStat.getNbSamples(sampleType));
                                    return t;
                                }
                            });


                            if (jobTime > limitTime) {
                                checkedEnv.submit(job, executionContext, jobStatisticCategory);
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
                    Duo<IExecutionContext, IJobStatisticCategory> keyCount = new Duo<IExecutionContext, IJobStatisticCategory>(executionContext, jobStatisticCategory);
                    AtomicInteger executionJobCounter = nbJobsByCategory.get(keyCount);
                    if (executionJobCounter == null) {
                        executionJobCounter = new AtomicInteger();
                        nbJobsByCategory.put(keyCount, executionJobCounter);
                    }
                    executionJobCounter.addAndGet(registries.getNbExecutionJobs(executionContext, jobStatisticCategory, job));


                }
            }


            for (Map.Entry<Duo<IExecutionContext, IJobStatisticCategory>, AtomicInteger> entry : nbJobsByCategory.entrySet()) {
                int nbRessub = minNumberOfJobsByCategory - entry.getValue().get();
                IExecutionContext executionContext = entry.getKey().getLeft();
                IJobStatisticCategory jobStatisticCategory = entry.getKey().getRight();

                if (nbRessub > 0) {
                    // Resubmit nbRessub jobs in a fair manner
                    MultiHashMap<Integer, IJob> order = new MultiHashMap<Integer, IJob>();
                    SortedSet<Integer> keys = new TreeSet<Integer>();

                    for (IJob job : registries.getAllJobs(executionContext, jobStatisticCategory)) {
                        Integer nb = registries.getNbExecutionJobs(executionContext, jobStatisticCategory, job);
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
                                // IExecutionContext executionContext = registry.getExecutionContext(job);
                                checkedEnv.submit(job, executionContext, jobStatisticCategory);
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





                //		}
            }


        }
        //	Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "End update oversubmission");

    }

    private IStatistic computeStat(Collection<IBatchExecutionJob> allExecutionjobs) {
        IStatistic statistic = new Statistic(allExecutionjobs.size());
        Long curTime = System.currentTimeMillis();
        Long length;

        for (IBatchExecutionJob executionJob : allExecutionjobs) {
            switch (executionJob.getState()) {
                case SUBMITED:
                    length = curTime - executionJob.getBatchJob().getTimeStemp(ExecutionState.SUBMITED);
                    statistic.sample(SampleType.WAITING, length);
                    break;
                case RUNNING:
                    length = curTime - executionJob.getBatchJob().getTimeStemp(ExecutionState.RUNNING);
                    statistic.sample(SampleType.RUNNING, length);
                    break;
            }
        }

        return statistic;
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
