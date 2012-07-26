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
import akka.actor.ActorSystem
import akka.actor.Props
import akka.dispatch.Dispatchers
import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedPriorityMailbox
import akka.routing.DefaultResizer
import akka.routing.RoundRobinRouter
import akka.routing.SmallestMailboxRouter
import akka.util.duration._
import org.openmole.core.batch.file.URIFile
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.IEnvironment
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.BatchEnvironment.JobManagmentThreads

object JobManager extends Logger

import JobManager._

class JobManager(environment: BatchEnvironment) extends Actor {

  val workers = ActorSystem.create("JobManagment", ConfigFactory.parseString(
    """
akka {
  daemonic="on"
  actor {
    default-dispatcher {
      executor = "fork-join-executor"
      type = Dispatcher
      mailbox-type = """ + '"' + classOf[PriorityMailBox].getName + '"' + """
      
      fork-join-executor {
        parallelism-min = """ + 10 + """
        parallelism-max = """ + Workspace.preference(JobManagmentThreads) + """
      }
      throughput = 1
    }
  }
}
"""))

  import environment._

  val resizer = DefaultResizer(lowerBound = 10, upperBound = Workspace.preferenceAsInt(JobManagmentThreads))
  // val workerForEach = Workspace.preferenceAsInt(JobManagmentThreads)
  val uploader = workers.actorOf(Props(new UploadActor(self)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val submitter = workers.actorOf(Props(new SubmitActor(self)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val refresher = workers.actorOf(Props(new RefreshActor(self)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val resultGetters = workers.actorOf(Props(new GetResultActor(self)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val killer = workers.actorOf(Props(new KillerActor).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))

  def receive = {
    case Upload(job) ⇒ uploader ! Upload(job)
    case Uploaded(job, sj) ⇒
      job.serializedJob = Some(sj)
      submitter ! Submit(job, sj)
    case Submit(job, sj) ⇒ submitter ! Submit(job, sj)
    case Submitted(job, sj, bj) ⇒
      job.batchJob = Some(bj)
      self ! RefreshDelay(job, sj, bj, minUpdateInterval, true)
    case RefreshDelay(job, sj, bj, delay, stateChanged) ⇒
      logger.fine("Refresh delay")
      val newDelay =
        if (!stateChanged) math.min(delay + incrementUpdateInterval, maxUpdateInterval) else minUpdateInterval
      logger.fine("Next state refresh in " + newDelay + " " + job)
      context.system.scheduler.scheduleOnce(newDelay milliseconds) {
        refresher ! Refresh(job, sj, bj, newDelay)
      }
    case GetResult(job, sj, out) ⇒
      resultGetters ! GetResult(job, sj, out)
    case Kill(job) ⇒
      job.state = ExecutionState.KILLED

      job.batchJob match {
        case Some(bj) ⇒ self ! KillBatchJob(bj)
        case None ⇒
      }

      job.serializedJob match {
        case Some(sj) ⇒
          val path = sj.communicationStorage.path
          URIFile.clean(path.toURIFile(sj.communicationDirPath))
        case None ⇒
      }

    case Error(job, exception) ⇒
      val level = exception match {
        case e: JobRemoteExecutionException ⇒ WARNING
        case _ ⇒ FINE
      }
      EventDispatcher.trigger(environment: IEnvironment, new IEnvironment.ExceptionRaised(job, exception, level))
      logger.log(level, "Error in job refresh", exception)
    case KillBatchJob(bj) ⇒ killer ! KillBatchJob(bj)
    case MoleJobError(mj, j, e) ⇒
      EventDispatcher.trigger(environment: IEnvironment, new IEnvironment.MoleJobExceptionRaised(j, e, WARNING, mj))
      logger.log(WARNING, "Error durring job execution, it will be resubmitted.", e)
  }
}
