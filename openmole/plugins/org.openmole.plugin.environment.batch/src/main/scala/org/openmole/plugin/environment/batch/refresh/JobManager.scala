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

import java.util.concurrent.TimeUnit

import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.mole.MoleExecution.moleJobIsFinished
import org.openmole.core.workflow.mole.{ MoleExecution, MoleExecutionMessage }
import org.openmole.plugin.environment.batch.environment.BatchEnvironment.ExecutionJobRegistry
import org.openmole.plugin.environment.batch.environment.JobStore.StoredJob
import org.openmole.plugin.environment.batch.environment._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.thread._

object JobManager extends JavaLogger { self ⇒
  import Log._

  def killPriority = 10

  def messagePriority(message: DispatchedMessage) =
    message match {
      case _: Refresh   ⇒ 5
      case _: Submit    ⇒ 50
      case _: GetResult ⇒ 50
      case _: Manage    ⇒ 75
      case _: Error     ⇒ 100 // This is very quick to process
      case _            ⇒ 1
    }

  object DispatcherActor {
    def receive(dispatched: DispatchedMessage)(implicit services: BatchEnvironment.Services) = {
      System.runFinalization // Help with finalization just in case

      dispatched match {
        case msg: Submit      ⇒ SubmitActor.receive(msg)
        case msg: Refresh     ⇒ RefreshActor.receive(msg)
        case msg: GetResult   ⇒ GetResultActor.receive(msg)
        case msg: RetryAction ⇒ RetryActionActor.receive(msg)
        case msg: Error       ⇒ ErrorActor.receive(msg)
        case msg: Kill        ⇒ KillActor.receive(msg)
      }
    }
  }

  def dispatch(msg: DispatchedMessage)(implicit services: BatchEnvironment.Services) = services.threadProvider.submit(messagePriority(msg)) { () ⇒ DispatcherActor.receive(msg) }

  def !(msg: JobMessage)(implicit services: BatchEnvironment.Services): Unit = {
    msg match {
      case msg: Submit      ⇒ shouldKill(msg.job.environment, msg.job.storedJob, Kill(msg.job, None)) { () ⇒ dispatch(msg) }
      case msg: Refresh     ⇒ shouldKill(msg.job.environment, msg.job.storedJob, Kill(msg.job, Some(msg.batchJob))) { () ⇒ dispatch(msg) }
      case msg: GetResult   ⇒ shouldKill(msg.job.environment, msg.job.storedJob, Kill(msg.job, Some(msg.batchJob))) { () ⇒ dispatch(msg) }
      case msg: RetryAction ⇒ dispatch(msg)
      case msg: Error       ⇒ dispatch(msg)
      case msg: Kill        ⇒ dispatch(msg)

      case Manage(job, environment) ⇒
        val bej = BatchExecutionJob(job, environment)
        ExecutionJobRegistry.register(environment.registry, bej)
        services.eventDispatcher.trigger(environment, Environment.JobSubmitted(bej))
        self ! Submit(bej)

      case Delay(msg, delay) ⇒
        services.threadProvider.scheduler.schedule((self ! msg): Runnable, delay.millis, TimeUnit.MILLISECONDS)

      case Submitted(job, bj) ⇒
        shouldKill(job.environment, job.storedJob, Kill(job, Some(bj))) { () ⇒ self ! Delay(Refresh(job, bj, bj.updateInterval.minUpdateInterval), bj.updateInterval.minUpdateInterval) }

      case MoleJobError(mj, j, e) ⇒
        val er = Environment.MoleJobExceptionRaised(j, e, WARNING, mj)
        j.environment.error(er)
        services.eventDispatcher.trigger(j.environment: Environment, er)
        logger.log(FINE, "Error during job execution, it will be resubmitted.", e)

    }
  }

  def jobIsFinished(moleExecution: MoleExecution, job: StoredJob) = {
    job.storedMoleJobs.map(_.id).forall(mj ⇒ moleJobIsFinished(moleExecution, mj))
  }

  def sendToMoleExecution(job: StoredJob)(f: MoleExecution ⇒ Unit) =
    MoleExecutionMessage.send(job.moleExecution) { MoleExecutionMessage.WithMoleExecutionSate(f) }

  def canceled(storedJob: StoredJob) = storedJob.storedMoleJobs.forall(_.subMoleCanceled())

  def shouldKill(environment: BatchEnvironment, storedJob: StoredJob, kill: Kill)(op: () ⇒ Unit)(implicit services: BatchEnvironment.Services) = {
    if (environment.stopped || canceled(storedJob)) self ! kill
    else sendToMoleExecution(storedJob) { state ⇒ if (!jobIsFinished(state, storedJob)) op() else self ! kill }
  }
}
