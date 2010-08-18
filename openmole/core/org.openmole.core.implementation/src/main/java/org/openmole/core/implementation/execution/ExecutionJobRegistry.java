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
package org.openmole.core.implementation.execution;

import org.openmole.core.model.execution.IExecutionJobRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    @Override
    public synchronized Collection<IJob> getAllJobs() {
        return jobs.keySet();
    }
      
    @Override
     public synchronized SortedSet<EXECUTIONJOB> getExecutionJobsFor(IJob job) {
         return jobs.get(job);      
     }
   

    @Override
    public synchronized void remove(EXECUTIONJOB job) {
        Set<EXECUTIONJOB> executionJobs = getExecutionJobsFor(job.getJob());
        if(executionJobs != null) executionJobs.remove(job);
    }

    @Override
    public synchronized boolean isEmpty() {
        return jobs.isEmpty();
    }

    @Override
    public synchronized void register(EXECUTIONJOB executionJob) {
        SortedSet<EXECUTIONJOB> lst = getExecutionJobsFor(executionJob.getJob());
        
         if (lst == null) {
            lst = Collections.synchronizedSortedSet(new TreeSet<EXECUTIONJOB>(ExecutionJobComparator));
            jobs.put(executionJob.getJob(), lst);
            
            IJobStatisticCategory category = new JobStatisticCategory(executionJob.getJob());
            Set<IJob> jobsOfTheCategory = jobCategories.get(category);
            
            if(jobsOfTheCategory == null) {
                jobsOfTheCategory = new HashSet<IJob>();
                jobCategories.put(category, jobsOfTheCategory);
            }

            jobsOfTheCategory.add(executionJob.getJob());
        }
        
        lst.add(executionJob);
    }

    
    @Override
    public synchronized Integer getNbExecutionJobsForJob(IJob job) {
         Set<EXECUTIONJOB> executionJobs = getExecutionJobsFor(job);
         if(executionJobs == null) return 0;
         else return executionJobs.size();
    }

    @Override
    public synchronized void removeJob(IJob job) {
        jobs.remove(job);

        IJobStatisticCategory category = new JobStatisticCategory(job);
        Set<IJob> jobs = jobCategories.get(category);
        jobs.remove(job);

        if(jobs.isEmpty()) {
            jobCategories.remove(category);
        }
        
    }

    @Override
    public synchronized Collection<EXECUTIONJOB> getAllExecutionJobs() {
        List<EXECUTIONJOB> ret = new LinkedList<EXECUTIONJOB>();

        for (IJob job : getAllJobs()) {
            Set<EXECUTIONJOB> executionJobs = getExecutionJobsFor(job);
            if(executionJobs != null) ret.addAll(executionJobs);
        }

        return ret;
    }

    @Override
    public synchronized EXECUTIONJOB getLastExecutionJobForJob(IJob job) {
        SortedSet<EXECUTIONJOB> set = getExecutionJobsFor(job);
        if(set == null || set.isEmpty()) return null;
        return set.first();
    }

    @Override
    public synchronized Collection<EXECUTIONJOB> getExecutionJobsForTheCategory(IJobStatisticCategory category) {
        List<EXECUTIONJOB> ret = new LinkedList<EXECUTIONJOB>();

        for(IJob job: getJobsForTheCategory(category)) {
            Set<EXECUTIONJOB> executionJobs = getExecutionJobsFor(job);
            if(executionJobs != null) ret.addAll(executionJobs);
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
