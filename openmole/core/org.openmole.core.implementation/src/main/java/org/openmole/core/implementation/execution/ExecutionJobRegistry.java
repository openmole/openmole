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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openmole.core.workflow.model.execution.IExecutionJob;

import org.openmole.core.workflow.model.job.IJob;

public class ExecutionJobRegistry<EXECUTIONJOB extends IExecutionJob> {

    Map<IJob, Set<EXECUTIONJOB>> jobs = new HashMap<IJob, Set<EXECUTIONJOB>>();
    Map<IJob, EXECUTIONJOB> lastExecutionJob = new HashMap<IJob, EXECUTIONJOB>();

    public synchronized Collection<IJob> getAllJobs() {
        return jobs.keySet();
    }

    public synchronized Set<EXECUTIONJOB> getExecutionJobsFor(IJob job) {
        Set<EXECUTIONJOB> ret = jobs.get(job);
        if (ret == null) {
            ret = Collections.synchronizedSet(new HashSet<EXECUTIONJOB>());
            jobs.put(job, ret);
        }
        return ret;
    }

    public synchronized void remove(EXECUTIONJOB job) {
        Set<EXECUTIONJOB> executionJobs = getExecutionJobsFor(job.getJob());

        executionJobs.remove(job);

        EXECUTIONJOB lastJob = getLastExecutionJobForJob(job.getJob());

        if (lastJob != null && lastJob.equals(job)) {
            if (!executionJobs.isEmpty()) {
                lastExecutionJob.put(job.getJob(), findLastExecutionJob(job.getJob()));
            } else {
                lastExecutionJob.remove(job.getJob());
            }
        }
    }

    public synchronized boolean isEmpty() {
        return jobs.isEmpty();
    }

    public synchronized void register(EXECUTIONJOB executionJob) {
        Set<EXECUTIONJOB> lst = getExecutionJobsFor(executionJob.getJob());
        lst.add(executionJob);

        EXECUTIONJOB last = getLastExecutionJobForJob(executionJob.getJob());
        if (last == null || last.getCreationTime() < executionJob.getCreationTime()) {
            lastExecutionJob.put(executionJob.getJob(), executionJob);
        }

    }

    public synchronized Integer getNbExecutionJobsForJob(IJob job) {
        return getExecutionJobsFor(job).size();
    }

    public synchronized Collection<EXECUTIONJOB> removeJob(IJob job) {
        Collection<EXECUTIONJOB> ret = getExecutionJobsFor(job);
        lastExecutionJob.remove(job);
        jobs.remove(job);
        return ret;
    }

    public synchronized Collection<EXECUTIONJOB> getAllExecutionJobs() {
        List<EXECUTIONJOB> ret = new LinkedList<EXECUTIONJOB>();

        for (IJob group : getAllJobs()) {
            ret.addAll(getExecutionJobsFor(group));
        }

        return ret;
    }

    public synchronized Collection<EXECUTIONJOB> getAllLastExecutionJobs() {
        List<EXECUTIONJOB> ret = new LinkedList<EXECUTIONJOB>();
        ret.addAll(lastExecutionJob.values());
        return ret;
    }

    public synchronized void purgeAll() {
        lastExecutionJob.clear();
        jobs.clear();
    }

    public synchronized Integer getNbExecutionJobs() {
        Integer total = 0;

        for (IJob group : getAllJobs()) {
            total += getNbExecutionJobsForJob(group);
        }

        return total;
    }

    private synchronized EXECUTIONJOB findLastExecutionJob(IJob job) {
        Set<EXECUTIONJOB> eJobs = getExecutionJobsFor(job);

        EXECUTIONJOB last;
        //synchronized (eJobs) {

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
        //		}

        return last;
    }

    public EXECUTIONJOB getLastExecutionJobForJob(IJob job) {
        return lastExecutionJob.get(job);
    }


}
