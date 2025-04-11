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
import org.openmole.core.dsl.extension.Logger

import java.util.logging.Level

object JobManager:
  self =>

  def killPriority = 10

  def messagePriority(message: DispatchedMessage) =
    message match
      case _: Refresh   => 5
      case _: Submit    => 50
      case _: GetResult => 50
      case _: Kill      => 10
      case _: Error     => 100 // This is very quick to process
      case _            => 1

  def dispatch(msg: DispatchedMessage)(using services: BatchEnvironment.Services) =
    given AccessControl.Priority = AccessControl.Priority(messagePriority(msg))

    msg match
      case msg: Submit => SubmitActor.receive(msg)
      case msg: Refresh => RefreshActor.receive(msg)
      case msg: GetResult => GetResultActor.receive(msg)
      case msg: RetryAction => RetryActionActor.receive(msg)
      case msg: Error => ErrorActor.receive(msg)
      case msg: Kill => KillActor.receive(msg)

  def !(msg: JobMessage)(using services: BatchEnvironment.Services): Unit = services.threadProvider.virtual: () =>
    System.runFinalization // Help with finalization just in case
    import services.*

    msg match
      case msg: Submit      => dispatch(msg)
      case msg: Refresh     => dispatch(msg)
      case msg: GetResult   => dispatch(msg)
      case msg: RetryAction => dispatch(msg)
      case msg: Error       => dispatch(msg)
      case msg: Kill        => dispatch(msg)

      case Manage(bej, environment) =>
        services.eventDispatcher.trigger(environment, Environment.JobSubmitted(bej.id, bej))
        self ! Submit(bej, environment)

      case Delay(msg, delay) =>
        services.threadProvider.scheduler.schedule((self ! msg): Runnable, delay.millis, TimeUnit.MILLISECONDS)

      case Submitted(job, environment, bj) =>
        killOr(environment, job.storedJob, Kill(job, environment, Some(bj))) { () => self ! Delay(Refresh(job, environment, bj, bj.updateInterval.minUpdateInterval), bj.updateInterval.minUpdateInterval) }

      case MoleJobError(mj, j, environment, e, output, host) =>
        def detail = output.map { output =>
          s"""OpenMOLE output on remote node ${host} was:
             |$output""".stripMargin
        }.getOrElse(s"OpenMOLE output on remote node ${host} was empty")

        val er = Environment.MoleJobExceptionRaised(j, e, Level.WARNING, mj, detail = Some(detail))
        environment.error(er)
        services.eventDispatcher.trigger(environment: Environment, er)
        Logger.fine("Error during job execution, it will be resubmitted.", e)


  def sendToMoleExecution(job: StoredJob)(f: MoleExecution => Unit) =
    MoleExecutionMessage.send(job.moleExecution) { MoleExecutionMessage.WithMoleExecutionSate(f) }

  def canceled(storedJob: StoredJob) = storedJob.storedMoleJobs.forall(JobStore.subMoleCanceled)

  def killOr(batchJob: BatchExecutionJob, kill: Kill)(op: () => Any)(implicit services: BatchEnvironment.Services) =
    if (batchJob.state == ExecutionState.KILLED) ()
    else if (canceled(batchJob.storedJob)) self ! kill
    else op()

  def killOr(environment: BatchEnvironment, storedJob: StoredJob, kill: Kill)(op: () => Unit)(implicit services: BatchEnvironment.Services) =
    if environment.stopped || canceled(storedJob)
    then self ! kill
    else
      sendToMoleExecution(storedJob): state =>
        if !jobIsFinished(state, storedJob) then op() else self ! kill

  def jobIsFinished(moleExecution: MoleExecution, job: StoredJob) =
    job.storedMoleJobs.map(_.id).forall(mj => moleJobIsFinished(moleExecution, mj))

