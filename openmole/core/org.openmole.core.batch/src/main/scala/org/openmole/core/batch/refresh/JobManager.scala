/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.batch.refresh

import akka.actor.Actor
import akka.actor.Props
import akka.routing.SmallestMailboxRouter
import akka.util.duration._
import org.openmole.core.batch.environment.BatchExecutionJob
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.batch.file.URIFile
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.IEnvironment
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.BatchEnvironment.Workers
import scala.collection.mutable.SynchronizedMap
import scala.collection.mutable.WeakHashMap

object JobManager extends Logger

import JobManager._

class JobManager(environment: BatchEnvironment) extends Actor {
 
  import environment._
  
  val serializedJobs = new WeakHashMap[BatchExecutionJob, SerializedJob] with SynchronizedMap[BatchExecutionJob, SerializedJob]

  val uploader = context.actorOf(Props(new UploadActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(Workers))), name = "upload")
  val submitter = context.actorOf(Props(new SubmitActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(Workers))), name = "submit")
  val refresher = context.actorOf(Props(new RefreshActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(Workers))), name = "refresher")
  val resultGetters = context.actorOf(Props(new GetResultActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(Workers))), name = "resultGetters")
  val killer = context.actorOf(Props(new KillerActor).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(Workers))), name = "killer")
 
  def receive = {
    case Upload(job) => uploader ! Upload(job)
    case Uploaded(job, sj) => 
      serializedJobs += job -> sj
      submitter ! Submit(job, sj)
    case SubmitDelay(job, sj) => 
      context.system.scheduler.scheduleOnce(minUpdateInterval milliseconds, submitter, Submit(job, sj))
    case Submitted(job, sj, bj) => 
      batchJobs += job -> bj
      self ! RefreshDelay(job, sj, bj, minUpdateInterval, false)
    case RefreshDelay(job, sj, bj, delay, stateChanged) => 
      val newDelay = 
        if(stateChanged) math.min(delay + incrementUpdateInterval, maxUpdateInterval) else minUpdateInterval
      context.system.scheduler.scheduleOnce(newDelay milliseconds, refresher, Refresh(job, sj, bj, newDelay))
    case GetResult(job, sj, out) => 
      resultGetters ! GetResult(job, sj, out)
    case Kill(job) => 
      job.state = ExecutionState.KILLED
      
      batchJobs.remove(job) match {
        case Some(bj) => self ! KillBatchJob(bj)
        case None =>
      }
      
      serializedJobs.remove(job) match {
        case Some(sj) => 
          val path = sj.communicationStorage.path
          URIFile.clean(path.toURIFile(sj.communicationDirPath))
        case None =>
      }
            
    case Error(job, exception) =>
      val level = exception match {
        case e: JobRemoteExecutionException => WARNING
        case _ => FINE
      }
      EventDispatcher.trigger(environment: IEnvironment, new IEnvironment.ExceptionRaised(job, exception,level))
      logger.log(level, "Error in job refresh", exception)
    case KillBatchJob(bj) => killer ! KillBatchJob(bj)
    case MoleJobError(mj, j, e) =>
      EventDispatcher.trigger(environment: IEnvironment, new IEnvironment.MoleJobExceptionRaised(j, e, WARNING, mj))
      logger.log(WARNING, "Error durring job execution, it will be resubmitted.", e)
  }
}
