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

package org.openmole.plugin.environment.batch.refresh

import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

import org.openmole.core.tools.service.Retry.retry
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment.{ BatchJobControl, _ }
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.thread._

import scala.tools.reflect.WrappedProperties

object JobManager extends JavaLogger { self ⇒
  import Log._

  def killPriority = 10

  def messagePriority(message: DispatchedMessage) =
    message match {
      case msg: Submit    ⇒ 50
      case msg: Refresh   ⇒ 5
      case msg: GetResult ⇒ 50
      case msg: Error     ⇒ 100 // This is very quick to process
      case _              ⇒ 1
    }

  object DispatcherActor {
    def receive(dispatched: DispatchedMessage)(implicit services: BatchEnvironment.Services) =
      dispatched match {
        case msg: Submit      ⇒ if (!msg.job.job.finished) SubmitActor.receive(msg) else self ! Kill(msg.job, None)
        case msg: Refresh     ⇒ if (!msg.job.job.finished) RefreshActor.receive(msg) else self ! Kill(msg.job, Some(msg.batchJob))
        case msg: GetResult   ⇒ if (!msg.job.job.finished) GetResultActor.receive(msg) else self ! Kill(msg.job, Some(msg.batchJob))
        case msg: RetryAction ⇒ RetryActionActor.receive(msg)
        case msg: Error       ⇒ ErrorActor.receive(msg)
      }
  }

  def dispatch(msg: DispatchedMessage)(implicit services: BatchEnvironment.Services) = services.threadProvider.submit(messagePriority(msg)) { () ⇒ DispatcherActor.receive(msg) }

  def !(msg: JobMessage)(implicit services: BatchEnvironment.Services): Unit = msg match {
    case msg: Submit      ⇒ dispatch(msg)
    case msg: Refresh     ⇒ dispatch(msg)
    case msg: GetResult   ⇒ dispatch(msg)
    case msg: RetryAction ⇒ dispatch(msg)
    case msg: Error       ⇒ dispatch(msg)

    case Manage(job) ⇒
      self ! Submit(job)

    case Delay(msg, delay) ⇒
      services.threadProvider.scheduler.schedule((self ! msg): Runnable, delay.millis, TimeUnit.MILLISECONDS)

    case Submitted(job, bj) ⇒
      self ! Delay(Refresh(job, bj, bj.updateInterval.minUpdateInterval), bj.updateInterval.minUpdateInterval)

    case Kill(job, batchJob) ⇒
      try {
        BatchEnvironment.finishedExecutionJob(job.environment, job)
        tryKillAndClean(job, batchJob)
        job.state = ExecutionState.KILLED
      }
      finally {
        if (job.job.finished) BatchEnvironment.finishedJob(job.environment, job.job)
        else job.environment.submit(job.job)
      }

    case Resubmit(job, batchJob) ⇒
      tryKillAndClean(job, Some(batchJob))
      job.state = ExecutionState.READY
      dispatch(Submit(job))

    case MoleJobError(mj, j, e) ⇒
      val er = Environment.MoleJobExceptionRaised(j, e, WARNING, mj)
      j.environment.error(er)
      services.eventDispatcher.trigger(j.environment: Environment, er)
      logger.log(FINE, "Error during job execution, it will be resubmitted.", e)

  }

  def tryKillAndClean(job: BatchExecutionJob, bj: Option[BatchJobControl])(implicit services: BatchEnvironment.Services) = {
    def kill(bj: BatchJobControl)(implicit services: BatchEnvironment.Services) = retry(services.preference(BatchEnvironment.killJobRetry))(bj.delete())
    def clean(bj: BatchJobControl)(implicit services: BatchEnvironment.Services) = retry(services.preference(BatchEnvironment.cleanJobRetry))(bj.clean())

    try bj.foreach(kill) catch {
      case e: Throwable ⇒ self ! Error(job, e, None)
    }

    try bj.foreach(clean) catch {
      case e: Throwable ⇒ self ! Error(job, e, None)
    }
  }

}
