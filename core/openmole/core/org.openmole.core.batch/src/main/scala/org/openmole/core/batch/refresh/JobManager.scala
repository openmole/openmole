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

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.dispatch.Dispatchers
import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedPriorityMailbox
import akka.routing.DefaultResizer
import akka.routing.RoundRobinRouter
import akka.routing.SmallestMailboxRouter
import org.openmole.core.model.execution._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.openmole.core.batch.environment._
import org.openmole.core.batch.storage._
import org.openmole.core.batch.environment.BatchEnvironment.JobManagmentThreads

import scala.concurrent.duration._

object JobManager extends Logger

import JobManager._

class JobManager(val environment: BatchEnvironment) extends Actor {

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
""").withFallback(ConfigFactory.load(classOf[ConfigFactory].getClassLoader)))

  import environment._
  import system.dispatcher

  val resizer = DefaultResizer(lowerBound = 10, upperBound = Workspace.preferenceAsInt(JobManagmentThreads))
  val uploader = workers.actorOf(Props(new UploadActor(self)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val submitter = workers.actorOf(Props(new SubmitActor(self)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val refresher = workers.actorOf(Props(new RefreshActor(self, environment)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val resultGetters = workers.actorOf(Props(new GetResultActor(self)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val killer = workers.actorOf(Props(new KillerActor(self)).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val cleaner = workers.actorOf(Props(new CleanerActor).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))
  val deleter = workers.actorOf(Props(new DeleteActor).withRouter(SmallestMailboxRouter(resizer = Some(resizer))))

  def receive = {
    case Upload(job) ⇒ uploader ! Upload(job)
    case Uploaded(job, sj) ⇒
      job.serializedJob = Some(sj)
      submitter ! Submit(job, sj)
    case Submit(job, sj) ⇒ submitter ! Submit(job, sj)
    case Submitted(job, sj, bj) ⇒
      job.batchJob = Some(bj)
      self ! Delay(() ⇒ refresher ! Refresh(job, sj, bj, minUpdateInterval), minUpdateInterval)
    case Delay(closure, delay) ⇒
      context.system.scheduler.scheduleOnce(delay milliseconds) {
        closure()
      }
    case GetResult(job, sj, out) ⇒
      resultGetters ! GetResult(job, sj, out)
    case Kill(job) ⇒
      job.state = ExecutionState.KILLED
      job.batchJob.foreach(bj ⇒ self ! KillBatchJob(bj))
      job.serializedJob.foreach(j ⇒ cleaner ! CleanSerializedJob(j))
    case Error(job, exception) ⇒
      val level = exception match {
        case e: JobRemoteExecutionException ⇒ WARNING
        case _ ⇒ FINE
      }
      EventDispatcher.trigger(environment: Environment, new Environment.ExceptionRaised(job, exception, level))
      logger.log(level, "Error in job refresh", exception)
    case msg: KillBatchJob ⇒ killer ! msg
    case MoleJobError(mj, j, e) ⇒
      EventDispatcher.trigger(environment: Environment, new Environment.MoleJobExceptionRaised(j, e, WARNING, mj))
      logger.log(WARNING, "Error durring job execution, it will be resubmitted.", e)
    case m: DeleteFile ⇒ deleter ! m
  }
}
