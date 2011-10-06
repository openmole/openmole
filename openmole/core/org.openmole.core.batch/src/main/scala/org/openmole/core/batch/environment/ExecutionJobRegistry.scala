/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.environment

import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.execution.IExecutionJobId
import org.openmole.core.model.job.IJob
import org.openmole.core.implementation.execution.StatisticKey

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import java.util.Comparator
import java.util.TreeSet
import scala.collection.JavaConversions._

class ExecutionJobRegistry [EXECUTIONJOB <: IExecutionJob]{

  private def ExecutionJobOrderOnTime = new Comparator[EXECUTIONJOB] {
    override def compare(o1: EXECUTIONJOB, o2: EXECUTIONJOB): Int = {
      val comp = o2.creationTime.compare(o1.creationTime)
      if(comp != 0) comp
      else IExecutionJobId.order.compare(o2.id, o1.id)
    }
  }
  
  //FIXME Move to tree map
  var jobs = new HashMap[IJob, TreeSet[EXECUTIONJOB]]
  var categories = new HashMap[StatisticKey, HashSet[IJob]]

  def allJobs: Iterable[IJob] = jobs.keySet

  def executionJobs(job: IJob): Iterable[EXECUTIONJOB] = jobs.get(job) match {
    case Some(ejobs) => ejobs
    case None => Iterable.empty
  }

  def remove(ejob: EXECUTIONJOB) = 
    jobs.get(ejob.job) match {
      case Some(ejobs) => ejobs -= ejob
      case None =>
    }     

  def isEmpty: Boolean = jobs.isEmpty

  def register(ejob: EXECUTIONJOB) = synchronized {
    jobs.getOrElseUpdate(ejob.job, new TreeSet[EXECUTIONJOB](ExecutionJobOrderOnTime)) += ejob
    val category = new StatisticKey(ejob.job)
    categories.getOrElseUpdate(category, new HashSet[IJob]) += ejob.job
  }
    
  def nbExecutionJobs(job: IJob): Int =  executionJobs(job).size

  def removeJob(job: IJob) = synchronized {
    jobs -= job

    val category = new StatisticKey(job)
    categories.get(category) match {
      case Some(jobs) => {
          val newJobs = jobs - job
          if(newJobs.isEmpty) categories -= category
          else categories(category) = newJobs
        }
      case None =>
    }
  }

  def allExecutionJobs:  Iterable[EXECUTIONJOB] = for (job <- allJobs ; ejob <- executionJobs(job)) yield ejob

  def lastExecutionJob(job: IJob): Option[EXECUTIONJOB] =
    jobs.get(job) match {
      case None => None
      case Some(ejobs) => ejobs.headOption
    }

  def executionJobs(category: StatisticKey): Iterable[EXECUTIONJOB] =
    categories.get(category) match {
      case Some(js) => for(j <- js; if jobs.contains(j); ejob <- jobs(j)) yield ejob
      case None => Iterable.empty
    }  
 

  def jobs(category: StatisticKey): Iterable[IJob] = 
    categories.get(category) match {
      case None => Iterable.empty
      case Some(jobs) => jobs
    }

}
