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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.glite.internal

import java.util.logging.Level
import java.util.logging.Logger

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.core.model.execution.batch.IBatchExecutionJob
import org.openmole.misc.updater.IUpdatable
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.SampleType
import org.openmole.core.model.job.IJob
import org.openmole.commons.tools.cache.AssociativeCache
import org.openmole.core.implementation.execution.JobStatisticCategory
import org.openmole.core.model.execution.IExecutionJobRegistry
import org.openmole.core.model.execution.IJobStatisticCategory
import org.openmole.plugin.environment.glite.GliteEnvironment
import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import scala.collection.JavaConversions._
import scala.collection.mutable.MultiMap

class OverSubmissionAgent(environment: GliteEnvironment, strategy: IWorkloadManagmentStrategy, minNumberOfJobsByCategory: Int, numberOfSimultaneousExecutionForAJobWhenUnderMinJob: Int) extends IUpdatable {

  override def update: Boolean = {

    val registry = environment.jobRegistry

    registry.synchronized {
      var nbJobsByCategory = new HashMap[IJobStatisticCategory, Int]

      val curTime = System.currentTimeMillis
      val timeCache = new AssociativeCache[(IJobStatisticCategory, SampleType), Long](AssociativeCache.HARD, AssociativeCache.HARD)

      for (job <- registry.allJobs) {
        if (!job.allMoleJobsFinished) {

          val jobStatisticCategory = new JobStatisticCategory(job)
          registry.lastExecutionJob(job) match {
            case Some(lastJob) => 
                        
              val executionState = lastJob.state
              getSampleType(executionState) match {
                case Some(sampleType) => 
                  val jobTime = curTime - lastJob.batchJob.timeStemp(executionState)

                  val key = (jobStatisticCategory, sampleType)
                  try {
                    val limitTime = timeCache.cache(this, key, {
                        val finishedStat = environment.statistic.statistic(job)(sampleType)
                          val runningStat = computeStat(sampleType, registry.executionJobs(jobStatisticCategory))
                          strategy.whenJobShouldBeResubmited(sampleType, finishedStat, runningStat)
                        })


                    if (jobTime > limitTime) {
                      environment.submit(job)
                    }

                  } catch {
                    case (e: Throwable) => Logger.getLogger(classOf[OverSubmissionAgent].getName()).log(Level.WARNING, "Oversubmission failed.", e);
                  } 
                case None =>
              }
          
              nbJobsByCategory(jobStatisticCategory) = nbJobsByCategory.get(jobStatisticCategory) match {
                case Some(nb) => nb + 1
                case None => 1
              }
            case None =>
          }
        }
                    
      }

      for (val entry <- nbJobsByCategory.entrySet) {
        var nbRessub = minNumberOfJobsByCategory - entry.getValue
        val jobStatisticCategory = entry.getKey
        //Logger.getLogger(classOf[OverSubmissionAgent].getName).log(Level.INFO,nbRessub + " " + entry.getValue);

        if (nbRessub > 0) {
          // Resubmit nbRessub jobs in a fair manner
          val order = new HashMap[Int, Set[IJob]] with MultiMap[Int, IJob]
          var keys = new TreeSet[Int]

          for (job <- registry.jobs(jobStatisticCategory)) {
            val nb = registry.nbExecutionJobs(job)
            if (nb < numberOfSimultaneousExecutionForAJobWhenUnderMinJob) {
             val set = order.get(nb) match {
                case None => 
                  val set = new HashSet[IJob]
                  order += ((nb, set))
                  set
                case Some(set) => set
              } 
              set += job
              keys += nb
            }
          }

          if (!keys.isEmpty) {
            while (nbRessub > 0 && keys.head < numberOfSimultaneousExecutionForAJobWhenUnderMinJob) {
              var key = keys.head
              val jobs = order(keys.head)
              val it = jobs.iterator
              val job = it.next

              //Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "Resubmit : running " + key + " nbRessub " + nbRessub);

              try {
                environment.submit(job)
              } catch {
                case e => Logger.getLogger(classOf[OverSubmissionAgent].getName).log(Level.WARNING, "Submission of job failed, oversubmission failed.", e);
              }

              jobs -= job
              if (jobs.isEmpty) {
                jobs.remove(key)
                keys -= key
              }

              key += 1
              if(!order.contains(key)) order.put(key, new HashSet[IJob])
             
              order(key) += job
              keys += key
              nbRessub -= 1
            }
          }
        }
      }
    }
        
    true
  }
   
    
  private def computeStat(sample: SampleType, allExecutionjobs: Iterable[IBatchExecutionJob] ): List[Long] = {
                  
    val curTime = System.currentTimeMillis
    val stat = new ListBuffer[Long]
        
    sample match {
      case SampleType.WAITING =>
        for (executionJob <- allExecutionjobs) {
          if(executionJob.state == ExecutionState.SUBMITED) {
            stat += curTime - executionJob.batchJob.timeStemp(ExecutionState.SUBMITED)
          }
        }
      case SampleType.RUNNING =>
        for (executionJob <- allExecutionjobs) {
          if(executionJob.state == ExecutionState.RUNNING) {
            stat += curTime - executionJob.batchJob.timeStemp(ExecutionState.RUNNING)
          }
        }
    }
        
    stat.toList
  }

  private def getSampleType(executionState: ExecutionState): Option[SampleType] = {
    executionState match {
      case ExecutionState.SUBMITED => Some(SampleType.WAITING)
      case ExecutionState.RUNNING => Some(SampleType.RUNNING)
      case _ => None
    }
  }

}
