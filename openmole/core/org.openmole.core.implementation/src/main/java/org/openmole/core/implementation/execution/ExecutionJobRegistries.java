/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.openmole.core.model.execution.IExecutionJob;
import org.openmole.core.model.execution.IExecutionJobRegistries;
import org.openmole.core.model.execution.IJobStatisticCategory;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.mole.IExecutionContext;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.commons.tools.structure.Trio;


/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class ExecutionJobRegistries<EXECUTIONJOB extends IExecutionJob> implements IExecutionJobRegistries<EXECUTIONJOB> {


    Map<IExecutionContext, Map<IJobStatisticCategory, ExecutionJobRegistry<EXECUTIONJOB>>> registries = Collections.synchronizedMap(new WeakHashMap<IExecutionContext, Map<IJobStatisticCategory, ExecutionJobRegistry<EXECUTIONJOB>>>());
    Map<EXECUTIONJOB, Duo<IExecutionContext, IJobStatisticCategory>> jobInfo = new WeakHashMap<EXECUTIONJOB, Duo<IExecutionContext, IJobStatisticCategory>>();


    @Override
    public synchronized  Collection<IJob> getAllJobs(IExecutionContext context, IJobStatisticCategory statisticCategory) {
        ExecutionJobRegistry<EXECUTIONJOB> jobRegistry = getExecutionJobRegistryOrNull(context, statisticCategory);
        if(jobRegistry == null) return Collections.EMPTY_LIST;
        return jobRegistry.getAllJobs();
    }

    @Override
    public synchronized Collection<EXECUTIONJOB> getAllExecutionJobs(IExecutionContext context, IJobStatisticCategory statisticCategory, IJob job) {
        ExecutionJobRegistry<EXECUTIONJOB> jobRegistry = getExecutionJobRegistryOrNull(context, statisticCategory);
        if(jobRegistry == null) return Collections.EMPTY_LIST;
        return jobRegistry.getExecutionJobsFor(job);
    }

    @Override
    public synchronized Collection<EXECUTIONJOB> getAllExecutionJobs(IExecutionContext context, IJobStatisticCategory statisticCategory) {
        ExecutionJobRegistry<EXECUTIONJOB> jobRegistry = getExecutionJobRegistryOrNull(context, statisticCategory);
        if(jobRegistry == null) return Collections.EMPTY_LIST;
        return jobRegistry.getAllExecutionJobs();
    }

    @Override
    public synchronized void remove(IExecutionContext context, IJobStatisticCategory statisticCategory, IJob job) {
        ExecutionJobRegistry<EXECUTIONJOB> ret = getExecutionJobRegistryOrNull(context, statisticCategory);
        if(ret == null) return;
        Collection<EXECUTIONJOB> ejobs = ret.removeJob(job);
       /* for(EXECUTIONJOB ejob : ejobs) {
            jobInfo.remove(ejob);
        }*/
        if(ret.isEmpty()) remove(context, statisticCategory);
    }

    @Override
    public synchronized void remove(IExecutionContext context, IJobStatisticCategory statisticCategory, EXECUTIONJOB executionJob) {
        ExecutionJobRegistry<EXECUTIONJOB> ret = getExecutionJobRegistryOrNull(context, statisticCategory);
        if(ret == null) return;
        ret.remove(executionJob);
        //jobInfo.remove(executionJob);
    }

    private synchronized void remove(IExecutionContext context, IJobStatisticCategory statisticCategory) {
        Map<IJobStatisticCategory, ExecutionJobRegistry<EXECUTIONJOB>> map = registries.get(context);
        if(map == null) return;
        map.remove(statisticCategory);
    }

    private ExecutionJobRegistry<EXECUTIONJOB> getExecutionJobRegistryOrNull(IExecutionContext context, IJobStatisticCategory statisticCategory) {
        Map<IJobStatisticCategory, ExecutionJobRegistry<EXECUTIONJOB>> map = registries.get(context);
        if(map == null) return null;
        ExecutionJobRegistry<EXECUTIONJOB> ret = map.get(statisticCategory);
        return ret;
    }


    @Override
    public synchronized  void register(IExecutionContext context, IJobStatisticCategory statisticCategory, EXECUTIONJOB executionJob) {
        Map<IJobStatisticCategory, ExecutionJobRegistry<EXECUTIONJOB>> map = registries.get(context);

        if(map == null) {
            map = new HashMap<IJobStatisticCategory, ExecutionJobRegistry<EXECUTIONJOB>>();
            registries.put(context, map);
        }

        ExecutionJobRegistry<EXECUTIONJOB> ret = map.get(statisticCategory);

        if(ret == null) {
            ret = new ExecutionJobRegistry<EXECUTIONJOB>();
            map.put(statisticCategory, ret);
        }


        ret.register(executionJob);
        jobInfo.put(executionJob, new Duo<IExecutionContext, IJobStatisticCategory>(context, statisticCategory));
    }

    @Override
    public synchronized Iterable<Trio<IExecutionContext,IJobStatisticCategory,IJob>> getAllJobs() {
        List<Trio<IExecutionContext,IJobStatisticCategory,IJob>> ret = new LinkedList<Trio<IExecutionContext,IJobStatisticCategory,IJob>>();
        
        for(Map.Entry<IExecutionContext, Map<IJobStatisticCategory, ExecutionJobRegistry<EXECUTIONJOB>>> val : registries.entrySet()) {
            for(Map.Entry<IJobStatisticCategory, ExecutionJobRegistry<EXECUTIONJOB>> regEntry : val.getValue().entrySet()) {
                for(IJob job : regEntry.getValue().getAllJobs()) {
                    ret.add(new Trio<IExecutionContext, IJobStatisticCategory, IJob>(val.getKey(), regEntry.getKey(), job));
                }
            }
        }

        return ret;
    }

    @Override
    public IExecutionContext getExecutionContext(EXECUTIONJOB job) {
        return jobInfo.get(job).getLeft();
    }

    @Override
    public IJobStatisticCategory getJobStatisticCategory(EXECUTIONJOB job) {
        return jobInfo.get(job).getRight();
    }

    @Override
    public Integer getNbExecutionJobs(IExecutionContext context, IJobStatisticCategory jobStatisticCategory, IJob job) {
        ExecutionJobRegistry<EXECUTIONJOB> ret = getExecutionJobRegistryOrNull(context, jobStatisticCategory);
        if(ret == null) return 0;
        return ret.getNbExecutionJobsForJob(job);
    }

    @Override
    public synchronized EXECUTIONJOB findLastExecutionJob(IExecutionContext context, IJobStatisticCategory jobStatisticCategory, IJob job) {
        ExecutionJobRegistry<EXECUTIONJOB> ret = getExecutionJobRegistryOrNull(context, jobStatisticCategory);
        if(ret == null) return null;

        return ret.getLastExecutionJobForJob(job);
    }

    @Override
    public synchronized  Iterable<EXECUTIONJOB> allExecutionjobs() {
        Collection<EXECUTIONJOB> ret = new LinkedList<EXECUTIONJOB>();

        Iterable<Trio<IExecutionContext,IJobStatisticCategory,IJob>> it = getAllJobs();

        for(Trio<IExecutionContext,IJobStatisticCategory,IJob> trio : it) {
            for(EXECUTIONJOB executionJob: getAllExecutionJobs(trio.getLeft(), trio.getCenter(), trio.getRight())) {
                ret.add(executionJob);
            }
        }

        return ret;
    }

    @Override
    public Collection<EXECUTIONJOB> getAllLastExecutionJobs(IExecutionContext context, IJobStatisticCategory statisticCategory) {
         ExecutionJobRegistry<EXECUTIONJOB> ret = getExecutionJobRegistryOrNull(context, statisticCategory);
         if(ret == null) return Collections.EMPTY_LIST;
         return ret.getAllLastExecutionJobs();
    }



}
