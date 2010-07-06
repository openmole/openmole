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

import org.openmole.core.model.execution.IExecutionJobRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.openmole.core.model.execution.IExecutionJob;
import org.openmole.core.model.execution.IJobStatisticCategory;

import org.openmole.core.model.job.IJob;

public class ExecutionJobRegistry<EXECUTIONJOB extends IExecutionJob> implements IExecutionJobRegistry<EXECUTIONJOB> {

    final private static Comparator<IExecutionJob> ExecutionJobComparator = new Comparator<IExecutionJob>() {

        @Override
        public int compare(IExecutionJob o1, IExecutionJob o2) {
            return new Long(o2.getCreationTime()).compareTo(o1.getCreationTime());
        }
        
    };
    
    
    Map<IJob, SortedSet<EXECUTIONJOB>> jobs = new HashMap<IJob, SortedSet<EXECUTIONJOB>>();
    Map<IJobStatisticCategory, Set<IJob>> jobCategories = new HashMap<IJobStatisticCategory, Set<IJob>>();


    public synchronized Collection<IJob> getAllJobs() {
        return jobs.keySet();
    }

    public synchronized SortedSet<EXECUTIONJOB> getExecutionJobsFor(IJob job) {
        SortedSet<EXECUTIONJOB> ret = jobs.get(job);
        if (ret == null) {
            ret = new TreeSet<EXECUTIONJOB>(ExecutionJobComparator);
            jobs.put(job, ret);
            IJobStatisticCategory category = new JobStatisticCategory(job);

            Set<IJob> jobsOfTheCategory = jobCategories.get(category);
            
            if(jobsOfTheCategory == null) {
                jobsOfTheCategory = new HashSet<IJob>();
                jobCategories.put(category, jobsOfTheCategory);
            }

            jobsOfTheCategory.add(job);
        }
        return ret;
    }

    public synchronized void remove(EXECUTIONJOB job) {
        Set<EXECUTIONJOB> executionJobs = getExecutionJobsFor(job.getJob());
        executionJobs.remove(job);
    }

    public synchronized boolean isEmpty() {
        return jobs.isEmpty();
    }

    public synchronized void register(EXECUTIONJOB executionJob) {
        Set<EXECUTIONJOB> lst = getExecutionJobsFor(executionJob.getJob());
        lst.add(executionJob);
    }

    public synchronized Integer getNbExecutionJobsForJob(IJob job) {
        return getExecutionJobsFor(job).size();
    }

    public synchronized Collection<EXECUTIONJOB> removeJob(IJob job) {
        Collection<EXECUTIONJOB> ret = getExecutionJobsFor(job);

        jobs.remove(job);

        IJobStatisticCategory category = new JobStatisticCategory(job);

        Set<IJob> jobs = jobCategories.get(category);
        jobs.remove(job);

        if(jobs.isEmpty()) {
            jobCategories.remove(category);
        }

        return ret;
    }

    public synchronized Collection<EXECUTIONJOB> getAllExecutionJobs() {
        List<EXECUTIONJOB> ret = new LinkedList<EXECUTIONJOB>();

        for (IJob group : getAllJobs()) {
            ret.addAll(getExecutionJobsFor(group));
        }

        return ret;
    }


    private synchronized EXECUTIONJOB findLastExecutionJob(IJob job) {
        Set<EXECUTIONJOB> eJobs = getExecutionJobsFor(job);

        EXECUTIONJOB last;

        if (eJobs == null || eJobs.isEmpty()) {
            return null;
        }

        Iterator<EXECUTIONJOB> eJob = eJobs.iterator();

        last = eJob.next();

        while (eJob.hasNext()) {
            EXECUTIONJOB cur = eJob.next();
            if (cur.getCreationTime() > last.getCreationTime()) {
                last = cur;
            }
        }

        return last;
    }

    public synchronized EXECUTIONJOB getLastExecutionJobForJob(IJob job) {
        SortedSet<EXECUTIONJOB> set = getExecutionJobsFor(job);
        if(set.isEmpty()) return null;
        return set.first();
    }

    @Override
    public synchronized Collection<EXECUTIONJOB> getExecutionJobsForTheCategory(IJobStatisticCategory category) {
        List<EXECUTIONJOB> ret = new LinkedList<EXECUTIONJOB>();

        for(IJob job: getJobsForTheCategory(category)) {
            ret.addAll(getExecutionJobsFor(job));
        }
        return ret;
    }

    @Override
    public Collection<IJob> getJobsForTheCategory(IJobStatisticCategory category) {
        Set<IJob> jobs = jobCategories.get(category);
        if(jobs == null) return Collections.EMPTY_LIST;
        return jobs;
    }


}
