/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.execution

import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.execution.IExecutionJobId
import org.openmole.core.model.execution.IExecutionJobRegistry
import org.openmole.core.model.execution.IJobStatisticCategory
import org.openmole.core.model.job.IJob

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.immutable.TreeSet


class ExecutionJobRegistry [EXECUTIONJOB <: IExecutionJob[_]] extends IExecutionJobRegistry[EXECUTIONJOB] {

  private def ExecutionJobOrderOnTime = new Ordering[EXECUTIONJOB] {
    
    override def compare(o1: EXECUTIONJOB, o2: EXECUTIONJOB): Int = {
      val comp = o2.creationTime.compare(o1.creationTime)
      if(comp != 0) comp
      else IExecutionJobId.order.compare(o2.id, o1.id)
    }     
  }

  var jobs = new HashMap[IJob, TreeSet[EXECUTIONJOB]]
  var categories = new HashMap[IJobStatisticCategory, HashSet[IJob]]

  override def getAllJobs: Iterable[IJob] = {
    return jobs.keySet
  }
      
  override def getExecutionJobsFor(job: IJob): Iterable[EXECUTIONJOB] = {
    jobs.get(job) match {
      case Some(ejobs) => ejobs
      case None => Iterable.empty
    }
  }
   

  override def remove(ejob: EXECUTIONJOB) = {
    jobs.get(ejob.job) match {
      case Some(ejobs) => jobs(ejob.job) = ejobs - ejob
      case None =>
    }     
  }

  override def isEmpty: Boolean = {
    jobs.isEmpty
  }

  override def register(ejob: EXECUTIONJOB) = {
    synchronized {  
     jobs(ejob.job) = jobs.get(ejob.job) match {
        case Some(ejobs) => ejobs + ejob
        case None => TreeSet[EXECUTIONJOB](ejob)(ExecutionJobOrderOnTime)
      }  
      val category = new JobStatisticCategory(ejob.job)
      categories(category) = categories.get(category) match {
        case Some(jobs) => jobs + ejob.job
        case None => HashSet(ejob.job)
      }
    }
  }

    
  override def getNbExecutionJobsForJob(job: IJob): Int = {
    getExecutionJobsFor(job).size
  }

  override def removeJob(job: IJob) = {
    synchronized {
      jobs -= job

      val category = new JobStatisticCategory(job)
      categories.get(category) match {
        case Some(jobs) => {
            val newJobs = jobs - job
            if(newJobs.isEmpty) categories -= category
            else categories(category) = newJobs
          }
        case None =>
      }
    }
  }

  override def getAllExecutionJobs:  Iterable[EXECUTIONJOB] = {
    for (job <- getAllJobs ; if jobs.contains(job) ; ejob <- jobs(job)) yield ejob
  }

  override def getLastExecutionJobForJob(job: IJob): Option[EXECUTIONJOB] = {
    jobs.get(job) match {
      case None => None
      case Some(ejobs) => ejobs.headOption
    }
  }

  override def getExecutionJobsForTheCategory(category: IJobStatisticCategory): Iterable[EXECUTIONJOB] = {
    categories.get(category) match {
      case Some(js) => for(j <- js; if jobs.contains(j); ejob <- jobs(j)) yield ejob
      case None => Iterable.empty
    }  
  }

  override def getJobsForTheCategory(category: IJobStatisticCategory): Iterable[IJob] = {
    categories.get(category) match {
      case None => Iterable.empty
      case Some(jobs) => jobs
    }
  }

}
