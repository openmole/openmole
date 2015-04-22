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

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ TimeUnit, Executors }

import org.openmole.core.eventdispatcher.EventDispatcher
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.tools.collection.PriorityQueue
import org.openmole.core.tools.service.Logger
import org.openmole.core.workflow.execution._
import com.typesafe.config.ConfigFactory
import org.openmole.core.batch.environment._
import org.openmole.core.batch.environment.BatchEnvironment.JobManagementThreads
import org.openmole.core.workspace.Workspace
import org.openmole.tool.thread._

import scala.concurrent.duration._
import scala.concurrent.duration.{ Duration ⇒ SDuration, MILLISECONDS }

object JobManager extends Logger

import JobManager.Log._

class JobManager { self ⇒

  var finalized = new AtomicBoolean(false)

  override def finalize(): Unit = {
    finalized.set(true)
    super.finalize()
  }

  val messageQueue = PriorityQueue[DispatchedMessage] {
    case msg: Upload             ⇒ 10
    case msg: Submit             ⇒ 50
    case msg: Refresh            ⇒ 5
    case msg: GetResult          ⇒ 50
    case msg: KillBatchJob       ⇒ 1
    case msg: DeleteFile         ⇒ 1
    case msg: CleanSerializedJob ⇒ 1
  }

  val delayedExecutor = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory)

  val executors =
    for {
      i ← 0 until Workspace.preferenceAsInt(JobManagementThreads)
    } {
      val t = new Thread { while (!self.finalized.get) DispatcherActor.receive(messageQueue.dequeue) }
      t.setDaemon(true)
      t.start()
      t
    }

  val uploader = new UploadActor(self)
  val submitter = new SubmitActor(self)
  val refresher = new RefreshActor(self)
  val resultGetters = new GetResultActor(self)
  val killer = new KillerActor(self)
  val cleaner = new CleanerActor(self)
  val deleter = new DeleteActor(self)

  object DispatcherActor {
    def receive(dispatched: DispatchedMessage) =
      dispatched match {
        case msg: Upload             ⇒ uploader.receive(msg)
        case msg: Submit             ⇒ submitter.receive(msg)
        case msg: Refresh            ⇒ refresher.receive(msg)
        case msg: GetResult          ⇒ resultGetters.receive(msg)
        case msg: KillBatchJob       ⇒ killer.receive(msg)
        case msg: DeleteFile         ⇒ deleter.receive(msg)
        case msg: CleanSerializedJob ⇒ cleaner.receive(msg)
      }
  }

  def !(msg: JobMessage): Unit = msg match {
    case msg: Upload             ⇒ messageQueue.enqueue(msg)
    case msg: Submit             ⇒ messageQueue.enqueue(msg)
    case msg: Refresh            ⇒ messageQueue.enqueue(msg)
    case msg: GetResult          ⇒ messageQueue.enqueue(msg)
    case msg: KillBatchJob       ⇒ messageQueue.enqueue(msg)
    case msg: DeleteFile         ⇒ messageQueue.enqueue(msg)
    case msg: CleanSerializedJob ⇒ messageQueue.enqueue(msg)

    case Manage(job) ⇒
      self ! Upload(job)

    case Delay(msg, delay) ⇒
      delayedExecutor.schedule(self ! msg, delay.toMillis, TimeUnit.MILLISECONDS)

    case Uploaded(job, sj) ⇒
      logger.fine(s"Uploaded $job send it to submit pool")
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
      messageQueue.enqueue(Upload(job))

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
