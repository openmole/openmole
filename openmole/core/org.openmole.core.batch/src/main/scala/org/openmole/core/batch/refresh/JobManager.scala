/*
 * Copyright (C) 2012 Romain Reuillon
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

import akka.actor.{ ActorRef, Actor, ActorSystem, Props }
import akka.routing.SmallestMailboxRouter
import org.openmole.core.eventdispatcher.EventDispatcher
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.tools.service.Logger
import org.openmole.core.workflow.execution._
import com.typesafe.config.ConfigFactory
import org.openmole.core.batch.environment._
import org.openmole.core.batch.environment.BatchEnvironment.JobManagementThreads
import org.openmole.core.workspace.Workspace

import scala.concurrent.duration._
import scala.concurrent.duration.{ Duration ⇒ SDuration, MILLISECONDS }

object JobManager extends Logger

import JobManager.Log._

class JobManager extends Actor {

  val workers = ActorSystem.create("JobManagement", ConfigFactory.parseString(
    """
akka {
  daemonic="on"
  actor {
    default-dispatcher {
      executor = "fork-join-executor"
      type = Dispatcher
      mailbox-type = """ + '"' + classOf[PriorityMailBox].getName + '"' + """
      
      fork-join-executor {
        parallelism-min = """ + Workspace.preference(JobManagementThreads) + """
        parallelism-max = """ + Workspace.preference(JobManagementThreads) + """
      }
      throughput = 1
    }
  }
}
""").withFallback(ConfigFactory.load(classOf[ConfigFactory].getClassLoader)))

  import BatchEnvironment.system.dispatcher

  //val resizer = DefaultResizer(lowerBound = 10, upperBound = Workspace.preferenceAsInt(JobManagementThreads))
  val uploader = workers.actorOf(Props(new UploadActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(JobManagementThreads))))
  val submitter = workers.actorOf(Props(new SubmitActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(JobManagementThreads))))
  val refresher = workers.actorOf(Props(new RefreshActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(JobManagementThreads))))
  val resultGetters = workers.actorOf(Props(new GetResultActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(JobManagementThreads))))
  val killer = workers.actorOf(Props(new KillerActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(JobManagementThreads))))
  val cleaner = workers.actorOf(Props(new CleanerActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(JobManagementThreads))))
  val deleter = workers.actorOf(Props(new DeleteActor(self)).withRouter(SmallestMailboxRouter(Workspace.preferenceAsInt(JobManagementThreads))))

  def receive = {
    case msg: Upload             ⇒ uploader ! msg
    case msg: Submit             ⇒ submitter ! msg
    case msg: Refresh            ⇒ refresher ! msg
    case msg: GetResult          ⇒ resultGetters ! msg
    case msg: KillBatchJob       ⇒ killer ! msg
    case msg: DeleteFile         ⇒ deleter ! msg
    case msg: CleanSerializedJob ⇒ cleaner ! msg

    case Manage(job) ⇒
      self ! Upload(job)

    case Delay(msg, delay) ⇒
      context.system.scheduler.scheduleOnce(delay) {
        self ! msg
      }

    case Uploaded(job, sj) ⇒
      job.serializedJob = Some(sj)
      self ! Submit(job, sj)

    case Submitted(job, sj, bj) ⇒
      job.batchJob = Some(bj)
      self ! Delay(Refresh(job, sj, bj, job.environment.minUpdateInterval), job.environment.minUpdateInterval)

    case Kill(job) ⇒
      job.state = ExecutionState.KILLED
      killAndClean(job)

    case Resubmit(job, storage) ⇒
      killAndClean(job)
      job.state = ExecutionState.READY
      uploader ! Upload(job)

    case Error(job, exception) ⇒
      val level = exception match {
        case e: UserBadDataError            ⇒ SEVERE
        case e: JobRemoteExecutionException ⇒ WARNING
        case _                              ⇒ FINE
      }
      EventDispatcher.trigger(job.environment: Environment, new Environment.ExceptionRaised(job, exception, level))
      logger.log(level, "Error in job refresh", exception)

    case MoleJobError(mj, j, e) ⇒
      EventDispatcher.trigger(j.environment: Environment, new Environment.MoleJobExceptionRaised(j, e, WARNING, mj))
      logger.log(WARNING, "Error during job execution, it will be resubmitted.", e)

  }

  def killAndClean(job: BatchExecutionJob) {
    job.batchJob.foreach(bj ⇒ self ! KillBatchJob(bj))
    job.batchJob = None
    job.serializedJob.foreach(j ⇒ self ! CleanSerializedJob(j))
    job.serializedJob = None
  }
}
